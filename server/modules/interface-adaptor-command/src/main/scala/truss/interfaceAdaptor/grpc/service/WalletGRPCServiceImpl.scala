package truss.interfaceAdaptor.grpc.service

import java.util.Currency

import akka.actor.typed.{ ActorSystem, Scheduler }
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.grpc.proto.{
  CreateWalletRequest,
  CreateWalletResponse,
  Error,
  GetWalletNameRequest,
  GetWalletNameResponse,
  RenameWalletRequest,
  RenameWalletResponse,
  WalletGRPCService
}
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
        WalletName(in.bodies(0).name),
        Money(
          BigDecimal(in.bodies(0).money.get.depositAmount),
          Currency.getInstance(in.bodies(0).money.get.depositCurrency)
        )
      )
      .map { _ =>
        CreateWalletResponse(
          id = ULID().asString,
          requestId = in.id,
          walletIds = Seq(
            in.bodies(0).walletId
          ),
          createdAt = in.createAt
        )
      }
      .recover {
        case ex =>
          CreateWalletResponse(
            id = ULID().asString,
            requestId = in.id,
            walletIds = Seq(
              in.bodies(0).walletId
            ),
            errors = Seq(
              Error(
                text = ex.getMessage
              )
            ),
            createdAt = in.createAt
          )
      }
  }

  override def renameWallet(in: RenameWalletRequest): Future[RenameWalletResponse] = ???

  override def getWalletName(in: GetWalletNameRequest): Future[GetWalletNameResponse] = ???
}
