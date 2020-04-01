package truss.interfaceAdaptor.serialization.json

import java.time.Instant
import java.util.Currency

import truss.domain.WalletId
import truss.domain.money.Money
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletEvents.WalletDeposited
import truss.interfaceAdaptor.serialization.DomainObjToJsonReprIso

case class WalletDepositedJSON(id: String, walletId: String, value: BigDecimal, currency: String, updatedAt: Long)

object WalletDepositedJSON {
  implicit object Iso extends DomainObjToJsonReprIso[WalletDeposited, WalletDepositedJSON] {
    override def convertTo(domainObj: WalletDeposited): WalletDepositedJSON =
      WalletDepositedJSON(
        id = domainObj.id.asString,
        walletId = domainObj.walletId.value.asString,
        value = domainObj.value.breachEncapsulationOfAmount,
        currency = domainObj.value.breachEncapsulationOfCurrency.getCurrencyCode,
        updatedAt = domainObj.updatedAt.toEpochMilli
      )

    override def convertFrom(json: WalletDepositedJSON): WalletDeposited =
      WalletDeposited(
        id = ULID.parseFromString(json.id).get,
        walletId = WalletId(ULID.parseFromString(json.walletId).get),
        value = Money(json.value, Currency.getInstance(json.currency)),
        updatedAt = Instant.ofEpochMilli(json.updatedAt)
      )
  }
}
