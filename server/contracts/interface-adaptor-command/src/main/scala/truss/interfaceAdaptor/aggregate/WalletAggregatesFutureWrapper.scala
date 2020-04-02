package truss.interfaceAdaptor.aggregate

import akka.actor.typed.Scheduler
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

import scala.concurrent.Future

trait WalletAggregatesFutureWrapper {

  def create(id: ULID, walletId: WalletId, name: WalletName, deposit: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[CreateWalletResult]

  def rename(id: ULID, walletId: WalletId, name: WalletName)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[RenameWalletResult]

  def deposit(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[DepositWalletResult]

  def withdraw(id: ULID, walletId: WalletId, value: Money)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[WithdrawWalletResult]

  def getBalance(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[GetBalanceResult]

  def getName(id: ULID, walletId: WalletId)(
      implicit timeout: Timeout,
      scheduler: Scheduler
  ): Future[GetNameResult]

}
