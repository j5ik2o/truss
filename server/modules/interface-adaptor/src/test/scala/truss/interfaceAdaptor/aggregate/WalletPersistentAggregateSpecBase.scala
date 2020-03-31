package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.typesafe.config.Config
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import truss.domain.money.Money
import truss.domain.{ WalletId, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._
import truss.interfaceAdaptor.utils.TypedActorSpecSupport

abstract class WalletPersistentAggregateSpecBase(config: Config)
    extends ScalaTestWithActorTestKit(config)
    with AnyFreeSpecLike
    with Matchers
    with TypedActorSpecSupport {
  "WalletPersistentAggregate" - {
    "create and replay" in {
      val walletId1               = WalletId(ULID())
      val wallet1                 = spawn(WalletPersistentAggregate(walletId1))
      val now                     = Instant.now()
      val createWalletResultProbe = createTestProbe[CreateWalletResult]()
      wallet1 ! CreateWallet(ULID(), walletId1, WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.walletId shouldBe walletId1

      val depositWalletResultProbe = createTestProbe[DepositWalletResult]
      wallet1 ! DepositWallet(ULID(), walletId1, Money.yens(100), Instant.now, depositWalletResultProbe.ref)
      val depositWalletSucceeded = depositWalletResultProbe.expectMessageType[DepositWalletSucceeded]
      depositWalletSucceeded.walletId shouldBe walletId1

      killActors(wallet1)

      val getBalanceResultProbe = createTestProbe[GetBalanceResult]()
      val rebootWallet1         = spawn(WalletPersistentAggregate(walletId1))
      rebootWallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe.ref)
      val getBalanceResult = getBalanceResultProbe.expectMessageType[GetBalanceResult]
      getBalanceResult.balance shouldBe Money.yens(200)
    }
  }
}
