package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AffiliateEvent
import com.example.data.model.AffiliateOffer
import com.example.data.model.AuditLog
import com.example.data.model.CommissionConfig
import com.example.data.model.DailyCheckInClaim
import com.example.data.model.DisputeRefund
import com.example.data.model.FraudFlag
import com.example.data.model.LedgerTransaction
import com.example.data.model.MicroTask
import com.example.data.model.ReferralRecord
import com.example.data.model.SupportTicket
import com.example.data.model.TaskSubmission
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.data.model.WithdrawalRequest
import com.example.data.security.CommissionCalculator
import com.example.data.security.OperationType
import com.example.data.security.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class AdminDashboardSnapshot(
  val totalUsers: Int = 0,
  val verifiedUsers: Int = 0,
  val totalTransactions: Int = 0,
  val totalGrossRevenueInr: Double = 0.0,
  val totalPlatformCommissionInr: Double = 0.0,
  val totalRefundsInr: Double = 0.0,
  val totalGatewayChargesInr: Double = 0.0,
  val estimatedOperatingCostsInr: Double = 0.0,
  val netProfitInr: Double = 0.0,
  val totalUserPayoutsInr: Double = 0.0,
  val pendingWithdrawalsCount: Int = 0,
)

class AdminRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireAdminUser(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("Admin authentication required.")

  suspend fun checkIsAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    if (user.isEmailVerified && user.email == BOOTSTRAPPED_ADMIN_EMAIL) return true
    return try {
      db.collection("admins").document(user.uid).get().await().exists()
    } catch (_: Exception) {
      false
    }
  }

  fun observeAllUsers(): Flow<List<UserProfile>> = observeCollection("users", UserProfile::class.java)
  fun observeAllLedger(): Flow<List<LedgerTransaction>> =
    observeCollection("ledger_transactions", LedgerTransaction::class.java)
  fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> =
    observeCollection("withdrawal_requests", WithdrawalRequest::class.java)
  fun observeAllSubmissions(): Flow<List<TaskSubmission>> =
    observeCollection("task_submissions", TaskSubmission::class.java)
  fun observeAllDisputes(): Flow<List<DisputeRefund>> =
    observeCollection("disputes_refunds", DisputeRefund::class.java)
  fun observeAllTickets(): Flow<List<SupportTicket>> =
    observeCollection("support_tickets", SupportTicket::class.java)
  fun observeAllFraudFlags(): Flow<List<FraudFlag>> =
    observeCollection("fraud_flags", FraudFlag::class.java)
  fun observeAllAuditLogs(): Flow<List<AuditLog>> =
    observeCollection("audit_logs", AuditLog::class.java)
  fun observeAllAffiliateEvents(): Flow<List<AffiliateEvent>> =
    observeCollection("affiliate_events", AffiliateEvent::class.java)
  fun observeAllDailyCheckIns(): Flow<List<DailyCheckInClaim>> =
    observeCollection("daily_checkins", DailyCheckInClaim::class.java)

  private fun <T> observeCollection(path: String, clazz: Class<T>): Flow<List<T>> = flow {
    val uid = requireAdminUser()
    emitAll(
      db.collection(path)
        .snapshots()
        .map { it.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun computeAnalytics(
    users: List<UserProfile>,
    ledger: List<LedgerTransaction>,
    withdrawals: List<WithdrawalRequest>,
    disputes: List<DisputeRefund>,
  ): AdminDashboardSnapshot {
    val verifiedCredits = ledger.filter { it.status == "VERIFIED" && it.type.startsWith("CREDIT_") }
    val gross = verifiedCredits.sumOf { it.grossAmountInr }
    val commission = verifiedCredits.sumOf { it.platformCommissionInr }
    val gatewayFees = verifiedCredits.sumOf { it.gatewayFeeInr }
    val userPayouts = verifiedCredits.sumOf { it.netAmountInr }
    val refunds = disputes.filter { it.status == "REFUNDED" }.sumOf { it.requestedRefundInr }
    val opCosts = gross * 0.015 // 1.5% MVP cloud/admin operating overhead estimate
    val netProfit = commission - refunds - gatewayFees - opCosts

    return AdminDashboardSnapshot(
      totalUsers = users.size,
      verifiedUsers = users.count { it.isVerified },
      totalTransactions = ledger.size,
      totalGrossRevenueInr = gross,
      totalPlatformCommissionInr = commission,
      totalRefundsInr = refunds,
      totalGatewayChargesInr = gatewayFees,
      estimatedOperatingCostsInr = opCosts,
      netProfitInr = netProfit,
      totalUserPayoutsInr = userPayouts,
      pendingWithdrawalsCount = withdrawals.count { it.status == "PENDING" },
    )
  }

  suspend fun seedInitialMarketplaceCatalog(): Result<Unit> {
    val adminUid = requireAdminUser()
    return try {
      val batch = db.batch()
      val defaults =
        listOf(
          Triple("TASKS", 15.0, 0.0),
          Triple("FREELANCE", 10.0, 2.0),
          Triple("PRODUCTS", 8.0, 2.0),
          Triple("AFFILIATE", 30.0, 0.0),
          Triple("TEACHING", 12.0, 2.0),
          Triple("SERVICES", 10.0, 2.0),
        )
      defaults.forEach { (cat, comm, gw) ->
        val ref = db.collection("commission_configs").document(cat)
        batch.set(
          ref,
          mapOf(
            "category" to cat,
            "commissionPercent" to comm,
            "gatewayFeePercent" to gw,
            "operatingCostPercent" to 1.5,
            "isActive" to true,
            "updatedByUid" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      val sampleTasks =
        listOf(
          mapOf(
            "id" to "task_retail_tag_01",
            "title" to "Categorize 25 Retail Product Images",
            "titleHi" to "25 उत्पाद चित्रों को वर्गीकृत करें",
            "category" to "IMAGE_CATEGORIZATION",
            "description" to "Tag grocery and apparel images accurately into categories.",
            "rewardGrossInr" to 100.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 85.0,
          ),
          mapOf(
            "id" to "task_data_entry_02",
            "title" to "Verify 30 Local Business PIN Codes",
            "titleHi" to "30 स्थानीय व्यापार पिन कोड सत्यापित करें",
            "category" to "DATA_ENTRY",
            "description" to "Verify city, state, and PIN accuracy for listed Indian businesses.",
            "rewardGrossInr" to 150.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 127.5,
          ),
          mapOf(
            "id" to "task_web_test_03",
            "title" to "Mobile Checkout Usability Test",
            "titleHi" to "मोबाइल चेकआउट उपयोगिता परीक्षण",
            "category" to "WEBSITE_TESTING",
            "description" to "Test mobile form validation and submit a 3-point UX report.",
            "rewardGrossInr" to 200.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 170.0,
          ),
          mapOf(
            "id" to "game_math_01",
            "title" to "Quick Math Sprint (4/5 Correct)",
            "titleHi" to "आसान गणित स्प्रिंट (5 में से 4 सही)",
            "category" to "GAME_TASK",
            "description" to "Complete the 5-question quick math puzzle inside Easy Game Tasks and submit verified score.",
            "rewardGrossInr" to 30.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 25.5,
          ),
          mapOf(
            "id" to "game_color_02",
            "title" to "Color & Word Match Challenge",
            "titleHi" to "रंग और शब्द मिलान गेम",
            "category" to "GAME_TASK",
            "description" to "Play the 6-round Stroop color matching test and submit your verified pass proof.",
            "rewardGrossInr" to 30.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 25.5,
          ),
          mapOf(
            "id" to "game_memory_03",
            "title" to "Memory Flip Pairs Puzzle",
            "titleHi" to "मेमोरी कार्ड जोड़ी मिलान",
            "category" to "GAME_TASK",
            "description" to "Match all 3 card pairs in 8 moves or fewer and submit completion proof.",
            "rewardGrossInr" to 40.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 34.0,
          ),
          mapOf(
            "id" to "game_sequence_04",
            "title" to "Number Sequence Brain Game",
            "titleHi" to "संख्या क्रम पहेली गेम",
            "category" to "GAME_TASK",
            "description" to "Solve 4 easy arithmetic number patterns and submit verified pass proof.",
            "rewardGrossInr" to 35.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 29.75,
          ),
          mapOf(
            "id" to "game_word_05",
            "title" to "Hindi-English Word Quiz",
            "titleHi" to "हिंदी-अंग्रेज़ी शब्द मैच गेम",
            "category" to "GAME_TASK",
            "description" to "Match 5 everyday Hindi words with English meanings for language QA reward.",
            "rewardGrossInr" to 35.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 29.75,
          ),
          mapOf(
            "id" to "game_tap_06",
            "title" to "Tap Speed & UX Reflex Test",
            "titleHi" to "तेज़ टैप रिफ्लेक्स गेम",
            "category" to "GAME_TASK",
            "description" to "Tap 8 highlighted targets accurately to test touch responsiveness.",
            "rewardGrossInr" to 25.0,
            "commissionPercent" to 15.0,
            "workerNetInr" to 21.25,
          ),
        )
      sampleTasks.forEach { t ->
        val id = t["id"] as String
        val ref = db.collection("tasks").document(id)
        batch.set(
          ref,
          mapOf(
            "title" to t["title"]!!,
            "titleHi" to t["titleHi"]!!,
            "category" to t["category"]!!,
            "description" to t["description"]!!,
            "rewardGrossInr" to t["rewardGrossInr"]!!,
            "commissionPercent" to t["commissionPercent"]!!,
            "workerNetInr" to t["workerNetInr"]!!,
            "slotsTotal" to 100L,
            "slotsFilled" to 0L,
            "status" to "OPEN",
            "createdByUid" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      val offerRef = db.collection("affiliate_offers").document("offer_upi_merchant_01")
      batch.set(
        offerRef,
        mapOf(
          "merchantName" to "BazaarKart India",
          "title" to "Promote Verified Kirana POS Starter Kit",
          "category" to "Business Tools",
          "baseUrl" to "https://bazaarkart.example.in/pos",
          "totalCommissionPercent" to 12.0,
          "userSharePercent" to 70.0,
          "isActive" to true,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )

      val logRef = db.collection("audit_logs").document()
      batch.set(
        logRef,
        mapOf(
          "actorUid" to adminUid,
          "action" to "SEED_MARKETPLACE_CATALOG",
          "targetPath" to "tasks,commission_configs,affiliate_offers",
          "summary" to "Initialized default commission configs, microtasks, and affiliate offer.",
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "admin_seed")
      Result.failure(e)
    }
  }

  suspend fun createMicroTask(
    title: String,
    titleHi: String,
    category: String,
    description: String,
    rewardGrossInr: Double,
    commissionPercent: Double,
    slotsTotal: Long,
  ): Result<String> {
    val adminUid = requireAdminUser()
    val docRef = db.collection("tasks").document()
    return try {
      val breakdown = CommissionCalculator.calculate(rewardGrossInr, commissionPercent, 0.0, 1.5)
      val payload =
        buildMap<String, Any> {
          put("title", title.trim().take(120))
          if (titleHi.isNotBlank()) put("titleHi", titleHi.trim().take(140))
          put("category", category)
          put("description", description.trim().take(1000))
          put("rewardGrossInr", breakdown.grossAmountInr)
          put("commissionPercent", commissionPercent)
          put("workerNetInr", breakdown.providerNetInr)
          put("slotsTotal", slotsTotal.coerceIn(1L, 10000L))
          put("slotsFilled", 0L)
          put("status", "OPEN")
          put("createdByUid", adminUid)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
      docRef.set(payload).await()
      writeAuditLog(adminUid, "CREATE_TASK", docRef.path, "Published microtask: $title")
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun approveTaskSubmission(submission: TaskSubmission): Result<Unit> {
    val adminUid = requireAdminUser()
    return try {
      val taskSnap = db.collection("tasks").document(submission.taskId).get().await()
      val task =
        taskSnap.toObject(MicroTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: throw IllegalStateException("Task not found")
      val walletRef = db.collection("wallets").document(submission.userId)
      val subRef = db.collection("task_submissions").document(submission.id)
      val idempotencyKey = "TASK_${submission.id}"
      val ledgerRef = db.collection("ledger_transactions").document(idempotencyKey)

      db.runTransaction { tx ->
        val subCurrent = tx.get(subRef)
        if (subCurrent.getString("status") == "APPROVED") {
          throw IllegalStateException("Duplicate approval blocked by idempotency check.")
        }
        val walletSnap = tx.get(walletRef)
        val avail = walletSnap.getDouble("availableBalanceInr") ?: 0.0
        val total = walletSnap.getDouble("totalEarnedInr") ?: 0.0
        val done = walletSnap.getLong("completedTasksCount") ?: 0L

        val commission = (task.rewardGrossInr - task.workerNetInr).coerceAtLeast(0.0)

        tx.update(
          subRef,
          mapOf(
            "status" to "APPROVED",
            "reviewNote" to "Verified by Admin",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.update(
          walletRef,
          mapOf(
            "availableBalanceInr" to (avail + task.workerNetInr),
            "totalEarnedInr" to (total + task.workerNetInr),
            "completedTasksCount" to (done + 1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          ledgerRef,
          mapOf(
            "userId" to submission.userId,
            "type" to "CREDIT_TASK",
            "category" to "TASKS",
            "grossAmountInr" to task.rewardGrossInr,
            "platformCommissionInr" to commission,
            "gatewayFeeInr" to 0.0,
            "netAmountInr" to task.workerNetInr,
            "status" to "VERIFIED",
            "referenceId" to submission.taskId,
            "idempotencyKey" to idempotencyKey,
            "description" to "Task Approved: ${task.title.take(60)}",
            "isTestMode" to true,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()

      unlockEligibleReferralIfAny(submission.userId, adminUid)
      writeAuditLog(adminUid, "APPROVE_TASK", subRef.path, "Credited ₹${task.workerNetInr} to ${submission.userId}")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "task_submissions/${submission.id}")
      Result.failure(e)
    }
  }

  private suspend fun unlockEligibleReferralIfAny(refereeUid: String, adminUid: String) {
    val refDoc = db.collection("referrals").document("ref_$refereeUid")
    val snap = refDoc.get().await()
    if (!snap.exists()) return
    val status = snap.getString("status") ?: return
    val referrerUid = snap.getString("referrerUid") ?: return
    if (status != "SIGNED_UP" || referrerUid == refereeUid) return

    val bonusInr = 25.0
    val walletRef = db.collection("wallets").document(referrerUid)
    val ledgerRef = db.collection("ledger_transactions").document("REF_BONUS_$refereeUid")
    db.runTransaction { tx ->
      val w = tx.get(walletRef)
      if (!w.exists()) return@runTransaction
      val avail = w.getDouble("availableBalanceInr") ?: 0.0
      val total = w.getDouble("totalEarnedInr") ?: 0.0
      tx.update(
        refDoc,
        mapOf(
          "status" to "REWARDED",
          "rewardInr" to bonusInr,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      tx.update(
        walletRef,
        mapOf(
          "availableBalanceInr" to (avail + bonusInr),
          "totalEarnedInr" to (total + bonusInr),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      tx.set(
        ledgerRef,
        mapOf(
          "userId" to referrerUid,
          "type" to "CREDIT_REFERRAL",
          "category" to "REFERRAL",
          "grossAmountInr" to bonusInr,
          "platformCommissionInr" to 0.0,
          "gatewayFeeInr" to 0.0,
          "netAmountInr" to bonusInr,
          "status" to "VERIFIED",
          "referenceId" to refereeUid,
          "idempotencyKey" to "REF_BONUS_$refereeUid",
          "description" to "Single-tier Referral Bonus (Eligible Activity Verified)",
          "isTestMode" to true,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
    }.await()
    writeAuditLog(adminUid, "REWARD_REFERRAL", refDoc.path, "Unlocked ₹25 single-tier bonus for $referrerUid")
  }

  suspend fun rejectTaskSubmission(submissionId: String, reason: String): Result<Unit> {
    val adminUid = requireAdminUser()
    val ref = db.collection("task_submissions").document(submissionId)
    return try {
      ref
        .update(
          mapOf(
            "status" to "REJECTED",
            "reviewNote" to reason.trim().ifBlank { "Proof insufficient" }.take(240),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      writeAuditLog(adminUid, "REJECT_TASK", ref.path, "Rejected submission $submissionId")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun processWithdrawal(
    request: WithdrawalRequest,
    approveAndPayTest: Boolean,
    note: String = "",
  ): Result<Unit> {
    val adminUid = requireAdminUser()
    val reqRef = db.collection("withdrawal_requests").document(request.id)
    val walletRef = db.collection("wallets").document(request.userId)
    val ledgerRef = db.collection("ledger_transactions").document("WD_${request.id}")
    return try {
      db.runTransaction { tx ->
        val current = tx.get(reqRef)
        if (current.getString("status") != "PENDING") {
          throw IllegalStateException("Withdrawal already processed.")
        }
        if (approveAndPayTest) {
          val w = tx.get(walletRef)
          val avail = w.getDouble("availableBalanceInr") ?: 0.0
          val withdrawn = w.getDouble("totalWithdrawnInr") ?: 0.0
          require(avail >= request.amountInr) { "Insufficient wallet balance." }
          tx.update(
            walletRef,
            mapOf(
              "availableBalanceInr" to (avail - request.amountInr),
              "totalWithdrawnInr" to (withdrawn + request.amountInr),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
          tx.set(
            ledgerRef,
            mapOf(
              "userId" to request.userId,
              "type" to "DEBIT_WITHDRAWAL",
              "category" to "PAYOUT",
              "grossAmountInr" to request.amountInr,
              "platformCommissionInr" to 0.0,
              "gatewayFeeInr" to 0.0,
              "netAmountInr" to request.amountInr,
              "status" to "VERIFIED",
              "referenceId" to request.id,
              "idempotencyKey" to "WD_${request.id}",
              "description" to "TEST MODE Payout (${request.payoutMethod} -> ${request.maskedDestination})",
              "isTestMode" to true,
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        tx.update(
          reqRef,
          mapOf(
            "status" to if (approveAndPayTest) "PAID_TEST" else "REJECTED",
            "reviewNote" to note.ifBlank { if (approveAndPayTest) "Settled in TEST MODE" else "Rejected by Admin" },
            "reviewedByAdminId" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      writeAuditLog(
        adminUid,
        if (approveAndPayTest) "PAY_WITHDRAWAL_TEST" else "REJECT_WITHDRAWAL",
        reqRef.path,
        "Processed ₹${request.amountInr} for ${request.userId}",
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, reqRef.path)
      Result.failure(e)
    }
  }

  suspend fun processDailyCheckIn(
    claim: DailyCheckInClaim,
    approve: Boolean,
    creditAmountInr: Double = claim.requestedBonusInr,
  ): Result<Unit> {
    val adminUid = requireAdminUser()
    val claimRef = db.collection("daily_checkins").document(claim.id)
    val walletRef = db.collection("wallets").document(claim.userId)
    val idempotencyKey = "PROMO_${claim.id}"
    val ledgerRef = db.collection("ledger_transactions").document(idempotencyKey)
    return try {
      require(!approve || creditAmountInr in 1.0..100.0) { "Promo credit must be between ₹1 and ₹100." }
      db.runTransaction { tx ->
        val current = tx.get(claimRef)
        if (current.getString("status") != "PENDING_ADMIN_FUNDING") {
          throw IllegalStateException("Check-in claim already processed.")
        }
        if (approve) {
          val w = tx.get(walletRef)
          val avail = w.getDouble("availableBalanceInr") ?: 0.0
          val total = w.getDouble("totalEarnedInr") ?: 0.0
          tx.update(
            walletRef,
            mapOf(
              "availableBalanceInr" to (avail + creditAmountInr),
              "totalEarnedInr" to (total + creditAmountInr),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
          tx.set(
            ledgerRef,
            mapOf(
              "userId" to claim.userId,
              "type" to "CREDIT_PROMO",
              "category" to "PROMO",
              "grossAmountInr" to creditAmountInr,
              "platformCommissionInr" to 0.0,
              "gatewayFeeInr" to 0.0,
              "netAmountInr" to creditAmountInr,
              "status" to "VERIFIED",
              "referenceId" to claim.id,
              "idempotencyKey" to idempotencyKey,
              "description" to "Admin-Funded Daily Promo (${claim.dateKey})",
              "isTestMode" to true,
              "createdAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        tx.update(
          claimRef,
          mapOf(
            "status" to if (approve) "APPROVED_TEST" else "REJECTED",
            "reviewNote" to if (approve) "Promo funded in TEST MODE (₹$creditAmountInr)" else "Promo budget not active",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      writeAuditLog(
        adminUid,
        if (approve) "APPROVE_PROMO_CHECKIN" else "REJECT_PROMO_CHECKIN",
        claimRef.path,
        "Check-in ${claim.dateKey} for ${claim.userId}: approve=$approve (₹$creditAmountInr)",
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, claimRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateCommissionConfig(
    category: String,
    commissionPercent: Double,
    gatewayFeePercent: Double,
    operatingCostPercent: Double,
  ): Result<Unit> {
    val adminUid = requireAdminUser()
    val ref = db.collection("commission_configs").document(category)
    return try {
      ref
        .set(
          mapOf(
            "category" to category,
            "commissionPercent" to commissionPercent,
            "gatewayFeePercent" to gatewayFeePercent,
            "operatingCostPercent" to operatingCostPercent,
            "isActive" to true,
            "updatedByUid" to adminUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      writeAuditLog(adminUid, "UPDATE_COMMISSION", ref.path, "Set $category commission to $commissionPercent%")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun verifyUserKyc(userId: String, verify: Boolean): Result<Unit> {
    val adminUid = requireAdminUser()
    val ref = db.collection("users").document(userId)
    return try {
      ref
        .update(
          mapOf(
            "isVerified" to verify,
            "kycStatus" to if (verify) "VERIFIED" else "UNVERIFIED",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      writeAuditLog(adminUid, "VERIFY_USER", ref.path, "Set KYC verified=$verify for $userId")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun verifyAffiliateSale(
    event: AffiliateEvent,
    saleAmountInr: Double,
    totalCommissionPercent: Double = 12.0,
    userSharePercent: Double = 70.0,
  ): Result<Unit> {
    val adminUid = requireAdminUser()
    val eventRef = db.collection("affiliate_events").document(event.id)
    val walletRef = db.collection("wallets").document(event.promoterUid)
    val ledgerRef = db.collection("ledger_transactions").document("AFF_${event.id}")
    return try {
      val grossComm = saleAmountInr * (totalCommissionPercent / 100.0)
      val userPayout = grossComm * (userSharePercent / 100.0)
      val platformNet = grossComm - userPayout
      db.runTransaction { tx ->
        val w = tx.get(walletRef)
        val avail = w.getDouble("availableBalanceInr") ?: 0.0
        val total = w.getDouble("totalEarnedInr") ?: 0.0
        tx.update(
          eventRef,
          mapOf(
            "status" to "VERIFIED_SALE",
            "saleAmountInr" to saleAmountInr,
            "grossCommissionInr" to grossComm,
            "userPayoutInr" to userPayout,
            "platformNetInr" to platformNet,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.update(
          walletRef,
          mapOf(
            "availableBalanceInr" to (avail + userPayout),
            "totalEarnedInr" to (total + userPayout),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          ledgerRef,
          mapOf(
            "userId" to event.promoterUid,
            "type" to "CREDIT_AFFILIATE",
            "category" to "AFFILIATE",
            "grossAmountInr" to grossComm,
            "platformCommissionInr" to platformNet,
            "gatewayFeeInr" to 0.0,
            "netAmountInr" to userPayout,
            "status" to "VERIFIED",
            "referenceId" to event.id,
            "idempotencyKey" to "AFF_${event.id}",
            "description" to "Verified Affiliate Sale (${event.trackingCode})",
            "isTestMode" to true,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      writeAuditLog(adminUid, "VERIFY_AFFILIATE", eventRef.path, "Credited ₹$userPayout to ${event.promoterUid}")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, eventRef.path)
      Result.failure(e)
    }
  }

  suspend fun resolveDispute(disputeId: String, status: String, note: String): Result<Unit> {
    val adminUid = requireAdminUser()
    val ref = db.collection("disputes_refunds").document(disputeId)
    return try {
      ref
        .update(
          mapOf(
            "status" to status,
            "adminResolutionNote" to note.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      writeAuditLog(adminUid, "RESOLVE_DISPUTE", ref.path, "Set dispute $disputeId -> $status")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  suspend fun replySupportTicket(ticketId: String, status: String, reply: String): Result<Unit> {
    val adminUid = requireAdminUser()
    val ref = db.collection("support_tickets").document(ticketId)
    return try {
      ref
        .update(
          mapOf(
            "status" to status,
            "adminReply" to reply.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      writeAuditLog(adminUid, "REPLY_TICKET", ref.path, "Updated ticket $ticketId -> $status")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }

  fun buildAnalyticsCsv(
    analytics: AdminDashboardSnapshot,
    ledger: List<LedgerTransaction>,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("EarnMore India Revenue & Profit Report (TEST MODE)")
    sb.appendLine("Total Users,${analytics.totalUsers}")
    sb.appendLine("Verified Users,${analytics.verifiedUsers}")
    sb.appendLine("Gross Revenue INR,${analytics.totalGrossRevenueInr}")
    sb.appendLine("Platform Commission INR,${analytics.totalPlatformCommissionInr}")
    sb.appendLine("Refunds INR,${analytics.totalRefundsInr}")
    sb.appendLine("Gateway Charges INR,${analytics.totalGatewayChargesInr}")
    sb.appendLine("Operating Costs INR,${analytics.estimatedOperatingCostsInr}")
    sb.appendLine("Net Profit INR,${analytics.netProfitInr}")
    sb.appendLine()
    sb.appendLine("txId,userId,type,category,grossInr,commissionInr,gatewayFeeInr,netInr,status,isTestMode")
    ledger.forEach { tx ->
      sb.appendLine(
        "${tx.id},${tx.userId},${tx.type},${tx.category},${tx.grossAmountInr},${tx.platformCommissionInr},${tx.gatewayFeeInr},${tx.netAmountInr},${tx.status},${tx.isTestMode}"
      )
    }
    return sb.toString()
  }

  private suspend fun writeAuditLog(actorUid: String, action: String, targetPath: String, summary: String) {
    try {
      db.collection("audit_logs")
        .add(
          mapOf(
            "actorUid" to actorUid,
            "action" to action,
            "targetPath" to targetPath.take(200),
            "summary" to summary.take(400),
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (_: Exception) {
      // Non-fatal logging fallback
    }
  }

  private companion object {
    const val BOOTSTRAPPED_ADMIN_EMAIL = "dashnitin110@gmail.com"
  }
}
