package truss.interfaceAdaptor.aggregate

import akka.actor.testkit.typed.scaladsl.LogCapturing
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.scalatest.BeforeAndAfterAll
import truss.interfaceAdaptor.utils.S3SpecSupport

class WalletPersistentAggregateOnKafkaSpec
    extends WalletPersistentAggregateSpecBase(
      ConfigFactory
        .parseString(
          s"""
          |akka.test.single-expect-default = 60s
          |akka.persistence.journal.plugin = "j5ik2o.kafka-journal"
          |akka.persistence.snapshot-store.plugin = "j5ik2o.s3-snapshot-store"
          |j5ik2o.s3-snapshot-store {
          |  class = "com.github.j5ik2o.akka.persistence.s3.snapshot.S3SnapshotStore"
          |  bucket-name-resolver-class-name = "truss.interfaceAdaptor.aggregate.persistence.TrussBucketNameResolver"
          |  key-converter-class-name = "com.github.j5ik2o.akka.persistence.s3.resolver.KeyConverter$$PersistenceId"
          |  path-prefix-resolver-class-name = "com.github.j5ik2o.akka.persistence.s3.resolver.PathPrefixResolver$$PersistenceId"
          |  extension-name = "snapshot"
          |  max-load-attempts = 3
          |  s3-client {
          |    access-key-id = "${S3SpecSupport.accessKeyId}"
          |    secret-access-key = "${S3SpecSupport.secretAccessKey}"
          |    endpoint = "http://127.0.0.1:${S3SpecSupport.minioPort}"
          |    s3-options {
          |      path-style-access-enabled = true
          |    }
          |  }
          |}
          """.stripMargin
        )
        .withFallback(ConfigFactory.load())
    )
    with S3SpecSupport
    with BeforeAndAfterAll
    with LogCapturing {

  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(
    customBrokerProperties = Map("num.partitions" -> "2")
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    EmbeddedKafka.start()
  }

  override def afterAll(): Unit = {
    EmbeddedKafka.stop()
    super.afterAll()
  }

}
