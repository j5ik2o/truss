package truss.interfaceAdaptor.aggregate

import akka.actor.testkit.typed.scaladsl.LogCapturing
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.scalatest.BeforeAndAfterAll

class WalletPersistentAggregateOnKafkaSpec
    extends WalletPersistentAggregateSpecBase(
      ConfigFactory
        .parseString(
          """
          |akka.test.single-expect-default = 60s
          |akka.persistence.journal.plugin = "j5ik2o.kafka-journal"
          |akka.persistence.snapshot-store.plugin = "j5ik2o.kafka-snapshot-store"
          """.stripMargin
        )
        .withFallback(ConfigFactory.load())
    )
    with BeforeAndAfterAll
    with LogCapturing {

  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(
    customBrokerProperties = Map("num.partitions" -> "2")
  )

  protected override def beforeAll(): Unit = {
    super.beforeAll()
    EmbeddedKafka.start()
  }

  protected override def afterAll(): Unit = {
    EmbeddedKafka.stop()
    super.afterAll()
  }

}
