package truss.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.{ ActorTestKit, LogCapturing }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import truss.domain.money.Money
import truss.domain.{ Id, Wallet, WalletName }
import truss.infrastructure.ulid.ULID
import truss.interfaceAdaptor.aggregate.WalletProtocol._

class WalletAggregateSpec extends AnyFreeSpec with BeforeAndAfterAll with Matchers with LogCapturing {

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "WalletAggregate" - {
    "rename" in {
      val walletId1 = Id(classOf[Wallet], ULID())
      val wallet1   = testKit.spawn(WalletAggregate(walletId1))
      val now       = Instant.now()

      val createWalletResultProbe = testKit.createTestProbe[CreateWalletResult]()
      wallet1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.walletId shouldBe walletId1

      val getNameResultProbe1 = testKit.createTestProbe[GetNameResult]()
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
      val walletId1 = Id(classOf[Wallet], ULID())
      val wallet1   = testKit.spawn(WalletAggregate(walletId1))
      val now       = Instant.now()

      val createWalletResultProbe = testKit.createTestProbe[CreateWalletResult]()
      wallet1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe1 = testKit.createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe1.ref)
      val getBalanceResult1 = getBalanceResultProbe1.expectMessageType[GetBalanceResult]
      getBalanceResult1.walletId shouldBe walletId1
      getBalanceResult1.balance shouldBe Money.yens(100)

      val depositWalletResultProbe = testKit.createTestProbe[DepositWalletResult]()
      wallet1 ! DepositWallet(ULID(), walletId1, Money.yens(100), now, depositWalletResultProbe.ref)
      val depositWalletSucceeded = depositWalletResultProbe.expectMessageType[DepositWalletSucceeded]
      depositWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe2 = testKit.createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe2.ref)
      val getBalanceResult2 = getBalanceResultProbe2.expectMessageType[GetBalanceResult]
      getBalanceResult2.walletId shouldBe walletId1
      getBalanceResult2.balance shouldBe Money.yens(200)
    }
    "withdraw" in {
      val walletId1 = Id(classOf[Wallet], ULID())
      val wallet1   = testKit.spawn(WalletAggregate(walletId1))
      val now       = Instant.now()

      val createWalletResultProbe = testKit.createTestProbe[CreateWalletResult]()
      wallet1 ! CreateWallet(ULID(), WalletName("test-1"), Money.yens(100), now, createWalletResultProbe.ref)
      val createWalletSucceeded = createWalletResultProbe.expectMessageType[CreateWalletSucceeded]
      createWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe1 = testKit.createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe1.ref)
      val getBalanceResult1 = getBalanceResultProbe1.expectMessageType[GetBalanceResult]
      getBalanceResult1.walletId shouldBe walletId1
      getBalanceResult1.balance shouldBe Money.yens(100)

      val withdrawWalletResultProbe = testKit.createTestProbe[WithdrawWalletResult]()
      wallet1 ! WithdrawWallet(ULID(), walletId1, Money.yens(100), now, withdrawWalletResultProbe.ref)
      val withdrawWalletSucceeded = withdrawWalletResultProbe.expectMessageType[WithdrawWalletSucceeded]
      withdrawWalletSucceeded.walletId shouldBe walletId1

      val getBalanceResultProbe2 = testKit.createTestProbe[GetBalanceResult]()
      wallet1 ! GetBalance(ULID(), walletId1, getBalanceResultProbe2.ref)
      val getBalanceResult2 = getBalanceResultProbe2.expectMessageType[GetBalanceResult]
      getBalanceResult2.walletId shouldBe walletId1
      getBalanceResult2.balance shouldBe Money.yens(0)
    }

  }

}
