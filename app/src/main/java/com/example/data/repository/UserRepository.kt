package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.security.OperationType
import com.example.data.security.handleFirestoreError
import com.google.firebase.Firebase
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

class UserRepository(
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
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun observeCurrentUserProfile(): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots().map { snap ->
        if (snap.exists()) {
          snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

  suspend fun ensureUserProfileAndWallet(
    displayName: String,
    email: String,
    preferredLanguage: String = "en",
    referredByUid: String? = null,
  ): Result<UserProfile> {
    return try {
      val uid = requireUserId()
      val userRef = db.collection("users").document(uid)
      val existingUser = userRef.get().await()
      if (!existingUser.exists()) {
        val codeSuffix = uid.take(6).uppercase()
        val referralCode = "EM$codeSuffix"
        val userPayload =
          buildMap<String, Any> {
            put("userId", uid)
            put("displayName", displayName.ifBlank { "EarnMore Member" }.take(80))
            put("email", email.ifBlank { "member@earnmore.local" })
            put("role", "USER")
            put("referralCode", referralCode)
            if (!referredByUid.isNullOrBlank() && referredByUid != uid) {
              put("referredByUid", referredByUid)
            }
            put("isVerified", false)
            put("kycStatus", "UNVERIFIED")
            put("preferredLanguage", if (preferredLanguage == "hi") "hi" else "en")
            put("consentAccepted", false)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        userRef.set(userPayload).await()
      }

      val walletRef = db.collection("wallets").document(uid)
      val existingWallet = walletRef.get().await()
      if (!existingWallet.exists()) {
        val walletPayload =
          mapOf(
            "userId" to uid,
            "availableBalanceInr" to 0.0,
            "pendingBalanceInr" to 0.0,
            "totalEarnedInr" to 0.0,
            "totalWithdrawnInr" to 0.0,
            "completedTasksCount" to 0L,
            "activeProjectsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        walletRef.set(walletPayload).await()
      }

      val profile =
        userRef.get().await()
          .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: throw IllegalStateException("Failed to deserialize created profile")

      if (profile.referralCode.isNotBlank()) {
        val refCodeDoc = db.collection("referral_codes").document(profile.referralCode)
        if (!refCodeDoc.get().await().exists()) {
          refCodeDoc.set(
            mapOf(
              "code" to profile.referralCode,
              "ownerUid" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      }

      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
      Result.failure(e)
    }
  }

  suspend fun updateSafeProfile(
    displayName: String,
    phone: String,
    preferredLanguage: String,
    consentAccepted: Boolean,
  ): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    return try {
      val updates =
        buildMap<String, Any> {
          put("displayName", displayName.trim().take(80))
          if (phone.isNotBlank()) put("phone", phone.trim().take(20))
          put("preferredLanguage", if (preferredLanguage == "hi") "hi" else "en")
          put("consentAccepted", consentAccepted)
          put("updatedAt", FieldValue.serverTimestamp())
        }
      userRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      Result.failure(e)
    }
  }
}
