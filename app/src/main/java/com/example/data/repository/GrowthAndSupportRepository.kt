package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AffiliateEvent
import com.example.data.model.AffiliateOffer
import com.example.data.model.DisputeRefund
import com.example.data.model.ReferralRecord
import com.example.data.model.SupportTicket
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GrowthAndSupportRepository(
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
      ?: throw IllegalStateException("Sign in required before accessing Growth & Support.")

  // --- AFFILIATE MARKETPLACE ---
  fun observeAffiliateOffers(): Flow<List<AffiliateOffer>> = flow {
    val uid = requireUserId()
    val path = "affiliate_offers"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(AffiliateOffer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  fun observeMyAffiliateEvents(): Flow<List<AffiliateEvent>> = flow {
    val uid = requireUserId()
    val path = "affiliate_events"
    emitAll(
      db.collection(path)
        .whereEqualTo("promoterUid", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(AffiliateEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  suspend fun createAffiliateTrackingLink(offer: AffiliateOffer): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("affiliate_events").document()
    return try {
      val trackingCode = "EM_${uid.take(6).uppercase()}_${offer.id.take(6).uppercase()}_${UUID.randomUUID().toString().take(4).uppercase()}"
      val payload =
        mapOf(
          "offerId" to offer.id,
          "promoterUid" to uid,
          "trackingCode" to trackingCode,
          "status" to "CLICKED",
          "saleAmountInr" to 0.0,
          "grossCommissionInr" to 0.0,
          "userPayoutInr" to 0.0,
          "platformNetInr" to 0.0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success("${offer.baseUrl}?ref=$trackingCode")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- SINGLE-TIER REFERRALS ---
  fun observeMyReferrals(): Flow<List<ReferralRecord>> = flow {
    val uid = requireUserId()
    val path = "referrals"
    val asReferrer =
      db.collection(path).whereEqualTo("referrerUid", uid).snapshots().map {
        it.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    val asReferee =
      db.collection(path).whereEqualTo("refereeUid", uid).snapshots().map {
        it.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    emitAll(
      combine(asReferrer, asReferee) { r1, r2 ->
        (r1 + r2).distinctBy { it.id }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }.catch { e -> handleStreamError(e, uid, path) }
    )
  }

  suspend fun applyReferralCode(rawCode: String): Result<Unit> {
    val uid = requireUserId()
    val cleanCode = rawCode.trim().uppercase()
    val referralDocRef = db.collection("referrals").document("ref_$uid")
    return try {
      require(cleanCode.matches(Regex("^[A-Z0-9]{4,24}$"))) {
        "Referral code must be 4-24 alphanumeric characters."
      }
      if (referralDocRef.get().await().exists()) {
        throw IllegalStateException("You have already linked a referral code.")
      }
      val lookupSnap = db.collection("referral_codes").document(cleanCode).get().await()
      if (!lookupSnap.exists()) {
        throw IllegalArgumentException("Referral code not found.")
      }
      val referrerUid = lookupSnap.getString("ownerUid") ?: throw IllegalStateException("Invalid code.")
      if (referrerUid == uid) {
        throw IllegalArgumentException("Self-referral is blocked by anti-fraud policy.")
      }

      val payload =
        mapOf(
          "referrerUid" to referrerUid,
          "refereeUid" to uid,
          "referralCode" to cleanCode,
          "status" to "SIGNED_UP",
          "rewardInr" to 0.0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      referralDocRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, referralDocRef.path)
      Result.failure(e)
    }
  }

  // --- SUPPORT & DISPUTES ---
  fun observeMySupportTickets(): Flow<List<SupportTicket>> = flow {
    val uid = requireUserId()
    val path = "support_tickets"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  fun observeMyDisputes(): Flow<List<DisputeRefund>> = flow {
    val uid = requireUserId()
    val path = "disputes_refunds"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snap ->
          snap.toObjects(DisputeRefund::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  suspend fun submitSupportTicket(
    subject: String,
    category: String,
    message: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("support_tickets").document()
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "subject" to subject.trim().take(120),
          "category" to category.trim().take(40),
          "message" to message.trim().take(1000),
          "status" to "OPEN",
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

  suspend fun submitDisputeRefund(
    relatedOrderId: String,
    category: String,
    reason: String,
    requestedRefundInr: Double,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("disputes_refunds").document()
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "relatedOrderId" to relatedOrderId.trim().take(128),
          "category" to category.trim().take(40),
          "reason" to reason.trim().take(1000),
          "requestedRefundInr" to requestedRefundInr,
          "status" to "OPEN",
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

  private fun handleStreamError(error: Throwable, uid: String, path: String) {
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
}
