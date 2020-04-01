package truss.interfaceAdaptor.aggregate

import java.time.Instant

import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID

object WalletEvents {

  sealed trait Event {
    def walletId: WalletId
  }
  case class WalletCreated(
      id: ULID,
      walletId: WalletId,
      name: WalletName,
      deposit: Money,
      createdAt: Instant
  ) extends Event
  case class WalletRenamed(id: ULID, walletId: WalletId, value: WalletName, updatedAt: Instant) extends Event
  case class WalletDeposited(id: ULID, walletId: WalletId, value: Money, updatedAt: Instant)    extends Event
  case class WalletWithdrew(id: ULID, walletId: WalletId, value: Money, updatedAt: Instant)     extends Event

}
