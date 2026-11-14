package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DailyCheckInClaim
import com.example.data.model.LedgerTransaction
import com.example.data.model.Wallet
import com.example.data.model.WithdrawalRequest
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

class WalletRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Wallet.")

  fun observeWallet(): Flow<Wallet?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("wallets").document(uid)
    emitAll(
      docRef.snapshots().map { snap ->
        if (snap.exists()) {
          snap.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }.catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
          handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
        }
        throw error
      }
    )
  }

  fun observeLedgerTransactions(): Flow<List<LedgerTransaction>> = flow {
    val uid = requireUserId()
    val path = "ledger_transactions"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(LedgerTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
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

  fun observeWithdrawalRequests(): Flow<List<WithdrawalRequest>> = flow {
    val uid = requireUserId()
    val path = "withdrawal_requests"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
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

  suspend fun requestTestModeWithdrawal(
    amountInr: Double,
    payoutMethod: String,
    maskedDestination: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("withdrawal_requests").document()
    return try {
      require(amountInr in 100.0..50000.0) { "Withdrawal must be between ₹100 and ₹50,000." }
      require(payoutMethod in listOf("UPI_TEST", "BANK_TEST")) { "Invalid TEST MODE payout method." }
      require(maskedDestination.trim().length in 3..80) { "Invalid destination." }

      val payload =
        mapOf(
          "userId" to uid,
          "amountInr" to amountInr,
          "payoutMethod" to payoutMethod,
          "maskedDestination" to maskedDestination.trim(),
          "status" to "PENDING",
          "isTestMode" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeMyDailyCheckIns(): Flow<List<DailyCheckInClaim>> = flow {
    val uid = requireUserId()
    val path = "daily_checkins"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(DailyCheckInClaim::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
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

  suspend fun submitDailyCheckInClaim(dateKey: String): Result<String> {
    val uid = requireUserId()
    val cleanDate = dateKey.trim()
    val claimId = "checkin_${uid}_$cleanDate"
    val docRef = db.collection("daily_checkins").document(claimId)
    return try {
      require(cleanDate.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
        "Invalid check-in date format."
      }
      if (docRef.get().await().exists()) {
        throw IllegalStateException("Today's check-in claim has already been submitted.")
      }
      val payload =
        mapOf(
          "userId" to uid,
          "dateKey" to cleanDate,
          "requestedBonusInr" to 100.0,
          "status" to "PENDING_ADMIN_FUNDING",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(claimId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }
}
