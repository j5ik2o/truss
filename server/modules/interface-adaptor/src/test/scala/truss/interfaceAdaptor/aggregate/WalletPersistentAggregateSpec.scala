package truss.interfaceAdaptor.aggregate

import java.time.Instant
import java.util.UUID

import akka.actor.testkit.typed.scaladsl.{ LogCapturing, ScalaTestWithActorTestKit }
import org.scalatest.freespec.AnyFreeSpecLike
import truss.domain.money.Money
import truss.domain.{ Id, Wallet, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol.{
  CreateWallet,
  CreateWalletResult,
  CreateWalletSucceeded,
  DepositWallet,
  DepositWalletResult,
  DepositWalletSucceeded,
  GetBalance,
  GetBalanceResult,
  GetName,
  GetNameResult,
  RenameWallet,
  RenameWalletResult,
  RenameWalletSucceeded,
  WithdrawWallet,
  WithdrawWalletResult,
  WithdrawWalletSucceeded
}

class WalletPersistentAggregateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyFreeSpecLike with LogCapturing {
  "WalletPersistent" - {

    "rename" in {
      val walletId1               = Id(classOf[Wallet], ULID())
      val wallet1                 = spawn(WalletPersistentAggregate(walletId1))
      val now                     = Instant.now()
      val createWalletResultProbe = createTestProbe[CreateWalletResult]()
      wallet1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.walletId shouldBe walletId1

      val getNameResultProbe1 = createTestProbe[GetNameResult]()
      wallet1 ! GetName(ULID(), walletId1, getNameResultProbe1.ref)
      val getNameResult1 = getNameResultProbe1.expectMessageType[GetNameResult]
      getNameResult1.name shouldBe WalletName("test-1")

      val renameWalletResultProbe = testKit.createTestProbe[RenameWalletResult]()
      wallet1 ! RenameWallet(ULID(), walletId1, WalletName("test-2"), now, renameWalletResultProbe.ref)
      val renameWalletSucceeded = renameWalletResultProbe.expectMessageType[RenameWalletSucceeded]
      renameWalletSucceeded.walletId shouldBe walletId1

      val getNameResultProbe2 = testKit.createTestProbe[GetNameResult]()
      wallet1 ! GetName(ULID(), walletId1, getNameResultProbe2.ref)
      val getNameResult2 = getNameResultProbe2.expectMessageType[GetNameResult]
      getNameResult2.name shouldBe WalletName("test-2")
    }

    "deposit" in {
      val walletId1               = Id(classOf[Wallet], ULID())
      val wallet1                 = spawn(WalletPersistentAggregate(walletId1))
      val now                     = Instant.now()
      val createWalletResultProbe = createTestProbe[CreateWalletResult]()
      wallet1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe1 = createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe1.ref)
      val getBalanceResult1 = getBalanceResultProbe1.expectMessageType[GetBalanceResult]
      getBalanceResult1.walletId shouldBe walletId1
      getBalanceResult1.balance shouldBe Money.yens(100)

      val depositWalletResultProbe = createTestProbe[DepositWalletResult]()
      wallet1 ! DepositWallet(ULID(), walletId1, Money.yens(100), now, depositWalletResultProbe.ref)
      val depositWalletSucceeded = depositWalletResultProbe.expectMessageType[DepositWalletSucceeded]
      depositWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe2 = createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe2.ref)
      val getBalanceResult2 = getBalanceResultProbe2.expectMessageType[GetBalanceResult]
      getBalanceResult2.walletId shouldBe walletId1
      getBalanceResult2.balance shouldBe Money.yens(200)
    }

    "withdraw" in {
      val walletId1               = Id(classOf[Wallet], ULID())
      val wallet1                 = spawn(WalletPersistentAggregate(walletId1))
      val now                     = Instant.now()
      val createWalletResultProbe = createTestProbe[CreateWalletResult]()
      wallet1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe1 = createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe1.ref)
      val getBalanceResult1 = getBalanceResultProbe1.expectMessageType[GetBalanceResult]
      getBalanceResult1.walletId shouldBe walletId1
      getBalanceResult1.balance shouldBe Money.yens(100)

      val withdrawWalletResultProbe = createTestProbe[WithdrawWalletResult]()
      wallet1 ! WithdrawWallet(ULID(), walletId1, Money.yens(100), now, withdrawWalletResultProbe.ref)
      val depositWalletSucceeded = withdrawWalletResultProbe.expectMessageType[WithdrawWalletSucceeded]
      depositWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe2 = createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe2.ref)
      val getBalanceResult2 = getBalanceResultProbe2.expectMessageType[GetBalanceResult]
      getBalanceResult2.walletId shouldBe walletId1
      getBalanceResult2.balance shouldBe Money.yens(0)
    }

  }
}
