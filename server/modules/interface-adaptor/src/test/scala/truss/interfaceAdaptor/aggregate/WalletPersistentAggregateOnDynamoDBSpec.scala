package truss.interfaceAdaptor.aggregate

import java.net.URI

import akka.actor.testkit.typed.scaladsl.LogCapturing
import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDbAsyncClient
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient => JavaDynamoDbAsyncClient }
import truss.interfaceAdaptor.utils.{ DynamoDBSpecSupport, RandomPortUtil }
object WalletPersistentAggregateOnDynamoDBSpec {
  val dynamoDBPort = RandomPortUtil.temporaryServerPort()
}
class WalletPersistentAggregateOnDynamoDBSpec
    extends WalletPersistentAggregateSpecBase(
      ConfigFactory
        .parseString(
          s"""
        |akka.test.single-expect-default = 60s
        |akka.persistence.journal.plugin = "j5ik2o.dynamo-db-journal"
        |akka.persistence.snapshot-store.plugin = "j5ik2o.dynamo-db-snapshot"
        |j5ik2o.dynamo-db-journal {
        |  refresh-interval = 10ms
        |  dynamo-db-client {
        |    access-key-id = "x"
        |    secret-access-key = "x"
        |    endpoint = "http://127.0.0.1:${WalletPersistentAggregateOnDynamoDBSpec.dynamoDBPort}/"
        |  }
        |}
        |j5ik2o.dynamo-db-snapshot {
        |  dynamo-db-client {
        |    access-key-id = "x"
        |    secret-access-key = "x"
        |    endpoint = "http://127.0.0.1:${WalletPersistentAggregateOnDynamoDBSpec.dynamoDBPort}/"
        |  }
        |}
        |""".stripMargin
        )
        .withFallback(ConfigFactory.load())
    )
    with BeforeAndAfterAll
    with LogCapturing
    with DynamoDBSpecSupport {
  override protected lazy val dynamoDBPort: Int = WalletPersistentAggregateOnDynamoDBSpec.dynamoDBPort

  val underlying: JavaDynamoDbAsyncClient = JavaDynamoDbAsyncClient
    .builder()
    .credentialsProvider(
      StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
    )
    .endpointOverride(URI.create(dynamoDBEndpoint))
    .build()

  override def asyncClient: DynamoDbAsyncClient = DynamoDbAsyncClient(underlying)

  protected override def beforeAll(): Unit = {
    super.beforeAll()
    createTable
  }

  protected override def afterAll(): Unit = {
    super.afterAll()
    deleteTable
  }

}
