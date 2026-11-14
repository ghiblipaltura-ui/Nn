package com.example.ui.marketplace

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AffiliateEvent
import com.example.data.model.AffiliateOffer
import com.example.data.model.DisputeRefund
import com.example.data.model.FreelanceGig
import com.example.data.model.FreelanceOrder
import com.example.data.model.LocalService
import com.example.data.model.MicroTask
import com.example.data.model.Product
import com.example.data.model.ProductOrder
import com.example.data.model.ReferralRecord
import com.example.data.model.ServiceBooking
import com.example.data.model.SupportTicket
import com.example.data.model.TaskSubmission
import com.example.data.model.TutorBooking
import com.example.data.model.TutorCourse
import com.example.data.repository.GrowthAndSupportRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.TaskRepository
import com.example.ui.home.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EarnViewModel(
  private val taskRepository: TaskRepository,
  private val marketplaceRepository: MarketplaceRepository,
  private val growthRepository: GrowthAndSupportRepository,
) : ViewModel() {

  private val _feedback = MutableStateFlow<String?>(null)
  val feedback: StateFlow<String?> = _feedback.asStateFlow()

  private val _generatedAffiliateLink = MutableStateFlow<String?>(null)
  val generatedAffiliateLink: StateFlow<String?> = _generatedAffiliateLink.asStateFlow()

  val tasks: StateFlow<UiState<List<MicroTask>>> = taskRepository.observeOpenTasks().toUiState("tasks")
  val submissions: StateFlow<UiState<List<TaskSubmission>>> =
    taskRepository.observeMySubmissions().toUiState("submissions")

  val gigs: StateFlow<UiState<List<FreelanceGig>>> =
    marketplaceRepository.observeActiveGigs().toUiState("gigs")
  val freelanceOrders: StateFlow<UiState<List<FreelanceOrder>>> =
    marketplaceRepository.observeMyFreelanceOrders().toUiState("freelance orders")

  val products: StateFlow<UiState<List<Product>>> =
    marketplaceRepository.observeActiveProducts().toUiState("products")
  val productOrders: StateFlow<UiState<List<ProductOrder>>> =
    marketplaceRepository.observeMyProductOrders().toUiState("product orders")

  val courses: StateFlow<UiState<List<TutorCourse>>> =
    marketplaceRepository.observeActiveCourses().toUiState("courses")
  val tutorBookings: StateFlow<UiState<List<TutorBooking>>> =
    marketplaceRepository.observeMyTutorBookings().toUiState("tutor bookings")

  val localServices: StateFlow<UiState<List<LocalService>>> =
    marketplaceRepository.observeActiveLocalServices().toUiState("local services")
  val serviceBookings: StateFlow<UiState<List<ServiceBooking>>> =
    marketplaceRepository.observeMyServiceBookings().toUiState("service bookings")

  val affiliateOffers: StateFlow<UiState<List<AffiliateOffer>>> =
    growthRepository.observeAffiliateOffers().toUiState("affiliate offers")
  val affiliateEvents: StateFlow<UiState<List<AffiliateEvent>>> =
    growthRepository.observeMyAffiliateEvents().toUiState("affiliate events")

  val referrals: StateFlow<UiState<List<ReferralRecord>>> =
    growthRepository.observeMyReferrals().toUiState("referrals")

  val supportTickets: StateFlow<UiState<List<SupportTicket>>> =
    growthRepository.observeMySupportTickets().toUiState("support tickets")
  val disputes: StateFlow<UiState<List<DisputeRefund>>> =
    growthRepository.observeMyDisputes().toUiState("disputes")

  fun submitTaskProof(taskId: String, proofText: String, proofUrl: String) =
    launchAction("Proof submitted for verification.") {
      taskRepository.submitTaskProof(taskId, proofText, proofUrl.ifBlank { null })
    }

  fun createGig(
    sellerName: String,
    category: String,
    title: String,
    description: String,
    priceInr: Double,
    deliveryDays: Long,
  ) =
    launchAction("Freelance gig published.") {
      marketplaceRepository.createFreelanceGig(sellerName, category, title, description, priceInr, deliveryDays)
    }

  fun orderGig(gig: FreelanceGig, requirements: String) =
    launchAction("TEST MODE freelance order placed.") {
      marketplaceRepository.placeFreelanceOrder(gig, requirements)
    }

  fun updateFreelanceOrder(orderId: String, status: String, note: String? = null) =
    launchAction("Order updated to $status.") {
      marketplaceRepository.updateFreelanceOrderStatus(orderId, status, note)
    }

  fun createProduct(
    sellerName: String,
    category: String,
    title: String,
    description: String,
    priceInr: Double,
    stock: Long,
  ) =
    launchAction("Product listed on marketplace.") {
      marketplaceRepository.createProduct(sellerName, category, title, description, priceInr, stock)
    }

  fun orderProduct(product: Product, qty: Long, area: String) =
    launchAction("TEST MODE product order placed.") {
      marketplaceRepository.placeProductOrder(product, qty, area)
    }

  fun updateProductTracking(orderId: String, status: String) =
    launchAction("Tracking updated to $status.") {
      marketplaceRepository.updateProductOrderTracking(orderId, status)
    }

  fun createCourse(
    tutorName: String,
    subject: String,
    title: String,
    description: String,
    hourlyRateInr: Double,
    mode: String,
  ) =
    launchAction("Course published to Teaching Marketplace.") {
      marketplaceRepository.createTutorCourse(tutorName, subject, title, description, hourlyRateInr, mode)
    }

  fun bookCourse(course: TutorCourse, slot: String) =
    launchAction("TEST MODE session booked.") {
      marketplaceRepository.bookTutorCourse(course, slot)
    }

  fun updateTutorBooking(bookingId: String, status: String) =
    launchAction("Booking updated to $status.") {
      marketplaceRepository.updateTutorBookingStatus(bookingId, status)
    }

  fun createService(
    providerName: String,
    serviceType: String,
    city: String,
    description: String,
    rateInr: Double,
  ) =
    launchAction("Local service published.") {
      marketplaceRepository.createLocalService(providerName, serviceType, city, description, rateInr)
    }

  fun bookService(service: LocalService, area: String, notes: String) =
    launchAction("Service request submitted (TEST MODE).") {
      marketplaceRepository.bookLocalService(service, area, notes)
    }

  fun updateServiceBooking(bookingId: String, status: String, confirmed: Boolean? = null) =
    launchAction("Service status updated to $status.") {
      marketplaceRepository.updateServiceBookingStatus(bookingId, status, confirmed)
    }

  fun generateAffiliateLink(offer: AffiliateOffer) {
    viewModelScope.launch {
      val result = growthRepository.createAffiliateTrackingLink(offer)
      if (result.isSuccess) {
        _generatedAffiliateLink.value = result.getOrNull()
        _feedback.value = "Unique tracking link generated."
      } else {
        _feedback.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to create link."
      }
    }
  }

  fun applyReferralCode(code: String) =
    launchAction("Referral linked! Bonus unlocks after verified eligible activity.") {
      growthRepository.applyReferralCode(code)
    }

  fun submitTicket(subject: String, category: String, message: String) =
    launchAction("Support ticket submitted.") {
      growthRepository.submitSupportTicket(subject, category, message)
    }

  fun submitDispute(orderId: String, category: String, reason: String, amountInr: Double) =
    launchAction("Dispute submitted for admin review.") {
      growthRepository.submitDisputeRefund(orderId, category, reason, amountInr)
    }

  fun clearFeedback() {
    _feedback.value = null
  }

  fun clearGeneratedLink() {
    _generatedAffiliateLink.value = null
  }

  private fun launchAction(successMsg: String, block: suspend () -> Result<Any>) {
    viewModelScope.launch {
      val res = block()
      _feedback.value =
        if (res.isSuccess) successMsg
        else res.exceptionOrNull()?.localizedMessage ?: "Operation failed."
    }
  }

  private fun <T> Flow<List<T>>.toUiState(label: String): StateFlow<UiState<List<T>>> =
    this.map<List<T>, UiState<List<T>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w("EarnVM", "Failed loading $label", root)
        emit(UiState.Error(root.message ?: "Failed to load $label"))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
}
