package truss.interfaceAdaptor.grpc.service

import java.util.Currency

import akka.actor.typed.{ ActorSystem, Scheduler }
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.grpc.proto.CreateWalletResponse.Body
import truss.interfaceAdaptor.grpc.proto.{ CreateWalletRequest, CreateWalletResponse, Error, WalletGRPCService }
import truss.useCase.WalletUseCase

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }

class WalletGRPCServiceImpl(useCase: WalletUseCase)(implicit system: ActorSystem[Nothing]) extends WalletGRPCService {
  implicit val timeout: Timeout             = 3.seconds
  implicit val sch: Scheduler               = system.scheduler
  implicit val ec: ExecutionContextExecutor = system.executionContext

  override def createWallet(in: CreateWalletRequest): Future[CreateWalletResponse] = {
    useCase
      .create(
        ULID.parseFromString(in.id).get,
        WalletId(ULID.parseFromString(in.id).get),
        WalletName(in.name),
        Money(BigDecimal(in.depositAmount), Currency.getInstance(in.depositCurrency))
      )
      .map { _ =>
        CreateWalletResponse(
          id = ULID().asString,
          requestId = in.id,
          body = Some(
            Body(
              in.walletId
            )
          ),
          createAt = in.createAt
        )
      }
      .recover {
        case ex =>
          CreateWalletResponse(
            id = ULID().asString,
            requestId = in.id,
            body = Some(
              Body(
                in.walletId
              )
            ),
            error = Some(
              Error(
                text = ex.getMessage
              )
            ),
            createAt = in.createAt
          )
      }
  }
}
