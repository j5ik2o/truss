package truss.interfaceAdaptor.utils
import java.io.File
import java.net.{ Socket, URI }
import java.util.concurrent.TimeUnit

import com.github.j5ik2o.reactive.aws.s3.S3AsyncClient
import com.spotify.docker.client.{ DefaultDockerClient, DockerClient }
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{
  DockerCommandExecutor,
  DockerContainer,
  DockerContainerState,
  DockerFactory,
  DockerReadyChecker,
  LogLineReceiver,
  VolumeMapping
}
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time.{ Second, Seconds, Span }
import org.scalatest.{ BeforeAndAfterAll, Suite }
import org.seasar.util.io.{ FileUtil, ResourceUtil }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.s3.{ S3AsyncClient => JavaS3AsyncClient }
import software.amazon.awssdk.services.s3.model._
import truss.interfaceAdaptor.aggregate.persistence.TrussBucketNameResolver

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._

case class TCPPortReadyChecker(
    port: Int,
    host: Option[String] = None,
    duration: Duration = Duration(1, TimeUnit.SECONDS)
) extends DockerReadyChecker {
  override def apply(container: DockerContainerState)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext
  ): Future[Boolean] = {
    container.getPorts().map(_(port)).flatMap { p =>
      var socket: Socket = null
      Future {
        try {
          socket = new Socket(host.getOrElse(docker.host), p)
          val result = socket.isConnected
          Thread.sleep(duration.toMillis)
          result
        } catch {
          case _: Exception =>
            false
        } finally {
          if (socket != null)
            socket.close()
        }
      }
    }
  }
}
object S3SpecSupport {
  val bucketName      = new TrussBucketNameResolver().resolve(null)
  val accessKeyId     = "AKIAIOSFODNN7EXAMPLE"
  val secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
  val minioPort       = 9000
}
trait S3SpecSupport extends DockerTestKit with BeforeAndAfterAll with ScalaFutures with Eventually { this: Suite =>
  import S3SpecSupport._
  protected val connectTimeout: FiniteDuration = 3 seconds
  protected val readTimeout: FiniteDuration    = 3 seconds

  implicit val pc = PatienceConfig(Span(30, Seconds), Span(1, Second))

  protected val dockerClient: DockerClient =
    DefaultDockerClient
      .fromEnv()
      .connectTimeoutMillis(connectTimeout.toMillis)
      .readTimeoutMillis(readTimeout.toMillis)
      .build()

  override implicit def dockerFactory: DockerFactory =
    new SpotifyDockerFactory(dockerClient)

  val base = new File(".").getAbsoluteFile.getParent
  val minioContainer: DockerContainer =
    DockerContainer("minio/minio:RELEASE.2020-03-19T21-49-00Z")
      .withEnv(
        s"MINIO_ACCESS_KEY=$accessKeyId",
        s"MINIO_SECRET_KEY=$secretAccessKey"
      )
      .withVolumes(Seq(VolumeMapping(s"$base/minio", "/data", rw = true)))
      .withPorts(minioPort -> Some(minioPort))
      .withReadyChecker(
        TCPPortReadyChecker(
          minioPort,
          duration = Duration(
            500 * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toInt,
            TimeUnit.MILLISECONDS
          )
        )
      )
      .withCommand("server", "--compat", "/data")
      .withLogLineReceiver(LogLineReceiver(true, { message => println(s">>> $message") }))

  override def dockerContainers: List[DockerContainer] =
    minioContainer :: super.dockerContainers

  override def beforeAll(): Unit = {
    super.beforeAll()
    eventually {
      val javaS3Client: JavaS3AsyncClient =
        JavaS3AsyncClient
          .builder()
          .credentialsProvider(
            StaticCredentialsProvider
              .create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
          )
          .endpointOverride(URI.create(s"http://127.0.0.1:$minioPort"))
          .build()
      val s3Client = S3AsyncClient(javaS3Client)
      s3Client
        .listBuckets()
        .flatMap { list =>
          if (list.buckets().asScala.exists(_.name() == bucketName))
            Future.successful(())
          else
            s3Client
              .createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
              .map(_ => ())
        }
        .futureValue
    }
  }

}
