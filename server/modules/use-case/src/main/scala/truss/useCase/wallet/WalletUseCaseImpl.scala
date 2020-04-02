package truss.useCase.wallet

import akka.actor.typed.Scheduler
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletAggregatesFutureWrapper
import truss.useCase.WalletUseCase

import scala.concurrent.{ ExecutionContext, Future }

class WalletUseCaseImpl(walletAggregateAsync: WalletAggregatesFutureWrapper) extends WalletUseCase {

  override def create(id: ULID, walletId: WalletId, name: WalletName, deposit: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit] = {
    walletAggregateAsync.create(id, walletId, name, deposit).map(_ => ())
  }

  override def rename(id: ULID, walletId: WalletId, name: WalletName)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit] = {
    walletAggregateAsync.rename(id, walletId, name).map(_ => ())
  }

  override def deposit(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit] = {
    walletAggregateAsync.deposit(id, walletId, value).map(_ => ())
  }

  override def withdraw(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit] = {
    walletAggregateAsync.withdraw(id, walletId, value).map(_ => ())
  }

  override def getBalance(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Money] = {
    walletAggregateAsync.getBalance(id, walletId).map(_.balance)
  }

  override def getName(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[WalletName] = {
    walletAggregateAsync.getName(id, walletId).map(_.name)
  }

}
