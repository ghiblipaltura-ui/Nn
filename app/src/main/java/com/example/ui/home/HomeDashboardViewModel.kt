package com.example.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyCheckInClaim
import com.example.data.model.LedgerTransaction
import com.example.data.model.MicroTask
import com.example.data.model.TaskSubmission
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class HomeDashboardViewModel(
  private val userRepository: UserRepository,
  private val walletRepository: WalletRepository,
  private val taskRepository: TaskRepository,
  private val currentUserId: String,
  private val preferredLanguage: String,
) : ViewModel() {

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  init {
    viewModelScope.launch {
      val user = Firebase.auth.currentUser
      if (user != null && user.uid == currentUserId) {
        userRepository.ensureUserProfileAndWallet(
          displayName = user.displayName ?: "EarnMore Member",
          email = user.email ?: "",
          preferredLanguage = preferredLanguage,
        )
      }
    }
  }

  val userProfile: StateFlow<UiState<UserProfile?>> =
    userRepository
      .observeCurrentUserProfile()
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapErrorMessage(error, "Failed to load profile"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val wallet: StateFlow<UiState<Wallet?>> =
    walletRepository
      .observeWallet()
      .map<Wallet?, UiState<Wallet?>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapErrorMessage(error, "Failed to load wallet"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val ledgerTransactions: StateFlow<UiState<List<LedgerTransaction>>> =
    walletRepository
      .observeLedgerTransactions()
      .map<List<LedgerTransaction>, UiState<List<LedgerTransaction>>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapErrorMessage(error, "Failed to load ledger"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val withdrawalRequests: StateFlow<UiState<List<WithdrawalRequest>>> =
    walletRepository
      .observeWithdrawalRequests()
      .map<List<WithdrawalRequest>, UiState<List<WithdrawalRequest>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(unwrapErrorMessage(error, "Failed to load withdrawals")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val dailyCheckIns: StateFlow<UiState<List<DailyCheckInClaim>>> =
    walletRepository
      .observeMyDailyCheckIns()
      .map<List<DailyCheckInClaim>, UiState<List<DailyCheckInClaim>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(unwrapErrorMessage(error, "Failed to load check-ins")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val openTasks: StateFlow<UiState<List<MicroTask>>> =
    taskRepository
      .observeOpenTasks()
      .map<List<MicroTask>, UiState<List<MicroTask>>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapErrorMessage(error, "Failed to load tasks"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  val mySubmissions: StateFlow<UiState<List<TaskSubmission>>> =
    taskRepository
      .observeMySubmissions()
      .map<List<TaskSubmission>, UiState<List<TaskSubmission>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(unwrapErrorMessage(error, "Failed to load submissions")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

  fun submitTaskProof(taskId: String, proofText: String, proofUrl: String) {
    viewModelScope.launch {
      val result = taskRepository.submitTaskProof(taskId, proofText, proofUrl.ifBlank { null })
      _actionMessage.value =
        if (result.isSuccess) "Proof submitted for admin review."
        else result.exceptionOrNull()?.localizedMessage ?: "Submission failed."
    }
  }

  fun requestTestModeWithdrawal(amountInr: Double, method: String, destination: String) {
    viewModelScope.launch {
      val result = walletRepository.requestTestModeWithdrawal(amountInr, method, destination)
      _actionMessage.value =
        if (result.isSuccess) "TEST MODE withdrawal request submitted."
        else result.exceptionOrNull()?.localizedMessage ?: "Withdrawal request rejected."
    }
  }

  fun claimDailyPromoBonus() {
    viewModelScope.launch {
      val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val result = walletRepository.submitDailyCheckInClaim(dateKey)
      _actionMessage.value =
        if (result.isSuccess) "Daily promo claim submitted for Admin verification."
        else result.exceptionOrNull()?.localizedMessage ?: "Check-in claim failed."
    }
  }

  fun updateProfileLanguage(language: String, displayName: String, phone: String) {
    viewModelScope.launch {
      val result =
        userRepository.updateSafeProfile(
          displayName = displayName.ifBlank { "EarnMore Member" },
          phone = phone,
          preferredLanguage = language,
          consentAccepted = true,
        )
      _actionMessage.value =
        if (result.isSuccess) "Profile preferences saved."
        else result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile."
    }
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  private fun unwrapErrorMessage(error: Throwable, fallback: String): String {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w("HomeDashboardVM", fallback, rootError)
    return rootError.message ?: fallback
  }
}
