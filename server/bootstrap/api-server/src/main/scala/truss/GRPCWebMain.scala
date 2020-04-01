package truss

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.scaladsl.WebHandler
import akka.http.scaladsl.Http
import com.typesafe.config.{ Config, ConfigFactory }
import net.ceedubs.ficus.Ficus._
import truss.interfaceAdaptor.aggregate.ShardedWalletAggregates
import truss.interfaceAdaptor.grpc.proto.WalletServiceHandler
import truss.interfaceAdaptor.grpc.service.WalletServiceImpl

import scala.concurrent._
import scala.concurrent.duration._

object GRPCWebMain extends App {
  val config: Config                                      = ConfigFactory.load()
  implicit val system: ActorSystem                        = ActorSystem("truss-api-server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val shardedWalletAggregates                             = new ShardedWalletAggregates(system.toTyped)

  val handler = WebHandler.grpcWebHandler(
    WalletServiceHandler
      .partial(new WalletServiceImpl(system.spawn(shardedWalletAggregates.ofProxy, "sharded-wallet-aggregates-proxy")))
  )
  val host = config.as[String]("truss.api-server.grpc.host")
  val port = config.as[Int]("truss.api-server.grpc.port")

  val bindingFuture = Http().bindAndHandleAsync(handler, interface = "0.0.0.0", port = 8081)

  val terminateDuration = config.as[Duration]("truss.api-server.terminate.duration")
  sys.addShutdownHook {
    val future = bindingFuture
      .flatMap { serverBinding => serverBinding.unbind() }
      .flatMap { _ => system.terminate() }
    Await.result(future, terminateDuration)
  }
}
