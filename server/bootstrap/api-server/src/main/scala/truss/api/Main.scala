package truss.api

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ ActorSystem, Props }
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.{ Cluster, ClusterEvent }
import akka.grpc.scaladsl.{ ServerReflection, ServiceHandler, WebHandler }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.typesafe.config.{ Config, ConfigFactory }
import net.ceedubs.ficus.Ficus._
import org.slf4j.{ Logger, LoggerFactory }
import scopt.{ OParser, Read }
import truss.api.AppTypes.AppTypes
import truss.interfaceAdaptor.aggregate._
import truss.interfaceAdaptor.aggregate.persistence.{
  PersistFailureSettings,
  SnapshotSettings,
  WalletPersistentAggregate
}
import truss.interfaceAdaptor.grpc.proto.{ WalletGRPCService, WalletGRPCServiceHandler }
import truss.interfaceAdaptor.grpc.service.WalletGRPCServiceImpl
import truss.useCase.WalletUseCase
import truss.useCase.wallet.WalletUseCaseImpl

import scala.concurrent.duration._
import scala.concurrent.{ Future, _ }
import scala.sys.ShutdownHookThread

object AppTypes extends Enumeration {
  type AppTypes = Value
  val All, RESTOnly, GRPCOnly, GRPCWebOnly  = Value
  implicit val appTypesRead: Read[AppTypes] = scopt.Read.reads(AppTypes.withName)
}

case class MainOptions(appTypes: AppTypes = AppTypes.All)
class CommandLineParseException(args: Array[String])
    extends Exception(s"Failed to parse command line: args = [${args.mkString(",")}]")

object Main extends App {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  val persistFailureRestartWithBackoffSettings: Option[PersistFailureSettings] = None
  val snapshotSettings: Option[SnapshotSettings]                               = None

