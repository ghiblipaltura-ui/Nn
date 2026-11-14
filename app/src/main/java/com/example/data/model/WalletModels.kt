package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Wallet(
  @DocumentId val id: String = "",
  val userId: String = "",
  val availableBalanceInr: Double = 0.0,
  val pendingBalanceInr: Double = 0.0,
  val totalEarnedInr: Double = 0.0,
  val totalWithdrawnInr: Double = 0.0,
  val completedTasksCount: Long = 0L,
  val activeProjectsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LedgerTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val type: String = "CREDIT_TASK",
  val category: String = "TASKS",
  val grossAmountInr: Double = 0.0,
  val platformCommissionInr: Double = 0.0,
  val gatewayFeeInr: Double = 0.0,
  val netAmountInr: Double = 0.0,
  val status: String = "PENDING",
  val referenceId: String = "",
  val idempotencyKey: String = "",
  val description: String = "",
  val isTestMode: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WithdrawalRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val amountInr: Double = 0.0,
  val payoutMethod: String = "UPI_TEST",
  val maskedDestination: String = "",
  val status: String = "PENDING",
  val isTestMode: Boolean = true,
  val reviewNote: String = "",
  val reviewedByAdminId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
