package truss.interfaceAdaptor.aggregate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import truss.interfaceAdaptor.aggregate.WalletProtocol._
import truss.interfaceAdaptor.aggregate.persistence.WalletPersistentAggregate

import scala.concurrent.duration._

object ShardedWalletAggregates {

  val TypeKey: EntityTypeKey[WalletCommand] = EntityTypeKey[WalletCommand]("Wallet")

}

object ShardedWalletAggregatesProxy {

  def behavior(sharding: ClusterSharding): Behavior[WalletCommand] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage[WalletCommand] { msg =>
      val entityRef = sharding.entityRefFor[WalletCommand](ShardedWalletAggregates.TypeKey, msg.walletId.value.asString)
      entityRef ! msg
      Behaviors.same
    }
  }

}

class ShardedWalletAggregates(system: ActorSystem[_], receiveTimeout: FiniteDuration = 3 seconds) {

  private def behavior(
      receiveTimeout: FiniteDuration
  ): EntityContext[WalletCommand] => Behavior[WalletCommand] = { entityContext =>
    Behaviors.setup[WalletCommand] { ctx =>
      val childRef = ctx.spawn(
        WalletAggregates.behavior(_.value.asString)(id => WalletPersistentAggregate(id)),
        name = WalletAggregates.name
      )
      ctx.setReceiveTimeout(receiveTimeout, Idle)
      Behaviors.receiveMessagePartial {
        case Idle =>
          entityContext.shard ! ClusterSharding.Passivate(ctx.self)
          Behaviors.same
        case Stop =>
          Behaviors.stopped
        case msg =>
          childRef ! msg
          Behaviors.same
      }
    }
  }

  val sharding = ClusterSharding(system)

  def initShardRegion: ActorRef[ShardingEnvelope[WalletCommand]] = sharding.init(
    Entity(typeKey = ShardedWalletAggregates.TypeKey)(createBehavior = behavior(receiveTimeout))
      .withStopMessage(Stop)
  )

}
