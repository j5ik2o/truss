package truss.interfaceAdaptor.aggregate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import truss.interfaceAdaptor.aggregate.WalletProtocol._

import scala.concurrent.duration._

class ShardedWalletAggregates(system: ActorSystem[_], receiveTimeout: FiniteDuration = 3 seconds) {

  val TypeKey: EntityTypeKey[WalletCommand] = EntityTypeKey[WalletCommand]("Wallet")

  private def behavior(
      receiveTimeout: FiniteDuration
  ): EntityContext[WalletCommand] => Behavior[WalletCommand] = { entityContext =>
    Behaviors.setup[WalletCommand] { ctx =>
      val childRef = ctx.spawn(
        WalletAggregates.behavior(_.value.asString)(WalletPersistentAggregate.apply),
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

  private val sharding = ClusterSharding(system)

  val shardRegion: ActorRef[ShardingEnvelope[WalletCommand]] = sharding.init(
    Entity(typeKey = TypeKey)(createBehavior = behavior(receiveTimeout))
      .withStopMessage(Stop)
  )

  def ofProxy: Behavior[WalletCommand] = Behaviors.receiveMessage[WalletCommand] { msg =>
    shardRegion ! ShardingEnvelope(msg.walletId.value.asString, msg)
    Behaviors.same
  }

  def ofAsync(
      spawnF: (Behavior[WalletCommand], String) => ActorRef[WalletCommand]
  ): WalletAggregatesAsync = {
    new WalletAggregatesAsync(
      spawnF(ofProxy, "sharded-wallet-aggregates-async")
    )
  }

}
