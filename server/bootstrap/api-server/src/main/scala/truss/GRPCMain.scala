package truss

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import com.typesafe.config.{ Config, ConfigFactory }
import truss.interfaceAdaptor.aggregate.ShardedWalletAggregates
import truss.interfaceAdaptor.grpc.proto.WalletServiceHandler
import truss.interfaceAdaptor.grpc.service.WalletServiceImpl

import scala.concurrent._
import scala.concurrent.duration._
import net.ceedubs.ficus.Ficus._

object GRPCMain extends App {
  val config: Config                                      = ConfigFactory.load()
  implicit val system: ActorSystem                        = ActorSystem("truss-api-server", config)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val shardedWalletAggregates                             = new ShardedWalletAggregates(system.toTyped)

  val walletService: PartialFunction[HttpRequest, Future[HttpResponse]] = WalletServiceHandler.partial(
    new WalletServiceImpl(system.spawn(shardedWalletAggregates.ofProxy, "sharded-wallet-aggregates-proxy"))
  )
  val services = ServiceHandler.concatOrNotFound(walletService)

  val host = config.as[String]("truss.api-server.grpc.host")
  val port = config.as[Int]("truss.api-server.grpc.port")

  val bindingFuture = Http().bindAndHandleAsync(services, interface = host, port)

  val terminateDuration = config.as[Duration]("truss.api-server.terminate.duration")
  sys.addShutdownHook {
    val future = bindingFuture
      .flatMap { serverBinding => serverBinding.unbind() }
      .flatMap { _ => system.terminate() }
    Await.result(future, terminateDuration)
  }

}
