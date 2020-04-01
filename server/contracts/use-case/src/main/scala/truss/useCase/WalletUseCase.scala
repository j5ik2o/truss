package truss.useCase

import akka.actor.typed.Scheduler
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID

import scala.concurrent.{ ExecutionContext, Future }

trait WalletUseCase {

  def create(id: ULID, walletId: WalletId, name: WalletName, deposit: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit]

  def rename(id: ULID, walletId: WalletId, name: WalletName)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit]

  def deposit(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit]

  def withdraw(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Unit]

  def getBalance(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[Money]

  def getName(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler,
      ec: ExecutionContext
  ): Future[WalletName]

}
