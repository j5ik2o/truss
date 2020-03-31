package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.ActorRef
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID

object WalletProtocol {

  sealed trait CommandReply
  sealed trait WalletCommand {
    def walletId: WalletId
  }
  case object Stop extends WalletCommand {
    override def walletId: WalletId = throw new NoSuchElementException
  }
  case object Idle extends WalletCommand {
    override def walletId: WalletId = throw new NoSuchElementException
  }

  sealed trait WalletCommandWithReply extends WalletCommand {
    type R <: CommandReply

    def replyTo: ActorRef[R]
  }

  final case class CreateWallet(
      id: ULID,
      walletId: WalletId,
      name: WalletName,
      deposit: Money,
      creatAt: Instant,
      replyTo: ActorRef[CreateWalletResult]
  ) extends WalletCommandWithReply {
    override type R = CreateWalletResult
  }

  sealed trait CreateWalletResult extends CommandReply

  final case class CreateWalletSucceeded(id: ULID, walletId: WalletId, creatAt: Instant) extends CreateWalletResult

  final case class CreateWalletFailed(id: ULID, walletId: WalletId, message: String, creatAt: Instant)
      extends CreateWalletResult

  final case class RenameWallet(
      id: ULID,
      walletId: WalletId,
      name: WalletName,
      updateAt: Instant,
      replyTo: ActorRef[RenameWalletResult]
  ) extends WalletCommandWithReply {
    override type R = RenameWalletResult
  }

  sealed trait RenameWalletResult extends CommandReply

  final case class RenameWalletSucceeded(id: ULID, walletId: WalletId, updatedAt: Instant) extends RenameWalletResult

  final case class RenameWalletFailed(id: ULID, walletId: WalletId, message: String, updatedAt: Instant)
      extends RenameWalletResult

  final case class DepositWallet(
      id: ULID,
      walletId: WalletId,
      value: Money,
      updateAt: Instant,
      replyTo: ActorRef[DepositWalletResult]
  ) extends WalletCommandWithReply {
    override type R = DepositWalletResult
  }

  sealed trait DepositWalletResult extends CommandReply

  final case class DepositWalletSucceeded(id: ULID, walletId: WalletId, updatedAt: Instant) extends DepositWalletResult

  final case class DepositWalletFailed(
      id: ULID,
      walletId: WalletId,
      message: String,
      updatedAt: Instant
  ) extends DepositWalletResult

  final case class WithdrawWallet(
      id: ULID,
      walletId: WalletId,
      value: Money,
      updateAt: Instant,
      replyTo: ActorRef[WithdrawWalletResult]
  ) extends WalletCommandWithReply {
    override type R = WithdrawWalletResult
  }

  sealed trait WithdrawWalletResult extends CommandReply

  final case class WithdrawWalletSucceeded(id: ULID, walletId: WalletId, updatedAt: Instant)
      extends WithdrawWalletResult

  final case class WithdrawWalletFailed(
      id: ULID,
      walletId: WalletId,
      message: String,
      updatedAt: Instant
  ) extends WithdrawWalletResult

  final case class GetName(id: ULID, walletId: WalletId, replyTo: ActorRef[GetNameResult])
      extends WalletCommandWithReply {
    override type R = GetNameResult
  }

  final case class GetNameResult(id: ULID, walletId: WalletId, name: WalletName) extends CommandReply

  final case class GetBalance(id: ULID, walletId: WalletId, replyTo: ActorRef[GetBalanceResult])
      extends WalletCommandWithReply {
    override type R = GetBalanceResult
  }

  final case class GetBalanceResult(id: ULID, walletId: WalletId, balance: Money) extends CommandReply

}
