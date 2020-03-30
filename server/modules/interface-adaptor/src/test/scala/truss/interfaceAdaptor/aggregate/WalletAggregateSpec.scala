package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import truss.domain.money.Money
import truss.domain.{ Id, Wallet, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

class WalletAggregateSpec extends AnyFreeSpec with BeforeAndAfterAll with Matchers {

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "WalletAggregate" - {
    "deposit" in {
      val bankAccountId1 = Id(classOf[Wallet], ULID())
      val bankAccount1   = testKit.spawn(WalletAggregate(bankAccountId1))
      val now            = Instant.now()

      val createWalletResultProbe = testKit.createTestProbe[CreateWalletResult]()
      bankAccount1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe1 = testKit.createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe1.ref)
      getBalanceResultProbe1.expectMessage(GetBalanceResult(Money.yens(100)))

      val depositWalletResultProbe = testKit.createTestProbe[DepositWalletResult]()
      bankAccount1 ! DepositWallet(ULID(), bankAccountId1, Money.yens(100), now, depositWalletResultProbe.ref)
      val depositWalletSucceeded = depositWalletResultProbe.expectMessageType[DepositWalletSucceeded]
      depositWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe2 = testKit.createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe2.ref)
      getBalanceResultProbe2.expectMessage(GetBalanceResult(Money.yens(200)))
    }
    "withdraw" in {
      val bankAccountId1 = Id(classOf[Wallet], ULID())
      val bankAccount1   = testKit.spawn(WalletAggregate(bankAccountId1))
      val now            = Instant.now()

      val createWalletResultProbe = testKit.createTestProbe[CreateWalletResult]()
      bankAccount1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe1 = testKit.createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe1.ref)
      getBalanceResultProbe1.expectMessage(GetBalanceResult(Money.yens(100)))

      val withdrawWalletResultProbe = testKit.createTestProbe[WithdrawWalletResult]()
      bankAccount1 ! WithdrawWallet(ULID(), bankAccountId1, Money.yens(100), now, withdrawWalletResultProbe.ref)
      val withdrawWalletSucceeded = withdrawWalletResultProbe.expectMessageType[WithdrawWalletSucceeded]
      withdrawWalletSucceeded.bankAccountId shouldBe bankAccountId1

      val getBalanceResultProbe2 = testKit.createTestProbe[GetBalanceResult]()
      bankAccount1 ! GetBalance(ULID(), bankAccountId1, getBalanceResultProbe2.ref)
      getBalanceResultProbe2.expectMessage(GetBalanceResult(Money.yens(0)))
    }

  }

}
