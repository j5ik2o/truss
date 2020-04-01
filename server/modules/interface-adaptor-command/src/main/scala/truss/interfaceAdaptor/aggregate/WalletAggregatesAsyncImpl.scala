package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, Scheduler }
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

import scala.concurrent.Future

class WalletAggregatesAsyncImpl(entityRef: ActorRef[WalletCommand]) extends WalletAggregateAsync {

  override def create(id: ULID, walletId: WalletId, name: WalletName, deposit: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[CreateWalletResult] = {
    entityRef.ask[CreateWalletResult](ref => CreateWallet(id, walletId, name, deposit, Instant.now(), ref))
  }

  override def rename(id: ULID, walletId: WalletId, name: WalletName)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[RenameWalletResult] = {
    entityRef.ask[RenameWalletResult](ref => RenameWallet(id, walletId, name, Instant.now(), ref))
  }

  override def deposit(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[DepositWalletResult] = {
    entityRef.ask[DepositWalletResult](ref => DepositWallet(id, walletId, value, Instant.now(), ref))
  }

  override def withdraw(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[WithdrawWalletResult] = {
    entityRef.ask[WithdrawWalletResult](ref => WithdrawWallet(id, walletId, value, Instant.now(), ref))
  }

  override def getBalance(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[GetBalanceResult] = {
    entityRef.ask[GetBalanceResult](ref => GetBalance(id, walletId, ref))
  }

  override def getName(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[GetNameResult] = {
    entityRef.ask[GetNameResult](ref => GetName(id, walletId, ref))
  }
}
