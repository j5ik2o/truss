package truss.interfaceAdaptor.serialization.json

import java.time.Instant
import java.util.Currency

import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol.WalletCreated
import truss.interfaceAdaptor.serialization.DomainObjToJsonReprIso

case class WalletCreatedJSON(
    id: String,
    walletId: String,
    name: String,
    balance: BigDecimal,
    currency: String,
    createdAt: Long
)

object WalletCreatedJSON {
  implicit object WalletCreatedJSONIso extends DomainObjToJsonReprIso[WalletCreated, WalletCreatedJSON] {
    override def convertTo(domainObj: WalletCreated): WalletCreatedJSON =
      WalletCreatedJSON(
        id = domainObj.id.asString,
        walletId = domainObj.walletId.value.asString,
        name = domainObj.name.value,
        balance = domainObj.deposit.breachEncapsulationOfAmount,
        currency = domainObj.deposit.breachEncapsulationOfCurrency.getCurrencyCode,
        createdAt = domainObj.createdAt.toEpochMilli
      )

    override def convertFrom(json: WalletCreatedJSON): WalletCreated =
      WalletCreated(
        id = ULID.parseFromString(json.id).get,
        walletId = WalletId(ULID.parseFromString(json.walletId).get),
        name = WalletName(json.name),
        deposit = Money(json.balance, Currency.getInstance(json.currency)),
        createdAt = Instant.ofEpochMilli(json.createdAt)
      )
  }
}
