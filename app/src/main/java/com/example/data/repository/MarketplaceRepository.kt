package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CommissionConfig
import com.example.data.model.FreelanceGig
import com.example.data.model.FreelanceOrder
import com.example.data.model.LocalService
import com.example.data.model.Product
import com.example.data.model.ProductOrder
import com.example.data.model.ServiceBooking
import com.example.data.model.TutorBooking
import com.example.data.model.TutorCourse
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MarketplaceRepository(
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
      ?: throw IllegalStateException("Sign in required before accessing Marketplace.")

  fun observeCommissionConfigs(): Flow<List<CommissionConfig>> = flow {
    val uid = requireUserId()
    val path = "commission_configs"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(CommissionConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  private suspend fun getCommissionPercent(category: String, defaultPercent: Double): Double {
    return try {
      val doc = db.collection("commission_configs").document(category).get().await()
      if (doc.exists()) doc.getDouble("commissionPercent") ?: defaultPercent else defaultPercent
    } catch (_: Exception) {
      defaultPercent
    }
  }

  // --- FREELANCING ---
  fun observeActiveGigs(): Flow<List<FreelanceGig>> = flow {
    val uid = requireUserId()
    val path = "freelance_gigs"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(FreelanceGig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  fun observeMyFreelanceOrders(): Flow<List<FreelanceOrder>> = flow {
    val uid = requireUserId()
    val path = "freelance_orders"
    val buyerFlow =
      db.collection(path).whereEqualTo("buyerId", uid).snapshots().map {
        it.toObjects(FreelanceOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    val sellerFlow =
      db.collection(path).whereEqualTo("sellerId", uid).snapshots().map {
        it.toObjects(FreelanceOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    emitAll(
      combine(buyerFlow, sellerFlow) { b, s ->
        (b + s).distinctBy { it.id }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }.catch { e -> handleStreamError(e, uid, path) }
    )
  }

  suspend fun createFreelanceGig(
    sellerName: String,
    category: String,
    title: String,
    description: String,
    priceInr: Double,
    deliveryDays: Long,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("freelance_gigs").document()
    return try {
      val payload =
        mapOf(
          "sellerId" to uid,
          "sellerName" to sellerName.trim().ifBlank { "Freelancer" }.take(80),
          "category" to category,
          "title" to title.trim().take(120),
          "description" to description.trim().take(1200),
          "priceInr" to priceInr,
          "deliveryDays" to deliveryDays.coerceIn(1L, 60L),
          "isActive" to true,
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

  suspend fun placeFreelanceOrder(gig: FreelanceGig, requirements: String): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("freelance_orders").document()
    return try {
      require(gig.sellerId != uid) { "You cannot order your own freelance gig." }
      val feePercent = getCommissionPercent("FREELANCE", 10.0)
      val breakdown = CommissionCalculator.calculate(gig.priceInr, feePercent)
      val payload =
        mapOf(
          "gigId" to gig.id,
          "buyerId" to uid,
          "sellerId" to gig.sellerId,
          "grossAmountInr" to breakdown.grossAmountInr,
          "platformCommissionInr" to breakdown.platformCommissionInr,
          "sellerNetInr" to breakdown.providerNetInr,
          "requirements" to requirements.trim().take(1000),
          "status" to "PLACED",
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

  suspend fun updateFreelanceOrderStatus(
    orderId: String,
    newStatus: String,
    deliveryNote: String? = null,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("freelance_orders").document(orderId)
    return try {
      val updates =
        buildMap<String, Any> {
          put("status", newStatus)
          if (!deliveryNote.isNullOrBlank()) put("deliveryNote", deliveryNote.trim().take(1000))
          put("updatedAt", FieldValue.serverTimestamp())
        }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- PRODUCT MARKETPLACE ---
  fun observeActiveProducts(): Flow<List<Product>> = flow {
    val uid = requireUserId()
    val path = "products"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  fun observeMyProductOrders(): Flow<List<ProductOrder>> = flow {
    val uid = requireUserId()
    val path = "product_orders"
    val buyerFlow =
      db.collection(path).whereEqualTo("buyerId", uid).snapshots().map {
        it.toObjects(ProductOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    val sellerFlow =
      db.collection(path).whereEqualTo("sellerId", uid).snapshots().map {
        it.toObjects(ProductOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    emitAll(
      combine(buyerFlow, sellerFlow) { b, s ->
        (b + s).distinctBy { it.id }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }.catch { e -> handleStreamError(e, uid, path) }
    )
  }

  suspend fun createProduct(
    sellerName: String,
    category: String,
    title: String,
    description: String,
    priceInr: Double,
    stock: Long,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("products").document()
    return try {
      val payload =
        mapOf(
          "sellerId" to uid,
          "sellerName" to sellerName.trim().ifBlank { "Seller" }.take(80),
          "category" to category.trim().take(50),
          "title" to title.trim().take(120),
          "description" to description.trim().take(1200),
          "priceInr" to priceInr,
          "stock" to stock.coerceIn(0L, 100000L),
          "isActive" to true,
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

  suspend fun placeProductOrder(
    product: Product,
    quantity: Long,
    shippingArea: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("product_orders").document()
    return try {
      require(product.sellerId != uid) { "You cannot purchase your own product listing." }
      val gross = product.priceInr * quantity
      val feePercent = getCommissionPercent("PRODUCTS", 8.0)
      val breakdown = CommissionCalculator.calculate(gross, feePercent)
      val payload =
        mapOf(
          "productId" to product.id,
          "buyerId" to uid,
          "sellerId" to product.sellerId,
          "quantity" to quantity,
          "grossAmountInr" to breakdown.grossAmountInr,
          "platformCommissionInr" to breakdown.platformCommissionInr,
          "sellerNetInr" to breakdown.providerNetInr,
          "shippingArea" to shippingArea.trim().take(160),
          "trackingStatus" to "ORDERED",
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

  suspend fun updateProductOrderTracking(orderId: String, newStatus: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("product_orders").document(orderId)
    return try {
      docRef
        .update(mapOf("trackingStatus" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- TEACHING MARKETPLACE ---
  fun observeActiveCourses(): Flow<List<TutorCourse>> = flow {
    val uid = requireUserId()
    val path = "tutor_courses"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(TutorCourse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  fun observeMyTutorBookings(): Flow<List<TutorBooking>> = flow {
    val uid = requireUserId()
    val path = "tutor_bookings"
    val studentFlow =
      db.collection(path).whereEqualTo("studentId", uid).snapshots().map {
        it.toObjects(TutorBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    val tutorFlow =
      db.collection(path).whereEqualTo("tutorId", uid).snapshots().map {
        it.toObjects(TutorBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    emitAll(
      combine(studentFlow, tutorFlow) { st, tu ->
        (st + tu).distinctBy { it.id }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }.catch { e -> handleStreamError(e, uid, path) }
    )
  }

  suspend fun createTutorCourse(
    tutorName: String,
    subject: String,
    title: String,
    description: String,
    hourlyRateInr: Double,
    mode: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("tutor_courses").document()
    return try {
      val payload =
        mapOf(
          "tutorId" to uid,
          "tutorName" to tutorName.trim().ifBlank { "Tutor" }.take(80),
          "subject" to subject.trim().take(60),
          "title" to title.trim().take(120),
          "description" to description.trim().take(1000),
          "hourlyRateInr" to hourlyRateInr,
          "mode" to mode,
          "isActive" to true,
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

  suspend fun bookTutorCourse(course: TutorCourse, scheduledSlot: String): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("tutor_bookings").document()
    return try {
      require(course.tutorId != uid) { "You cannot book your own course." }
      val feePercent = getCommissionPercent("TEACHING", 12.0)
      val breakdown = CommissionCalculator.calculate(course.hourlyRateInr, feePercent)
      val payload =
        mapOf(
          "courseId" to course.id,
          "studentId" to uid,
          "tutorId" to course.tutorId,
          "scheduledSlot" to scheduledSlot.trim().take(80),
          "grossAmountInr" to breakdown.grossAmountInr,
          "platformCommissionInr" to breakdown.platformCommissionInr,
          "tutorNetInr" to breakdown.providerNetInr,
          "status" to "BOOKED",
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

  suspend fun updateTutorBookingStatus(bookingId: String, newStatus: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("tutor_bookings").document(bookingId)
    return try {
      docRef
        .update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- LOCAL SERVICES ---
  fun observeActiveLocalServices(): Flow<List<LocalService>> = flow {
    val uid = requireUserId()
    val path = "local_services"
    emitAll(
      db.collection(path)
        .whereEqualTo("isActive", true)
        .snapshots()
        .map { snap ->
          snap.toObjects(LocalService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { e -> handleStreamError(e, uid, path) }
    )
  }

  fun observeMyServiceBookings(): Flow<List<ServiceBooking>> = flow {
    val uid = requireUserId()
    val path = "service_bookings"
    val customerFlow =
      db.collection(path).whereEqualTo("customerId", uid).snapshots().map {
        it.toObjects(ServiceBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    val providerFlow =
      db.collection(path).whereEqualTo("providerId", uid).snapshots().map {
        it.toObjects(ServiceBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    emitAll(
      combine(customerFlow, providerFlow) { c, p ->
        (c + p).distinctBy { it.id }.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }.catch { e -> handleStreamError(e, uid, path) }
    )
  }

  suspend fun createLocalService(
    providerName: String,
    serviceType: String,
    city: String,
    description: String,
    rateInr: Double,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("local_services").document()
    return try {
      val payload =
        mapOf(
          "providerId" to uid,
          "providerName" to providerName.trim().ifBlank { "Service Provider" }.take(80),
          "serviceType" to serviceType.trim().take(60),
          "city" to city.trim().take(60),
          "description" to description.trim().take(1000),
          "rateInr" to rateInr,
          "isActive" to true,
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

  suspend fun bookLocalService(
    service: LocalService,
    cityArea: String,
    notes: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("service_bookings").document()
    return try {
      require(service.providerId != uid) { "You cannot book your own local service." }
      val feePercent = getCommissionPercent("SERVICES", 10.0)
      val breakdown = CommissionCalculator.calculate(service.rateInr, feePercent)
      val payload =
        buildMap<String, Any> {
          put("serviceId", service.id)
          put("customerId", uid)
          put("providerId", service.providerId)
          put("cityArea", cityArea.trim().take(120))
          if (notes.isNotBlank()) put("notes", notes.trim().take(500))
          put("grossAmountInr", breakdown.grossAmountInr)
          put("platformCommissionInr", breakdown.platformCommissionInr)
          put("providerNetInr", breakdown.providerNetInr)
          put("customerConfirmed", false)
          put("status", "REQUESTED")
          put("isTestMode", true)
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

  suspend fun updateServiceBookingStatus(
    bookingId: String,
    newStatus: String,
    customerConfirmed: Boolean? = null,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("service_bookings").document(bookingId)
    return try {
      val updates =
        buildMap<String, Any> {
          put("status", newStatus)
          if (customerConfirmed != null) put("customerConfirmed", customerConfirmed)
          put("updatedAt", FieldValue.serverTimestamp())
        }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
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
