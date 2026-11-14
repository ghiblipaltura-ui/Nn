package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MicroTask(
  @DocumentId val id: String = "",
  val title: String = "",
  val titleHi: String = "",
  val category: String = "DATA_ENTRY",
  val description: String = "",
  val rewardGrossInr: Double = 0.0,
  val commissionPercent: Double = 15.0,
  val workerNetInr: Double = 0.0,
  val slotsTotal: Long = 10L,
  val slotsFilled: Long = 0L,
  val status: String = "OPEN",
  val createdByUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TaskSubmission(
  @DocumentId val id: String = "",
  val taskId: String = "",
  val userId: String = "",
  val proofText: String = "",
  val proofUrl: String = "",
  val status: String = "SUBMITTED",
  val reviewNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FreelanceGig(
  @DocumentId val id: String = "",
  val sellerId: String = "",
  val sellerName: String = "",
  val category: String = "GRAPHIC_DESIGN",
  val title: String = "",
  val description: String = "",
  val priceInr: Double = 0.0,
  val deliveryDays: Long = 3L,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FreelanceOrder(
  @DocumentId val id: String = "",
  val gigId: String = "",
  val buyerId: String = "",
  val sellerId: String = "",
  val grossAmountInr: Double = 0.0,
  val platformCommissionInr: Double = 0.0,
  val sellerNetInr: Double = 0.0,
  val requirements: String = "",
  val deliveryNote: String = "",
  val status: String = "PLACED",
  val isTestMode: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val sellerId: String = "",
  val sellerName: String = "",
  val category: String = "",
  val title: String = "",
  val description: String = "",
  val priceInr: Double = 0.0,
  val stock: Long = 0L,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ProductOrder(
  @DocumentId val id: String = "",
  val productId: String = "",
  val buyerId: String = "",
  val sellerId: String = "",
  val quantity: Long = 1L,
  val grossAmountInr: Double = 0.0,
  val platformCommissionInr: Double = 0.0,
  val sellerNetInr: Double = 0.0,
  val shippingArea: String = "",
  val trackingStatus: String = "ORDERED",
  val isTestMode: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
