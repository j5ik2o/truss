package truss.interfaceAdaptor.aggregate

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.LogCapturing
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ typed, ActorSystem }
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, Join }
import akka.testkit.TestKit
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Second, Seconds, Span }
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol.{
  CreateWalletFailed,
  CreateWalletSucceeded,
  Stop,
  WalletCommand
}
import truss.interfaceAdaptor.utils.TypedActorSpecSupport

import scala.concurrent.duration._

class ShardedWalletAggregatesSpec
    extends TestKit(
      ActorSystem(
        "ShardedWalletAggregatesSpec",
        ConfigFactory
          .parseString(s"""
                                                                       |akka.actor.provider = cluster
                                                                       |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                                                                       |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
                                                                       |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID
                            .randomUUID()
                            .toString}"
    """.stripMargin)
          .withFallback(ConfigFactory.load())
      )
    )
    with AnyFreeSpecLike
    with Matchers
    with Eventually
    with LogCapturing
    with ScalaFutures
    with TypedActorSpecSupport {
  def typedSystem: typed.ActorSystem[Nothing] = system.toTyped
  val cluster: Cluster                        = Cluster(typedSystem)
  val clusterSharding: ClusterSharding        = ClusterSharding(typedSystem)

  implicit val pc = PatienceConfig(Span(30, Seconds), Span(1, Second))

  "ShardedThreadAggregates" - {
    "create" in {
      cluster.manager ! Join(cluster.selfMember.address)
      eventually {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }
      implicit val to  = Timeout(3 seconds)
      implicit val sch = system.scheduler.toTyped

      val shardedWalletAggregates = new ShardedWalletAggregates(typedSystem)
      shardedWalletAggregates.initShardRegion

      val proxyRef = system.spawn(ShardedWalletAggregatesProxy.behavior(clusterSharding), "proxy")
      val async    = new WalletAggregatesFutureWrapperImpl(proxyRef)

      val walletId = WalletId(ULID())

      val createResult =
        async.create(ULID(), walletId, WalletName("test-1"), Money.yens(100)).futureValue

      createResult match {
        case CreateWalletSucceeded(_, _walletId, creatAt) =>
          walletId shouldBe _walletId
        case CreateWalletFailed(id, walletId, message, creatAt) =>
          assert(false)
      }

      val balanceResult =
        async.getBalance(ULID(), walletId).futureValue
      balanceResult.balance shouldBe Money.yens(100)

    }
  }

  override def killActor(actor: ActorRef[Nothing], max: FiniteDuration): Unit = {
    actor.asInstanceOf[ActorRef[WalletCommand]] ! Stop
  }
}
