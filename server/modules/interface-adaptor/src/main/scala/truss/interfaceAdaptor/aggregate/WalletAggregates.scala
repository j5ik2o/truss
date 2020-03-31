package truss.interfaceAdaptor.aggregate

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import truss.domain.WalletId
import truss.interfaceAdaptor.aggregate.WalletProtocol.WalletCommand

object WalletAggregates {

  val name = "wallets"

  def behavior(name: WalletId => String)(
      behaviorF: WalletId => Behavior[WalletCommand]
  ): Behavior[WalletCommand] = {
    Behaviors.setup { ctx =>
      def createAndSend(walletId: WalletId): ActorRef[WalletCommand] = {
        ctx.child(name(walletId)) match {
          case None      => ctx.spawn(behaviorF(walletId), name = name(walletId))
          case Some(ref) => ref.asInstanceOf[ActorRef[WalletCommand]]
        }
      }
      Behaviors.receiveMessage[WalletCommand] { msg =>
        createAndSend(msg.walletId) ! msg
        Behaviors.same
      }
    }
  }

}
