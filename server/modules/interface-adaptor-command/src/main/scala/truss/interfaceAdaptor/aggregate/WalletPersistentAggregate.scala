package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.{ ActorRef, Behavior, SupervisorStrategy }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria }
import truss.domain.money.Money
import truss.domain.{ Wallet, WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletEvents._
import truss.interfaceAdaptor.aggregate.WalletProtocol._
import truss.interfaceAdaptor.aggregate.persistence.{ PersistFailureRestartWithBackoffSettings, SnapshotSettings }

import scala.concurrent.duration._

object WalletPersistentAggregate {

  sealed trait State
  final case class JustState(value: Wallet) extends State
  final case object EmptyState              extends State

  def apply(
      id: WalletId,
      persistFailureRestartWithBackoffSettings: PersistFailureRestartWithBackoffSettings = persistence
        .PersistFailureRestartWithBackoffSettings(minBackoff = 200 millis, maxBackoff = 5 seconds, randomFactor = 0.1),
      snapshotSettings: Option[SnapshotSettings] = Some(SnapshotSettings(numberOfEvents = 5, keepNSnapshots = 2))
  ): Behavior[WalletCommand] = {
    EventSourcedBehavior[WalletCommand, Event, State](
      persistenceId = PersistenceId.of(id.modelName, id.value.asString, "-"),
      emptyState = EmptyState,
      commandHandler = commandHandler(id),
      eventHandler = eventHandler
    ).withTagger(event => Set(event.walletId.modelName))
      .withRetention(
        snapshotSettings.fold(RetentionCriteria.disabled)(ss =>
          RetentionCriteria.snapshotEvery(
            ss.numberOfEvents,
            ss.keepNSnapshots
          )
        )
      )
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(
          persistFailureRestartWithBackoffSettings.minBackoff,
          persistFailureRestartWithBackoffSettings.maxBackoff,
          persistFailureRestartWithBackoffSettings.randomFactor
        )
      )
  }

  private def commandHandler(id: WalletId): (State, WalletCommand) => Effect[Event, State] = { (state, command) =>
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
      id: WalletId,
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
      id: WalletId,
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
      id: WalletId,
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
      id: WalletId,
      value: WalletName,
      replyTo: ActorRef[RenameWalletResult],
      now: Instant
  ): ReplyEffect[WalletRenamed, State] = {
    Effect.persist(WalletRenamed(ULID(), id, value, now)).thenReply(replyTo) { _ =>
      RenameWalletSucceeded(ULID(), id, now)
    }
  }

}
