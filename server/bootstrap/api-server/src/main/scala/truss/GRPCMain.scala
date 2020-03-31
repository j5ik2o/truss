package truss

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import com.typesafe.config.{ Config, ConfigFactory }
import truss.interfaceAdaptor.aggregate.ShardedWalletAggregates
import truss.interfaceAdaptor.grpc.proto.WalletServiceHandler
import truss.interfaceAdaptor.grpc.service.WalletServiceImpl

import scala.concurrent._
import scala.concurrent.duration._

object GRPCMain extends App {
  val config: Config               = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem("truss-api-server")
  implicit val executionContext    = system.dispatcher

  val shardedWalletAggregates = new ShardedWalletAggregates(system.toTyped)
  val service: HttpRequest => Future[HttpResponse] = WalletServiceHandler(
    new WalletServiceImpl(system.spawn(shardedWalletAggregates.ofProxy, "sharded-wallet-aggregates-proxy"))
  )

  val bindingFuture = Http().bindAndHandleAsync(service, interface = "0.0.0.0", port = 8080)

  sys.addShutdownHook {
    val future = bindingFuture
      .flatMap { serverBinding => serverBinding.unbind() }
      .flatMap { _ => system.terminate() }
    Await.result(future, config.getDuration("truss.api-server.terminate.duration").toMillis millis)
  }

}
