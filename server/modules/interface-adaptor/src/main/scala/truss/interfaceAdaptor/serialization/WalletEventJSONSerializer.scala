package truss.interfaceAdaptor.serialization

import akka.actor.ExtendedActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.serialization.SerializerWithStringManifest
import truss.interfaceAdaptor.aggregate.WalletProtocol.{ WalletCreated, WalletDeposited, WalletRenamed, WalletWithdrew }
import truss.interfaceAdaptor.serialization.json.{
  WalletCreatedJSON,
  WalletDepositedJSON,
  WalletRenamedJSON,
  WalletWithdrewJSON
}

object WalletEventJSONSerializer {
  final val CREATED   = WalletCreated.getClass.getName.stripSuffix("$")
  final val RENAMED   = WalletRenamed.getClass.getName.stripSuffix("$")
  final val DEPOSITED = WalletDeposited.getClass.getName.stripSuffix("$")
  final val WITHDREW  = WalletWithdrew.getClass.getName.stripSuffix("$")
}

class WalletEventJSONSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  import WalletEventJSONSerializer._
  import io.circe.generic.auto._

  private implicit val log: LoggingAdapter = Logging.getLogger(system, getClass)

  override def identifier: Int = 50

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case orig: WalletCreated =>
      CirceJsonSerialization.toBinary[WalletCreated, WalletCreatedJSON](orig)
    case orig: WalletRenamed =>
      CirceJsonSerialization.toBinary[WalletRenamed, WalletRenamedJSON](orig)
    case orig: WalletDeposited =>
      CirceJsonSerialization.toBinary[WalletDeposited, WalletDepositedJSON](orig)
    case orig: WalletWithdrew =>
      CirceJsonSerialization.toBinary[WalletWithdrew, WalletWithdrewJSON](orig)
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case CREATED =>
      CirceJsonSerialization.fromBinary[WalletCreated, WalletCreatedJSON](bytes)
    case RENAMED =>
      CirceJsonSerialization.fromBinary[WalletRenamed, WalletRenamedJSON](bytes)
    case DEPOSITED =>
      CirceJsonSerialization.fromBinary[WalletDeposited, WalletDepositedJSON](bytes)
    case WITHDREW =>
      CirceJsonSerialization.fromBinary[WalletWithdrew, WalletWithdrewJSON](bytes)
  }

}
