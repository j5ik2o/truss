package truss.interfaceAdaptor.aggregate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import truss.domain.WalletId
import truss.interfaceAdaptor.aggregate.WalletProtocol.{ Stop, WalletCommand }

import scala.concurrent.duration._

/**
  *
  */
object WalletAggregatesMessageBroker {

  val name: String = "wallets-message-broker"

  def behavior(name: WalletId => String, receiveTimeout: FiniteDuration = 3 seconds)(
      behaviorF: WalletId => Behavior[WalletCommand]
  ): Behavior[WalletCommand] = {
    Behaviors.setup { ctx =>
      def createAndSend(walletId: WalletId): ActorRef[WalletCommand] = {
        ctx.child(name(walletId)) match {
          case None =>
            ctx.log.debug(s"spawn: child = $walletId")
            ctx.spawn(behaviorF(walletId), name = name(walletId))
          case Some(ref) =>
            ref.asInstanceOf[ActorRef[WalletCommand]]
        }
      }
      ctx.setReceiveTimeout(receiveTimeout, Stop)
      Behaviors.receiveMessage[WalletCommand] {
        case Stop =>
          ctx.log.debug("Changed state: Stop")
          Behaviors.stopped
        case msg =>
          createAndSend(msg.walletId) ! msg
          Behaviors.same
      }
    }
  }

}
