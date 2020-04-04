package truss.api

import java.time.Instant

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import truss.domain.money.Money
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.grpc.proto.CreateWalletRequest.Body
import truss.interfaceAdaptor.grpc.proto.{ CreateWalletRequest, WalletGRPCServiceClient }
import truss.interfaceAdaptor.grpc.proto.{ Money => MoneyProto }

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TestClient extends App {

  implicit val sys   = ActorSystem("HelloWorldClient")
  implicit val ec    = sys.dispatcher
  val clientSettings = GrpcClientSettings.connectToServiceAt("127.0.0.1", 18080).withTls(false)
  val client         = WalletGRPCServiceClient(clientSettings)

  val id              = ULID().asString
  val walletId        = ULID().asString
  val name            = "test-1"
  val deposit         = Money.yens(100)
  val depositAmount   = deposit.breachEncapsulationOfAmount.toString()
  val depositCurrency = deposit.breachEncapsulationOfCurrency.getCurrencyCode
  val createAt        = Instant.now().toEpochMilli
  val future = client.createWallet(
    CreateWalletRequest(id, Seq(Body(walletId, name, Some(MoneyProto(depositAmount, depositCurrency)))), createAt)
  )
  val result = Await.result(future, Duration.Inf)
  println(result)
}
