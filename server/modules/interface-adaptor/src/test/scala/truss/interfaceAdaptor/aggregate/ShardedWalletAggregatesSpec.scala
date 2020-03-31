package truss.interfaceAdaptor.aggregate

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.{ LogCapturing, ScalaTestWithActorTestKit }
import akka.actor.typed.ActorSystem
import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, Join }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol.{ CreateWalletFailed, CreateWalletSucceeded }
import truss.interfaceAdaptor.utils.TypedActorSpecSupport

class ShardedWalletAggregatesSpec
    extends ScalaTestWithActorTestKit(s"""
                                                                       |akka.actor.provider = cluster
                                                                       |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                                                                       |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
                                                                       |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID
                                           .randomUUID()
                                           .toString}"
    """.stripMargin)
    with AnyFreeSpecLike
    with LogCapturing
    with ScalaFutures
    with TypedActorSpecSupport {
  def typedSystem[T]: ActorSystem[T]   = system.asInstanceOf[ActorSystem[T]]
  val cluster: Cluster                 = Cluster(system)
  val clusterSharding: ClusterSharding = ClusterSharding(typedSystem)

  "ShardedThreadAggregates" - {
    "create" in {
      cluster.manager ! Join(cluster.selfMember.address)
      eventually {
        cluster.selfMember.status shouldEqual MemberStatus.Up
      }
      implicit val sch     = system.scheduler
      val shardedAggregate = new ShardedWalletAggregates(typedSystem)
      val async            = shardedAggregate.ofAsync { (b, _) => spawn(b) }
      val walletId         = WalletId(ULID())

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
}
