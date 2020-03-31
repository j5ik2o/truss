package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.{ ActorRef, Behavior }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, ReplyEffect }
import truss.domain.money.Money
import truss.domain.{ Id, Wallet, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

object WalletPersistentAggregate {

  sealed trait State
  final case class JustState(value: Wallet) extends State
  final case object EmptyState              extends State

  def apply(id: Id[Wallet]): Behavior[WalletCommand] = {
    EventSourcedBehavior[WalletCommand, Event, State](
      persistenceId = PersistenceId.of(id.model, id.value.asString),
      emptyState = EmptyState,
      commandHandler = commandHandler(id),
      eventHandler = eventHandler
    )
  }

  private def commandHandler(id: Id[Wallet]): (State, WalletCommand) => Effect[Event, State] = { (state, command) =>
    (command, state) match {
      case (GetName(_, id, replyTo), JustState(state)) if id == state.id =>
        replyTo ! GetNameResult(ULID(), id, state.name)
        Effect.none
      case (GetBalance(_, id, replyTo), JustState(state)) if id == state.id =>
        replyTo ! GetBalanceResult(ULID(), id, state.balance)
        Effect.none
      case (CreateWallet(_, _id, name, deposit, _, replyTo), EmptyState) if id == _id =>
        create(id, name, deposit, replyTo, Instant.now())
      case (DepositWallet(_, id, value, _, replyTo), JustState(state)) if id == state.id =>
        deposit(id, value, replyTo, state, Instant.now())
      case (WithdrawWallet(_, id, value, _, replyTo), JustState(state)) if id == state.id =>
        withdraw(id, value, replyTo, state, Instant.now())
      case (RenameWallet(_, id, value, _, replyTo), JustState(state)) if id == state.id =>
        rename(id, value, replyTo, Instant.now())
      case _ =>
        Effect.none
    }
  }

  private def eventHandler: (State, Event) => State = { (state, event) =>
    (event, state) match {
      case (WalletCreated(_, id, name, deposit, _), EmptyState) =>
        JustState(new Wallet(id, name, deposit, Instant.now, Instant.now))
      case (WalletDeposited(_, id, value, _), JustState(state)) if id == state.id =>
        state.deposit(value).fold(error => throw new IllegalStateException(error.message), JustState)
      case (WalletWithdrew(_, id, value, _), JustState(state)) if id == state.id =>
        state.withdraw(value).fold(error => throw new IllegalStateException(error.message), JustState)
      case (WalletRenamed(_, id, value, _), JustState(state)) if id == state.id =>
        state.rename(value).fold(error => throw new IllegalArgumentException(error.message), JustState)
      case _ =>
        state
    }
  }

  private def create(
      id: Id[Wallet],
      name: WalletName,
      deposit: Money,
      replyTo: ActorRef[CreateWalletResult],
      now: Instant
  ): ReplyEffect[WalletCreated, State] = {
    Wallet(id, name, deposit, now, now) match {
      case Right(_) =>
        Effect.persist(WalletCreated(ULID(), id, name, deposit, now)).thenReply(replyTo) { _ =>
          CreateWalletSucceeded(ULID(), id, now)
        }
      case Left(error) =>
        Effect.reply(replyTo)(CreateWalletFailed(ULID(), id, error.message, now))
    }
  }

  private def withdraw(
      id: Id[Wallet],
      value: Money,
      replyTo: ActorRef[WithdrawWalletResult],
      state: Wallet,
      now: Instant
  ): ReplyEffect[WalletWithdrew, State] = {
    state.canWithdraw(value) match {
      case Right(_) =>
        Effect.persist(WalletWithdrew(ULID(), id, value, now)).thenReply(replyTo) { _ =>
          WithdrawWalletSucceeded(ULID(), id, now)
        }
      case Left(error) =>
        Effect.reply(replyTo)(WithdrawWalletFailed(ULID(), id, error.message, now))
    }
  }

  private def deposit(
      id: Id[Wallet],
      value: Money,
      replyTo: ActorRef[DepositWalletResult],
      state: Wallet,
      now: Instant
  ): ReplyEffect[WalletDeposited, State] = {
    state.canDeposit(value) match {
      case Right(_) =>
        Effect.persist(WalletDeposited(ULID(), id, value, now)).thenReply(replyTo) { _ =>
          DepositWalletSucceeded(ULID(), id, now)
        }
      case Left(error) =>
        Effect.reply(replyTo)(DepositWalletFailed(ULID(), id, error.message, now))
    }
  }

  private def rename(
      id: Id[Wallet],
      value: WalletName,
      replyTo: ActorRef[RenameWalletResult],
      now: Instant
  ): ReplyEffect[WalletRenamed, State] = {
    Effect.persist(WalletRenamed(ULID(), id, value, now)).thenReply(replyTo) { _ =>
      RenameWalletSucceeded(ULID(), id, now)
    }
  }

}
