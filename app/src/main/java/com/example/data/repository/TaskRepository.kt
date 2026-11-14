package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.MicroTask
import com.example.data.model.TaskSubmission
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

class TaskRepository(
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
      ?: throw IllegalStateException("User must be signed in with Google before accessing Tasks.")

  fun observeOpenTasks(): Flow<List<MicroTask>> = flow {
    val uid = requireUserId()
    val path = "tasks"
    emitAll(
      db.collection(path)
        .whereEqualTo("status", "OPEN")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(MicroTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

  fun observeMySubmissions(): Flow<List<TaskSubmission>> = flow {
    val uid = requireUserId()
    val path = "task_submissions"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(TaskSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

  suspend fun submitTaskProof(
    taskId: String,
    proofText: String,
    proofUrl: String? = null,
  ): Result<String> {
    val uid = requireUserId()
    // Deterministic per-user-per-task document ID prevents duplicate submissions for the same task
    val docId = "${taskId}_${uid}"
    val docRef = db.collection("task_submissions").document(docId)
    return try {
      require(taskId.isNotBlank()) { "Task ID is required" }
      require(proofText.trim().length in 3..1000) { "Proof text must be 3-1000 characters" }

      val payload =
        buildMap<String, Any> {
          put("taskId", taskId)
          put("userId", uid)
          put("proofText", proofText.trim())
          if (!proofUrl.isNullOrBlank()) {
            put("proofUrl", proofUrl.trim().take(400))
          }
          put("status", "SUBMITTED")
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }
}
