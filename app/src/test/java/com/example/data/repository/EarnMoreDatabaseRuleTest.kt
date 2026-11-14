package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FreelanceGig
import com.example.data.model.LedgerTransaction
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class EarnMoreDatabaseRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProfileAndWallet_authenticatedUser_createsProfileAndZeroWallet() = runBlocking {
    val uid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
    val userRepo = UserRepository(firestore, auth)

    val result =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo.ensureUserProfileAndWallet(
          displayName = "Alice Kumar",
          email = "alice@test.com",
          preferredLanguage = "en",
        )
      }

    assertTrue("Expected profile creation to succeed: ${result.exceptionOrNull()}", result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals("USER", profile.role)

    val walletRepo = WalletRepository(firestore, auth)
    val wallet = withTimeout(FLOW_TIMEOUT_MS) { walletRepo.observeWallet().first { it != null } }
    assertNotNull(wallet)
    assertEquals(0.0, wallet!!.availableBalanceInr, 0.001)
  }

  @Test
  fun wallet_userAttemptsToEditOwnBalance_failsWithPermissionDenied() = runBlocking {
    val uid = signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@test.com")
    val userRepo = UserRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      userRepo.ensureUserProfileAndWallet("Bob Singh", "bob@test.com").getOrThrow()
    }

    var caught: FirebaseFirestoreException? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("wallets").document(uid)
          .update(
            mapOf(
              "availableBalanceInr" to 9999.0,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    } catch (e: Exception) {
      caught = e.unwrapFirestoreException()
    }

    assertNotNull("Expected PERMISSION_DENIED when user edits wallet balance", caught)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught!!.code)
  }

  @Test
  fun observeOpenTasks_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val taskRepo = TaskRepository(firestore, auth)

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { taskRepo.observeOpenTasks().first() }
    } catch (e: Exception) {
      caught = e
    }

    assertNotNull(caught)
    assertTrue(
      caught is IllegalStateException ||
        caught?.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    )
  }

  @Test
  fun observeLedger_unauthenticatedFirestoreQuery_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    var caught: FirebaseFirestoreException? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("ledger_transactions").whereEqualTo("userId", "someone").get().await()
      }
    } catch (e: Exception) {
      caught = e.unwrapFirestoreException()
    }
    assertNotNull(caught)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught!!.code)
  }

  @Test
  fun marketplace_createGigAndBlockSelfOrder_succeedsAndEnforcesRules() = runBlocking {
    val sellerUid = signInTestUser("seller_${UUID.randomUUID().toString().take(6)}@test.com")
    val marketRepo = MarketplaceRepository(firestore, auth)

    val gigRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        marketRepo.createFreelanceGig(
          sellerName = "Ravi Designer",
          category = "LOGO_DESIGN",
          title = "Startup Logo Package",
          description = "Vector logo with transparent PNG and SVG deliverables.",
          priceInr = 1000.0,
          deliveryDays = 2L,
        )
      }
    assertTrue("Expected gig creation to succeed: ${gigRes.exceptionOrNull()}", gigRes.isSuccess)
    val gigId = gigRes.getOrThrow()

    // Self-order must fail
    val selfOrderRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        marketRepo.placeFreelanceOrder(
          FreelanceGig(
            id = gigId,
            sellerId = sellerUid,
            sellerName = "Ravi Designer",
            priceInr = 1000.0,
          ),
          "Need a minimalist fintech emblem",
        )
      }
    assertTrue("Self-order must be rejected", selfOrderRes.isFailure)

    // Another user can place order
    signInTestUser("buyer_${UUID.randomUUID().toString().take(6)}@test.com")
    val validOrderRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        marketRepo.placeFreelanceOrder(
          FreelanceGig(
            id = gigId,
            sellerId = sellerUid,
            sellerName = "Ravi Designer",
            priceInr = 1000.0,
          ),
          "Need a minimalist fintech emblem",
        )
      }
    assertTrue("Buyer order should succeed: ${validOrderRes.exceptionOrNull()}", validOrderRes.isSuccess)
  }

  @Test
  fun growthAndSupport_referralLookupBlocksSelfAndSupportsFriendAndTicket() = runBlocking {
    signInTestUser("referrer_${UUID.randomUUID().toString().take(6)}@test.com")
    val userRepo = UserRepository(firestore, auth)
    val referrerProfile =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        userRepo.ensureUserProfileAndWallet("Priya Sharma", "priya@test.com").getOrThrow()
      }

    val growthRepo = GrowthAndSupportRepository(firestore, auth)
    val selfRef = withTimeout(DEFAULT_TIMEOUT_MS) { growthRepo.applyReferralCode(referrerProfile.referralCode) }
    assertTrue("Self-referral must be blocked", selfRef.isFailure)

    // Sign in second user and apply referrer code via PII-free /referral_codes lookup
    signInTestUser("referee_${UUID.randomUUID().toString().take(6)}@test.com")
    withTimeout(DEFAULT_TIMEOUT_MS) {
      userRepo.ensureUserProfileAndWallet("Amit Verma", "amit@test.com").getOrThrow()
    }
    val validRef = withTimeout(DEFAULT_TIMEOUT_MS) { growthRepo.applyReferralCode(referrerProfile.referralCode) }
    assertTrue("Friend referral should succeed: ${validRef.exceptionOrNull()}", validRef.isSuccess)

    val ticketRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        growthRepo.submitSupportTicket(
          subject = "Payout Test Question",
          category = "PAYOUT",
          message = "Checking TEST MODE payout cycle timelines.",
        )
      }
    assertTrue("Support ticket should succeed: ${ticketRes.exceptionOrNull()}", ticketRes.isSuccess)
  }

  @Test
  fun admin_nonAdminDeniedCreatingTask_andAnalyticsCalculationIsAccurate() = runBlocking {
    signInTestUser("regular_${UUID.randomUUID().toString().take(6)}@test.com")
    val adminRepo = AdminRepository(firestore, auth)

    val taskRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        adminRepo.createMicroTask(
          title = "Unauthorized Task",
          titleHi = "",
          category = "DATA_ENTRY",
          description = "Regular user should be blocked from publishing admin tasks.",
          rewardGrossInr = 100.0,
          commissionPercent = 15.0,
          slotsTotal = 10L,
        )
      }
    assertTrue("Non-admin task creation must fail", taskRes.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      taskRes.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    val snapshot =
      adminRepo.computeAnalytics(
        users = listOf(UserProfile(userId = "u1", isVerified = true), UserProfile(userId = "u2", isVerified = false)),
        ledger =
          listOf(
            LedgerTransaction(
              id = "t1",
              type = "CREDIT_FREELANCE",
              grossAmountInr = 1000.0,
              platformCommissionInr = 100.0,
              gatewayFeeInr = 20.0,
              netAmountInr = 900.0,
              status = "VERIFIED",
            )
          ),
        withdrawals = listOf(WithdrawalRequest(id = "w1", status = "PENDING")),
        disputes = emptyList(),
      )
    assertEquals(2, snapshot.totalUsers)
    assertEquals(1, snapshot.verifiedUsers)
    assertEquals(1000.0, snapshot.totalGrossRevenueInr, 0.001)
    assertEquals(100.0, snapshot.totalPlatformCommissionInr, 0.001)
    assertEquals(65.0, snapshot.netProfitInr, 0.001)
    assertEquals(1, snapshot.pendingWithdrawalsCount)
  }

  @Test
  fun dailyCheckIn_oncePerDayPendingAndNoSelfWalletCredit() = runBlocking {
    val uid = signInTestUser("promo_${UUID.randomUUID().toString().take(6)}@test.com")
    val userRepo = UserRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      userRepo.ensureUserProfileAndWallet("Neha Gupta", "neha@test.com").getOrThrow()
    }
    val walletRepo = WalletRepository(firestore, auth)
    val firstClaim = withTimeout(DEFAULT_TIMEOUT_MS) { walletRepo.submitDailyCheckInClaim("2026-11-14") }
    assertTrue("Daily promo claim should submit as PENDING_ADMIN_FUNDING: ${firstClaim.exceptionOrNull()}", firstClaim.isSuccess)

    // Duplicate same-day claim is rejected
    val secondClaim = withTimeout(DEFAULT_TIMEOUT_MS) { walletRepo.submitDailyCheckInClaim("2026-11-14") }
    assertTrue("Duplicate same-day claim must fail", secondClaim.isFailure)

    // Balance remains ₹0.00 until Admin explicitly funds it
    val wallet = withTimeout(FLOW_TIMEOUT_MS) { walletRepo.observeWallet().first { it != null } }
    assertEquals(0.0, wallet!!.availableBalanceInr, 0.001)

    // User cannot self-approve check-in
    var caught: FirebaseFirestoreException? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("daily_checkins").document("checkin_${uid}_2026-11-14")
          .update(mapOf("status" to "APPROVED_TEST", "updatedAt" to FieldValue.serverTimestamp()))
          .await()
      }
    } catch (e: Exception) {
      caught = e.unwrapFirestoreException()
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
