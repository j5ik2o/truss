package truss.interfaceAdaptor.grpc.service

import java.util.Currency

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletAggregatesAsync
import truss.interfaceAdaptor.aggregate.WalletProtocol.{ CreateWalletFailed, CreateWalletSucceeded, WalletCommand }
import truss.interfaceAdaptor.grpc.proto.{ CreateWalletRequest, CreateWalletResponse, WalletService }
import akka.actor.typed.scaladsl.adapter._
import scala.concurrent.Future
import scala.concurrent.duration._

class WalletServiceImpl(walletRef: ActorRef[WalletCommand])(implicit val system: ActorSystem) extends WalletService {
  implicit val timeout: Timeout = 3.seconds
  implicit val sch              = system.scheduler.toTyped
  implicit val ec               = system.dispatcher

  private val async = new WalletAggregatesAsync(walletRef)

  override def createWallet(in: CreateWalletRequest): Future[CreateWalletResponse] = {
    async
      .create(
        ULID.parseFromString(in.id).get,
        WalletId(ULID.parseFromString(in.id).get),
        WalletName(in.name),
        Money(BigDecimal(in.depositAmount), Currency.getInstance(in.depositCurrency))
      )
      .map {
        case f: CreateWalletFailed =>
          CreateWalletResponse(
            id = f.id.asString,
            walletId = f.walletId.value.asString,
            errorMessage = f.message,
            createAt = f.creatAt.toEpochMilli
          )
        case s: CreateWalletSucceeded =>
          CreateWalletResponse(
            id = s.id.asString,
            walletId = s.walletId.value.asString,
            createAt = s.creatAt.toEpochMilli
          )
      }
  }
}
