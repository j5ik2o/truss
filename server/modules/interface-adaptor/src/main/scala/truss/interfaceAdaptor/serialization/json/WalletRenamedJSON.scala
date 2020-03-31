package truss.interfaceAdaptor.serialization.json

import java.time.Instant

import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletEvents.WalletRenamed
import truss.interfaceAdaptor.serialization.DomainObjToJsonReprIso

case class WalletRenamedJSON(id: String, walletId: String, name: String, updatedAt: Long)

object WalletRenamedJSON {
  implicit object WalletRenamedJSONIso extends DomainObjToJsonReprIso[WalletRenamed, WalletRenamedJSON] {
    override def convertTo(domainObj: WalletRenamed): WalletRenamedJSON =
      WalletRenamedJSON(
        id = domainObj.id.asString,
        walletId = domainObj.walletId.value.asString,
        name = domainObj.value.value,
        updatedAt = domainObj.updatedAt.toEpochMilli
      )

    override def convertFrom(json: WalletRenamedJSON): WalletRenamed =
      WalletRenamed(
        id = ULID.parseFromString(json.id).get,
        walletId = WalletId(ULID.parseFromString(json.walletId).get),
        value = WalletName(json.name),
        updatedAt = Instant.ofEpochMilli(json.updatedAt)
      )
  }
}
