package truss.domain

import java.time.Instant

import truss.domain.money.Money

sealed trait WalletError {
  def message: String
}
case class WalletDepositError(id: Id[Wallet], message: String)  extends WalletError
case class WalletWithdrawError(id: Id[Wallet], message: String) extends WalletError

object Wallet {
  def apply(
      id: Id[Wallet],
      name: WalletName,
      balance: Money,
      createAt: Instant,
      updatedAt: Instant
  ): Either[WalletError, Wallet] = {
    try {
      Right(new Wallet(id, name, balance, createAt, updatedAt))
    } catch {
      case _: IllegalArgumentException =>
        Left(WalletDepositError(id, s"Failed to deposit: money = $balance"))
    }
  }
}

case class Wallet(
    id: Id[Wallet],
    name: WalletName,
    balance: Money,
    createAt: Instant,
    updatedAt: Instant
) {
  require(!balance.isLessThan(Money.zero(Money.DefaultCurrency)), "balance is less than 0")

  def canDeposit(money: Money): Either[WalletError, Unit] = {
    if ((balance + money).isNegative) Left(WalletDepositError(id, s"Failed to deposit: money = $money"))
    else Right(())
  }

  def deposit(money: Money): Either[WalletError, Wallet] = {
    canDeposit(money).map { _ => copy(balance = this.balance + money) }
  }

  def canWithdraw(money: Money): Either[WalletDepositError, Unit] = {
    if ((balance + money).isNegative) Left(WalletDepositError(id, s"Failed to withdraw: money = $money"))
    else Right(())
  }

  def withdraw(money: Money): Either[WalletError, Wallet] = {
    canWithdraw(money).map { _ => copy(balance = this.balance - money) }
  }

}