  val config: Config = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.load())

  implicit val system: ActorSystem              = ActorSystem("truss-api-server", config)
  implicit val cluster: Cluster                 = Cluster(system)
  implicit val clusterSharding: ClusterSharding = ClusterSharding(system.toTyped)

  import system.dispatcher

  logger.info(s"Started [$system], cluster.selfAddress = ${cluster.selfAddress}")

  val akkaManagement = AkkaManagement(system)
  akkaManagement.start()
  val clusterBootstrap = ClusterBootstrap(system)
  clusterBootstrap.start()

  registerClusterWatcher(cluster)

  val walletAggregatesMessageBrokerBehavior =
    WalletAggregatesMessageBroker.behavior(_.value.asString, 3 seconds) { id =>
      WalletPersistentAggregate.behavior(id, persistFailureRestartWithBackoffSettings, snapshotSettings)
    }

  val shardedWalletAggregates =
    new ShardedWalletAggregates(walletAggregatesMessageBrokerBehavior, WalletAggregatesMessageBroker.name)
  shardedWalletAggregates.initShardRegion

  val shardedWalletAggregatesProxyRef: ActorRef[WalletProtocol.WalletCommand] =
    system.spawn(ShardedWalletAggregatesProxy.behavior(shardedWalletAggregates.sharding), "proxy")
  val walletAggregatesFutureWrapper: WalletAggregatesFutureWrapper = new WalletAggregatesFutureWrapperImpl(
    shardedWalletAggregatesProxyRef
  )

  val walletUseCase: WalletUseCase     = new WalletUseCaseImpl(walletAggregatesFutureWrapper)
  val walletService: WalletGRPCService = new WalletGRPCServiceImpl(walletUseCase)(system.toTyped)

  val terminateDuration: Duration = config.as[Duration]("truss.api-server.terminate.duration")

  val mainOptions = parseCommandLine(args)
  mainOptions.appTypes match {
    case AppTypes.GRPCOnly =>
      startGRPC(config, walletService, terminateDuration)
    case AppTypes.GRPCWebOnly =>
      startGRPCWeb(config, walletService, terminateDuration)
    case AppTypes.All =>
      startGRPC(config, walletService, terminateDuration)
      startGRPCWeb(config, walletService, terminateDuration)
  }

  startK8SProbe(config, terminateDuration)
  sys.addShutdownHook {
    val future = akkaManagement.stop().flatMap { _ => system.terminate() }
    Await.result(future, terminateDuration)
    logger.info("Stopped ActorSystem")
  }

  cluster.registerOnMemberUp {
    logger.info("Cluster member is up!")
  }

  cluster.registerOnMemberRemoved {
    logger.info("Cluster member is removed!")
  }

  private def registerClusterWatcher(cluster: Cluster)(implicit system: ActorSystem): Unit = {
    cluster.subscribe(
      system.actorOf(Props[ClusterWatcher]),
      ClusterEvent.InitialStateAsEvents,
      classOf[ClusterDomainEvent]
    )
  }

  private def startK8SProbe(config: Config, terminateDuration: Duration)(implicit system: ActorSystem): Unit = {
    import com.github.everpeace.healthchecks.k8s._
    import system.dispatcher
    val akkaHealthCheck = HealthCheck.akka
    val serverBinding = bindAndHandleProbes(
      readinessProbe(akkaHealthCheck),
      livenessProbe(akkaHealthCheck)
    ).map { v =>
      logger.info(s"Started [$system], k8s probe = $host, $port")
      v
    }
    registerShutdownHook(config, serverBinding, terminateDuration)
  }

  private def parseCommandLine(args: Array[String]): MainOptions = {
    val commandLineParser = {
      val mainOptionsBuilder = OParser.builder[MainOptions]
      import mainOptionsBuilder._
      OParser.sequence(
        programName("truss"),
        head("truss", "1.0.0"),
        opt[AppTypes]('a', "app-types")
          .action((at, mo) => mo.copy(appTypes = at))
          .text("app-types is application type to boot")
      )
    }
    OParser
      .parse(commandLineParser, args, MainOptions())
      .getOrElse(throw new CommandLineParseException(args))
  }

  private def registerShutdownHook(
      config: Config,
      bindingFuture: Future[Http.ServerBinding],
      terminateDuration: Duration
  )(implicit system: ActorSystem): ShutdownHookThread = {
    import system.dispatcher
    val terminateDuration: Duration = config.as[Duration]("truss.api-server.terminate.duration")
    sys.addShutdownHook {
      val future = bindingFuture
        .flatMap { serverBinding => serverBinding.unbind() }
      Await.result(future, terminateDuration)
    }
  }

  private def startGRPC(config: Config, walletGRPCService: WalletGRPCService, terminateDuration: Duration)(
      implicit system: ActorSystem
  ): Unit = {
    import system.dispatcher
    val walletServiceHandler: PartialFunction[HttpRequest, Future[HttpResponse]] =
      WalletGRPCServiceHandler.partial(walletGRPCService)
    val walletRefectionService = ServerReflection.partial(List(WalletGRPCService))
    val services: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(walletServiceHandler, walletRefectionService)

    val host = config.as[String]("truss.api-server.grpc.host")
    val port = config.as[Int]("truss.api-server.grpc.port")

    val bindingFuture: Future[Http.ServerBinding] = Http()
      .bindAndHandleAsync(
        services,
        interface = host,
        port
//        connectionContext = HttpConnectionContext(UseHttp2.Always)
      )
      .map { v =>
        logger.info(s"Started [$system], grpc = $host, $port")
        v
      }

    registerShutdownHook(config, bindingFuture, terminateDuration)
  }

  private def startGRPCWeb(config: Config, walletGRPCService: WalletGRPCService, terminateDuration: Duration)(
      implicit system: ActorSystem
  ): Unit = {
    import system.dispatcher
    val walletRefectionService = ServerReflection.partial(List(WalletGRPCService))
    val grpcWebHandler =
      WebHandler.grpcWebHandler(WalletGRPCServiceHandler.partial(walletGRPCService), walletRefectionService)
    val host = config.as[String]("truss.api-server.grpc-web.host")
    val port = config.as[Int]("truss.api-server.grpc-web.port")

    val bindingFuture: Future[Http.ServerBinding] = Http()
      .bindAndHandleAsync(
        grpcWebHandler,
        interface = host,
        port
//        connectionContext = HttpConnectionContext(UseHttp2.Always)
      )
      .map { v =>
        logger.info(s"Started [$system], grpc-web = $host, $port")
        v
      }

    registerShutdownHook(config, bindingFuture, terminateDuration)
  }

}
