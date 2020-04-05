package truss.interfaceAdaptor.aggregate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, Props }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import truss.interfaceAdaptor.aggregate.WalletProtocol._
import truss.interfaceAdaptor.aggregate.persistence.WalletPersistentAggregate

import scala.concurrent.duration._

object ShardedWalletAggregates {

  val TypeKey: EntityTypeKey[WalletCommand] = EntityTypeKey[WalletCommand]("Wallet")

}

/**
  * The proxy to [[ShardedWalletAggregates]].
  *
  */
object ShardedWalletAggregatesProxy {

  def behavior(sharding: ClusterSharding): Behavior[WalletCommand] =
    Behaviors.receiveMessage[WalletCommand] { msg =>
      val entityRef = sharding.entityRefFor[WalletCommand](ShardedWalletAggregates.TypeKey, msg.walletId.value.asString)
      entityRef ! msg
      Behaviors.same
    }

}

/**
  *
  *
  * @param sharding
  * @param receiveTimeout
  */
class ShardedWalletAggregates(
    behavior: Behavior[WalletCommand],
    name: String,
    receiveTimeout: FiniteDuration = 3 seconds
)(implicit val sharding: ClusterSharding) {

  private def behavior(
      receiveTimeout: FiniteDuration
  ): EntityContext[WalletCommand] => Behavior[WalletCommand] = { entityContext =>
    Behaviors.setup[WalletCommand] { ctx =>
      val childRef = ctx.spawn(behavior, name)
      ctx.setReceiveTimeout(receiveTimeout, Idle)
      Behaviors.receiveMessagePartial {
        case Idle =>
          ctx.log.debug("Changed state: Idle")
          entityContext.shard ! ClusterSharding.Passivate(ctx.self)
          Behaviors.same
        case Stop =>
          ctx.log.debug("Changed state: Stop")
          Behaviors.stopped
        case msg =>
          childRef ! msg
          Behaviors.same
      }
    }
  }

  def initShardRegion: ActorRef[ShardingEnvelope[WalletCommand]] = sharding.init(
    Entity(typeKey = ShardedWalletAggregates.TypeKey)(createBehavior = behavior(receiveTimeout))
      .withStopMessage(Stop)
  )

}
