package truss

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.scaladsl.WebHandler
import akka.http.scaladsl.Http
import com.typesafe.config.{ Config, ConfigFactory }
import net.ceedubs.ficus.Ficus._
import truss.interfaceAdaptor.aggregate.{ ShardedWalletAggregates, WalletAggregatesAsyncImpl }
import truss.interfaceAdaptor.grpc.proto.WalletGRPCServiceHandler
import truss.interfaceAdaptor.grpc.service.WalletGRPCServiceImpl
import truss.useCase.wallet.WalletUseCaseImpl

import scala.concurrent._
import scala.concurrent.duration._

object GRPCWebMain extends App {
  val config: Config                                      = ConfigFactory.load()
  implicit val system: ActorSystem                        = ActorSystem("truss-api-server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val shardedWalletAggregates = new ShardedWalletAggregates(system.toTyped)

  val async = new WalletAggregatesAsyncImpl(
    system.spawn(shardedWalletAggregates.ofProxy, "sharded-wallet-aggregates-proxy")
  )
  val walletUseCase  = new WalletUseCaseImpl(async)
  val walletService  = new WalletGRPCServiceImpl(walletUseCase)(system.toTyped)
  val grpcWebHandler = WebHandler.grpcWebHandler(WalletGRPCServiceHandler.partial(walletService))

  val host = config.as[String]("truss.api-server.grpc.host")
  val port = config.as[Int]("truss.api-server.grpc.port")

  val bindingFuture = Http().bindAndHandleAsync(grpcWebHandler, interface = host, port)

  val terminateDuration = config.as[Duration]("truss.api-server.terminate.duration")

  sys.addShutdownHook {
    val future = bindingFuture
      .flatMap { serverBinding => serverBinding.unbind() }
      .flatMap { _ => system.terminate() }
    Await.result(future, terminateDuration)
  }
}
