package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.ActorRef
import truss.domain.{ Id, Wallet, WalletName }
import truss.domain.money.Money
import truss.infrastructure.ulid.ULID

object WalletProtocol {
  sealed trait CommandReply
  sealed trait WalletCommand {
    type R <: CommandReply
    def replyTo: ActorRef[R]
  }
  final case class CreateWallet(
      id: ULID,
      name: WalletName,
      deposit: Money,
      creatAt: Instant,
      replyTo: ActorRef[CreateWalletResult]
  ) extends WalletCommand {
    override type R = CreateWalletResult
  }
  sealed trait CreateWalletResult extends CommandReply
  final case class CreateWalletSucceeded(id: ULID, bankAccountId: Id[Wallet], creatAt: Instant)
      extends CreateWalletResult
  final case class CreateWalletFailed(id: ULID, bankAccountId: Id[Wallet], message: String, creatAt: Instant)
      extends CreateWalletResult

  final case class DepositWallet(
      id: ULID,
      bankAccountId: Id[Wallet],
      value: Money,
      updateAt: Instant,
      replyTo: ActorRef[DepositWalletResult]
  ) extends WalletCommand {
    override type R = DepositWalletResult
  }
  sealed trait DepositWalletResult extends CommandReply
  final case class DepositWalletSucceeded(id: ULID, bankAccountId: Id[Wallet], updatedAt: Instant)
      extends DepositWalletResult
  final case class DepositWalletFailed(
      id: ULID,
      bankAccountId: Id[Wallet],
      message: String,
      updatedAt: Instant
  ) extends DepositWalletResult

  final case class WithdrawWallet(
      id: ULID,
      bankAccountId: Id[Wallet],
      value: Money,
      updateAt: Instant,
      replyTo: ActorRef[WithdrawWalletResult]
  ) extends WalletCommand {
    override type R = WithdrawWalletResult
  }
  sealed trait WithdrawWalletResult extends CommandReply
  final case class WithdrawWalletSucceeded(id: ULID, bankAccountId: Id[Wallet], updatedAt: Instant)
      extends WithdrawWalletResult
  final case class WithdrawWalletFailed(
      id: ULID,
      bankAccountId: Id[Wallet],
      message: String,
      updatedAt: Instant
  ) extends WithdrawWalletResult

  final case class GetBalance(id: ULID, bankAccountId: Id[Wallet], replyTo: ActorRef[GetBalanceResult])
      extends WalletCommand {
    override type R = GetBalanceResult
  }

  final case class GetBalanceResult(balance: Money) extends CommandReply
  // ---

  sealed trait Event
  case class WalletCreated(
      id: ULID,
      bankAccountId: Id[Wallet],
      name: WalletName,
      deposit: Money,
      createdAt: Instant
  ) extends Event
  case class WalletDeposited(id: ULID, bankAccountId: Id[Wallet], value: Money, updatedAt: Instant) extends Event
  case class WalletWithdrew(id: ULID, bankAccountId: Id[Wallet], value: Money, updatedAt: Instant)  extends Event

}
