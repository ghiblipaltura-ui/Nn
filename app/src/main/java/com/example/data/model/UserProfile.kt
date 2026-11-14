package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val phone: String = "",
  val role: String = "USER",
  val referralCode: String = "",
  val referredByUid: String = "",
  val isVerified: Boolean = false,
  val kycStatus: String = "UNVERIFIED",
  val preferredLanguage: String = "en",
  val consentAccepted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
