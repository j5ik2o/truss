package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import truss.domain.money.Money
import truss.domain.{ Id, Wallet, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

object WalletAggregate {

  def apply(id: Id[Wallet]): Behavior[WalletCommand] = initialize(id)

  private def initialize(id: Id[Wallet]): Behavior[WalletCommand] = Behaviors.receive { (_, message) =>
    message match {
      case CreateWallet(_, name, deposit, _, replyTo) =>
        create(id, name, deposit, replyTo)
      case _ =>
        Behaviors.ignore
    }
  }

  private def walletCreated(wallet: Wallet): Behavior[WalletCommand] = Behaviors.receive { (_, message) =>
    message match {
      case GetBalance(_, bankAccountId, replyTo) if wallet.id == bankAccountId =>
        replyTo ! GetBalanceResult(wallet.balance)
        Behaviors.same
      case DepositWallet(_, bankAccountId, value, _, replyTo) if wallet.id == bankAccountId =>
        deposit(wallet, bankAccountId, value, replyTo)
      case WithdrawWallet(_, bankAccountId, value, _, replyTo) if wallet.id == bankAccountId =>
        withdraw(wallet, bankAccountId, value, replyTo)
      case _ =>
        Behaviors.ignore
    }
  }

  private def create(
      id: Id[Wallet],
      name: WalletName,
      deposit: Money,
      replyTo: ActorRef[CreateWalletResult]
  ): Behavior[WalletCommand] = {
    Wallet(id, name, deposit, Instant.now, Instant.now) match {
      case Right(s) =>
        replyTo ! CreateWalletSucceeded(ULID(), id, Instant.now)
        walletCreated(s)
      case Left(error) =>
        replyTo ! CreateWalletFailed(ULID(), id, error.message, Instant.now)
        Behaviors.same
    }
  }

  private def withdraw(
      wallet: Wallet,
      bankAccountId: Id[Wallet],
      value: Money,
      replyTo: ActorRef[WithdrawWalletResult]
  ): Behavior[WalletCommand] = {
    wallet.withdraw(value) match {
      case Right(s) =>
        replyTo ! WithdrawWalletSucceeded(ULID(), bankAccountId, Instant.now())
        walletCreated(s)
      case Left(error) =>
        replyTo ! WithdrawWalletFailed(ULID(), bankAccountId, error.message, Instant.now())
        Behaviors.same
    }
  }

  private def deposit(
      wallet: Wallet,
      bankAccountId: Id[Wallet],
      value: Money,
      replyTo: ActorRef[DepositWalletResult]
  ): Behavior[WalletCommand] = {
    wallet.deposit(value) match {
      case Right(s) =>
        replyTo ! DepositWalletSucceeded(ULID(), bankAccountId, Instant.now())
        walletCreated(s)
      case Left(error) =>
        replyTo ! DepositWalletFailed(ULID(), bankAccountId, error.message, Instant.now())
        Behaviors.same
    }
  }

}
