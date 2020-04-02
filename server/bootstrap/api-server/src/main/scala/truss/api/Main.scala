package truss.api

import akka.actor.{ ActorSystem, Props }
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.{ Cluster, ClusterEvent }
import akka.grpc.scaladsl.{ ServiceHandler, WebHandler }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.{ Http, HttpConnectionContext, UseHttp2 }
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.typesafe.config.{ Config, ConfigFactory }
import net.ceedubs.ficus.Ficus._
import org.slf4j.LoggerFactory
import scopt.{ OParser, Read }
import truss.api.AppTypes.AppTypes
import truss.interfaceAdaptor.aggregate._
import truss.interfaceAdaptor.grpc.proto.{ WalletGRPCService, WalletGRPCServiceHandler }
import truss.interfaceAdaptor.grpc.service.WalletGRPCServiceImpl
import truss.useCase.WalletUseCase
import truss.useCase.wallet.WalletUseCaseImpl

import scala.concurrent._
import scala.concurrent.duration._

object AppTypes extends Enumeration {
  type AppTypes = Value
  val All, RESTOnly, GRPCOnly, GRPCWebOnly  = Value
  implicit val appTypesRead: Read[AppTypes] = scopt.Read.reads(AppTypes.withName)
}

case class MainOptions(appTypes: AppTypes = AppTypes.All)
class CommandLineParseException(args: Array[String])
    extends Exception(s"Failed to parse command line: args = [${args.mkString(",")}]")

object Main extends App {
  val logger                                              = LoggerFactory.getLogger(getClass)
  val config: Config                                      = ConfigFactory.load()
  implicit val system: ActorSystem                        = ActorSystem("truss-api-server", config)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val cluster                                    = Cluster(system)
  logger.info(s"Started [$system], cluster.selfAddress = ${cluster.selfAddress}")

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  cluster.subscribe(
    system.actorOf(Props[ClusterWatcher]),
    ClusterEvent.InitialStateAsEvents,
    classOf[ClusterDomainEvent]
  )

  val shardedWalletAggregates = new ShardedWalletAggregates(system.toTyped)
  shardedWalletAggregates.initShardRegion

  val proxyRef: ActorRef[WalletProtocol.WalletCommand] =
    system.spawn(ShardedWalletAggregatesProxy.behavior(shardedWalletAggregates.sharding), "proxy")
  val walletAggregatesFutureWrapperImpl: WalletAggregatesFutureWrapper = new WalletAggregatesFutureWrapperImpl(proxyRef)

  val walletUseCaseImpl: WalletUseCase = new WalletUseCaseImpl(walletAggregatesFutureWrapperImpl)
  val walletService: WalletGRPCService = new WalletGRPCServiceImpl(walletUseCaseImpl)(system.toTyped)

  val mainOptions = parseCommandLine
  mainOptions.appTypes match {
    case AppTypes.GRPCOnly =>
      startGRPC()
    case AppTypes.GRPCWebOnly =>
      startGRPCWeb()
    case AppTypes.All =>
      startGRPC()
    // startGRPCWeb()
  }

  cluster.registerOnMemberUp {
    logger.info("Cluster member is up!")
  }

  private def parseCommandLine: MainOptions = {
    val commandLineParser = {
      val mainOptionsBuilder = OParser.builder[MainOptions]
      import mainOptionsBuilder._
      OParser.sequence(
        programName("truss"),
        head("truss", "1.0.0"),
        opt[AppTypes]('a', "app-types")
          .action((x, c) => c.copy(appTypes = x))
          .text("app-types is application type to boot")
      )
    }
    OParser
      .parse(commandLineParser, args, MainOptions())
      .getOrElse(throw new CommandLineParseException(args))
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

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandleAsync(
      services,
      interface = host,
      port,
      connectionContext = HttpConnectionContext(UseHttp2.Always)
    )
    registerShutdown(bindingFuture)
  }

  def startGRPCWeb(): Unit = {
    val grpcWebHandler = WebHandler.grpcWebHandler(WalletGRPCServiceHandler.partial(walletService))

    val host = config.as[String]("truss.api-server.grpc-web.host")
    val port = config.as[Int]("truss.api-server.grpc-web.port")

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandleAsync(
      grpcWebHandler,
      interface = host,
      port,
      connectionContext = HttpConnectionContext(UseHttp2.Always)
    )
    registerShutdown(bindingFuture)
  }

}
