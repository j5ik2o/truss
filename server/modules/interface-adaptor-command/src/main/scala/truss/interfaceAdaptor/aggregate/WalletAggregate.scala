package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import truss.domain.money.Money
import truss.domain.{ Id, Wallet, WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

object WalletAggregate {

  def apply(id: WalletId): Behavior[WalletCommand] = initialize(id)

  private def initialize(id: WalletId): Behavior[WalletCommand] = Behaviors.receive { (_, message) =>
    message match {
      case CreateWallet(_, _id, name, deposit, _, replyTo) if id == _id =>
        create(id, name, deposit, Instant.now(), replyTo)
      case _ =>
        Behaviors.ignore
    }
  }

  private def walletCreated(wallet: Wallet): Behavior[WalletCommand] = Behaviors.receive { (_, message) =>
    message match {
      case GetName(_, walletId, replyTo) if wallet.id == walletId =>
        replyTo ! GetNameResult(ULID(), walletId, wallet.name)
        Behaviors.same
      case GetBalance(_, walletId, replyTo) if wallet.id == walletId =>
        replyTo ! GetBalanceResult(ULID(), walletId, wallet.balance)
        Behaviors.same
      case DepositWallet(_, walletId, value, _, replyTo) if wallet.id == walletId =>
        deposit(wallet, walletId, value, Instant.now(), replyTo)
      case WithdrawWallet(_, walletId, value, _, replyTo) if wallet.id == walletId =>
        withdraw(wallet, walletId, value, Instant.now(), replyTo)
      case RenameWallet(_, walletId, value, _, replyTo) =>
        rename(wallet, walletId, value, Instant.now(), replyTo)
      case _ =>
        Behaviors.ignore
    }
  }

  private def create(
      id: WalletId,
      name: WalletName,
      deposit: Money,
      now: Instant,
      replyTo: ActorRef[CreateWalletResult]
  ): Behavior[WalletCommand] = {
    Wallet(id, name, deposit, Instant.now, now) match {
      case Right(s) =>
        replyTo ! CreateWalletSucceeded(ULID(), id, now)
        walletCreated(s)
      case Left(error) =>
        replyTo ! CreateWalletFailed(ULID(), id, error.message, now)
        Behaviors.same
    }
  }

  private def withdraw(
      wallet: Wallet,
      walletId: WalletId,
      value: Money,
      now: Instant,
      replyTo: ActorRef[WithdrawWalletResult]
  ): Behavior[WalletCommand] = {
    wallet.withdraw(value) match {
      case Right(s) =>
        replyTo ! WithdrawWalletSucceeded(ULID(), walletId, now)
        walletCreated(s)
      case Left(error) =>
        replyTo ! WithdrawWalletFailed(ULID(), walletId, error.message, now)
        Behaviors.same
    }
  }

  private def deposit(
      wallet: Wallet,
      walletId: WalletId,
      value: Money,
      now: Instant,
      replyTo: ActorRef[DepositWalletResult]
  ): Behavior[WalletCommand] = {
    wallet.deposit(value) match {
      case Right(s) =>
        replyTo ! DepositWalletSucceeded(ULID(), walletId, now)
        walletCreated(s)
      case Left(error) =>
        replyTo ! DepositWalletFailed(ULID(), walletId, error.message, now)
        Behaviors.same
    }
  }

  private def rename(
      wallet: Wallet,
      walletId: WalletId,
      value: WalletName,
      now: Instant,
      replyTo: ActorRef[RenameWalletResult]
  ): Behavior[WalletCommand] = {
    wallet.rename(value) match {
      case Right(s) =>
        replyTo ! RenameWalletSucceeded(ULID(), walletId, now)
        walletCreated(s)
      case Left(error) =>
        replyTo ! RenameWalletFailed(ULID(), walletId, error.message, now)
        Behaviors.same
    }
  }

}
