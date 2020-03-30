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

    "deposit" in {
      val bankAccountId1          = Id(classOf[Wallet], ULID())
      val bankAccount1            = spawn(WalletPersistentAggregate(bankAccountId1))
      val now                     = Instant.now()
      val createWalletResultProbe = createTestProbe[CreateWalletResult]()
      bankAccount1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe1 = createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe1.ref)
      getBalanceResultProbe1.expectMessage(GetBalanceResult(Money.yens(100)))

      val depositWalletResultProbe = createTestProbe[DepositWalletResult]()
      bankAccount1 ! DepositWallet(ULID(), bankAccountId1, Money.yens(100), now, depositWalletResultProbe.ref)
      val depositWalletSucceeded = depositWalletResultProbe.expectMessageType[DepositWalletSucceeded]
      depositWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe2 = createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe2.ref)
      getBalanceResultProbe2.expectMessage(GetBalanceResult(Money.yens(200)))
    }

    "withdraw" in {
      val bankAccountId1          = Id(classOf[Wallet], ULID())
      val bankAccount1            = spawn(WalletPersistentAggregate(bankAccountId1))
      val now                     = Instant.now()
      val createWalletResultProbe = createTestProbe[CreateWalletResult]()
      bankAccount1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe1 = createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe1.ref)
      getBalanceResultProbe1.expectMessage(GetBalanceResult(Money.yens(100)))

      val withdrawWalletResultProbe = createTestProbe[WithdrawWalletResult]()
      bankAccount1 ! WithdrawWallet(ULID(), bankAccountId1, Money.yens(100), now, withdrawWalletResultProbe.ref)
      val depositWalletSucceeded = withdrawWalletResultProbe.expectMessageType[WithdrawWalletSucceeded]
      depositWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe2 = createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe2.ref)
      getBalanceResultProbe2.expectMessage(GetBalanceResult(Money.yens(0)))
    }

  }
}
