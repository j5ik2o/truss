package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.ShardCoordinator.ShardAllocationStrategy
import akka.cluster.sharding.ShardRegion.ShardId
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.util.Timeout
import truss.domain.money.Money
import truss.domain.{ Id, Wallet, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

import scala.concurrent.Future
import scala.concurrent.duration._

class ShardedWalletAggregate(system: ActorSystem[_]) {

  private val sharding = ClusterSharding(system)

  val TypeKey: EntityTypeKey[WalletCommand] = EntityTypeKey[WalletCommand]("Wallet")

  val shardRegion: ActorRef[ShardingEnvelope[WalletCommand]] =
    sharding.init(Entity(TypeKey) { entityContext =>
      WalletPersistentAggregate(Id(classOf[Wallet], ULID.parseFromString(entityContext.entityId).get))
    })

  implicit val timeout: Timeout = 3.seconds

  def create(id: Id[Wallet], name: WalletName, deposit: Money): Future[CreateWalletResult] = {
    val entityRef = sharding.entityRefFor(TypeKey, id.value.asString)
    entityRef.ask[CreateWalletResult](ref => CreateWallet(ULID(), id, name, deposit, Instant.now(), ref))
  }

  def rename(id: Id[Wallet], name: WalletName): Future[RenameWalletResult] = {
    val entityRef = sharding.entityRefFor(TypeKey, id.value.asString)
    entityRef.ask[RenameWalletResult](ref => RenameWallet(ULID(), id, name, Instant.now(), ref))
  }

  def deposit(id: Id[Wallet], value: Money): Future[DepositWalletResult] = {
    val entityRef = sharding.entityRefFor(TypeKey, id.value.asString)
    entityRef.ask[DepositWalletResult](ref => DepositWallet(ULID(), id, value, Instant.now(), ref))
  }

  def withdraw(id: Id[Wallet], value: Money): Future[WithdrawWalletResult] = {
    val entityRef = sharding.entityRefFor(TypeKey, id.value.asString)
    entityRef.ask[WithdrawWalletResult](ref => WithdrawWallet(ULID(), id, value, Instant.now(), ref))
  }

  def getBalance(id: Id[Wallet]): Future[GetBalanceResult] = {
    val entityRef = sharding.entityRefFor(TypeKey, id.value.asString)
    entityRef.ask[GetBalanceResult](ref => GetBalance(ULID(), id, ref))
  }

}
