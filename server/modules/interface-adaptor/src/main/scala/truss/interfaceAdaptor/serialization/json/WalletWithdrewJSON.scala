package truss.interfaceAdaptor.serialization.json

import java.time.Instant
import java.util.Currency

import truss.domain.WalletId
import truss.domain.money.Money
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletEvents.WalletWithdrew
import truss.interfaceAdaptor.serialization.DomainObjToJsonReprIso

case class WalletWithdrewJSON(id: String, walletId: String, value: BigDecimal, currency: String, updatedAt: Long)

object WalletWithdrewJSON {

  implicit object WalletWithdrewJSONIso extends DomainObjToJsonReprIso[WalletWithdrew, WalletWithdrewJSON] {

    override def convertTo(domainObj: WalletWithdrew): WalletWithdrewJSON =
      WalletWithdrewJSON(
        id = domainObj.id.asString,
        walletId = domainObj.walletId.value.asString,
        value = domainObj.value.breachEncapsulationOfAmount,
        currency = domainObj.value.breachEncapsulationOfCurrency.getCurrencyCode,
        updatedAt = domainObj.updatedAt.toEpochMilli
      )

    override def convertFrom(json: WalletWithdrewJSON): WalletWithdrew =
      WalletWithdrew(
        id = ULID.parseFromString(json.id).get,
        walletId = WalletId(ULID.parseFromString(json.walletId).get),
        value = Money(json.value, Currency.getInstance(json.currency)),
        updatedAt = Instant.ofEpochMilli(json.updatedAt)
      )

  }
}
