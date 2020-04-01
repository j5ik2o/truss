package truss

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.scaladsl.{ ServiceHandler, WebHandler }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import com.typesafe.config.{ Config, ConfigFactory }
import net.ceedubs.ficus.Ficus._
import scopt.Read
import truss.AppTypes.AppTypes
import truss.interfaceAdaptor.aggregate.{
  ShardedWalletAggregates,
  ShardedWalletAggregatesProxy,
  WalletAggregatesFutureWrapperImpl
}
import truss.interfaceAdaptor.grpc.proto.{ WalletGRPCService, WalletGRPCServiceHandler }
import truss.interfaceAdaptor.grpc.service.WalletGRPCServiceImpl
import truss.useCase.WalletUseCase
import truss.useCase.wallet.WalletUseCaseImpl

import scala.concurrent._
import scala.concurrent.duration._

object AppTypes extends Enumeration {
  type AppTypes = Value
  val All, RESTOnly, GRPCOnly, GRPCWebOnly = Value
}

case class MainOptions(appTypes: AppTypes = AppTypes.All)

object Main extends App {
  implicit val appTypesRead: Read[AppTypes] = scopt.Read.reads(AppTypes withName)
  import scopt.OParser
  val builder = OParser.builder[MainOptions]
  val parser1 = {
    import builder._
    OParser.sequence(
      programName("truss"),
      head("truss", "4.x"),
      opt[AppTypes]('a', "app-types")
        .action((x, c) => c.copy(appTypes = x))
        .text("app-types is application type to boot")
    )
  }

  val mainOptions = OParser.parse(parser1, args, MainOptions()).getOrElse(throw new Exception())

  val config: Config                                      = ConfigFactory.load()
  implicit val system: ActorSystem                        = ActorSystem("truss-api-server", config)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val shardedWalletAggregates = new ShardedWalletAggregates(system.toTyped)
  shardedWalletAggregates.initShardRegion

  val walletAggregatesFutureWrapperImpl = new WalletAggregatesFutureWrapperImpl(
    system.spawn(ShardedWalletAggregatesProxy.behavior(shardedWalletAggregates.sharding), "proxy")
  )
  val walletUseCaseImpl: WalletUseCase = new WalletUseCaseImpl(walletAggregatesFutureWrapperImpl)
  val walletService: WalletGRPCService = new WalletGRPCServiceImpl(walletUseCaseImpl)(system.toTyped)

  mainOptions.appTypes match {
    case AppTypes.GRPCOnly =>
      startGRPC()
    case AppTypes.GRPCWebOnly =>
      startGRPCWeb()
    case AppTypes.All =>
      startGRPC()
      startGRPCWeb()
  }

  def registerShutdown(bindingFuture: Future[Http.ServerBinding]) = {
    val terminateDuration = config.as[Duration]("truss.api-server.terminate.duration")

    sys.addShutdownHook {
      val future = bindingFuture
        .flatMap { serverBinding => serverBinding.unbind() }
        .flatMap { _ => system.terminate() }
      Await.result(future, terminateDuration)
    }
  }

  def startGRPC(): Unit = {
    val walletServiceHandler: PartialFunction[HttpRequest, Future[HttpResponse]] =
      WalletGRPCServiceHandler.partial(walletService)
    val services: HttpRequest => Future[HttpResponse] = ServiceHandler.concatOrNotFound(walletServiceHandler)

    val host = config.as[String]("truss.api-server.grpc.host")
    val port = config.as[Int]("truss.api-server.grpc.port")

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandleAsync(services, interface = host, port)
    registerShutdown(bindingFuture)
  }

  def startGRPCWeb(): Unit = {
    val grpcWebHandler = WebHandler.grpcWebHandler(WalletGRPCServiceHandler.partial(walletService))

    val host = config.as[String]("truss.api-server.grpc-web.host")
    val port = config.as[Int]("truss.api-server.grpc-web.port")

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandleAsync(grpcWebHandler, interface = host, port)
    registerShutdown(bindingFuture)
  }

}
