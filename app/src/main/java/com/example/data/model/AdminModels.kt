package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AffiliateOffer(
  @DocumentId val id: String = "",
  val merchantName: String = "",
  val title: String = "",
  val category: String = "",
  val baseUrl: String = "",
  val totalCommissionPercent: Double = 10.0,
  val userSharePercent: Double = 70.0,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AffiliateEvent(
  @DocumentId val id: String = "",
  val offerId: String = "",
  val promoterUid: String = "",
  val trackingCode: String = "",
  val status: String = "CLICKED",
  val saleAmountInr: Double = 0.0,
  val grossCommissionInr: Double = 0.0,
  val userPayoutInr: Double = 0.0,
  val platformNetInr: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ReferralRecord(
  @DocumentId val id: String = "",
  val referrerUid: String = "",
  val refereeUid: String = "",
  val referralCode: String = "",
  val status: String = "SIGNED_UP",
  val rewardInr: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TutorCourse(
  @DocumentId val id: String = "",
  val tutorId: String = "",
  val tutorName: String = "",
  val subject: String = "",
  val title: String = "",
  val description: String = "",
  val hourlyRateInr: Double = 0.0,
  val mode: String = "LIVE_1ON1",
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TutorBooking(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val studentId: String = "",
  val tutorId: String = "",
  val scheduledSlot: String = "",
  val grossAmountInr: Double = 0.0,
  val platformCommissionInr: Double = 0.0,
  val tutorNetInr: Double = 0.0,
  val status: String = "BOOKED",
  val isTestMode: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LocalService(
  @DocumentId val id: String = "",
  val providerId: String = "",
  val providerName: String = "",
  val serviceType: String = "",
  val city: String = "",
  val description: String = "",
  val rateInr: Double = 0.0,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ServiceBooking(
  @DocumentId val id: String = "",
  val serviceId: String = "",
  val customerId: String = "",
  val providerId: String = "",
  val cityArea: String = "",
  val notes: String = "",
  val grossAmountInr: Double = 0.0,
  val platformCommissionInr: Double = 0.0,
  val providerNetInr: Double = 0.0,
  val customerConfirmed: Boolean = false,
  val status: String = "REQUESTED",
  val isTestMode: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CommissionConfig(
  @DocumentId val id: String = "",
  val category: String = "TASKS",
  val commissionPercent: Double = 15.0,
  val gatewayFeePercent: Double = 2.0,
  val operatingCostPercent: Double = 1.5,
  val isActive: Boolean = true,
  val updatedByUid: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DisputeRefund(
  @DocumentId val id: String = "",
  val userId: String = "",
  val relatedOrderId: String = "",
  val category: String = "",
  val reason: String = "",
  val requestedRefundInr: Double = 0.0,
  val status: String = "OPEN",
  val adminResolutionNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SupportTicket(
  @DocumentId val id: String = "",
  val userId: String = "",
  val subject: String = "",
  val message: String = "",
  val category: String = "",
  val status: String = "OPEN",
  val adminReply: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AuditLog(
  @DocumentId val id: String = "",
  val actorUid: String = "",
  val action: String = "",
  val targetPath: String = "",
  val summary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FraudFlag(
  @DocumentId val id: String = "",
  val userId: String = "",
  val reason: String = "",
  val severity: String = "MEDIUM",
  val status: String = "OPEN",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DailyCheckInClaim(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateKey: String = "",
  val requestedBonusInr: Double = 100.0,
  val status: String = "PENDING_ADMIN_FUNDING",
  val reviewNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

