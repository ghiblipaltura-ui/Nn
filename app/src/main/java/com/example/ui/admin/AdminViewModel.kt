package com.example.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AffiliateEvent
import com.example.data.model.AuditLog
import com.example.data.model.CommissionConfig
import com.example.data.model.DailyCheckInClaim
import com.example.data.model.DisputeRefund
import com.example.data.model.FraudFlag
import com.example.data.model.LedgerTransaction
import com.example.data.model.SupportTicket
import com.example.data.model.TaskSubmission
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.AdminDashboardSnapshot
import com.example.data.repository.AdminRepository
import com.example.data.repository.MarketplaceRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
  private val adminRepository: AdminRepository,
  marketplaceRepository: MarketplaceRepository,
) : ViewModel() {

  private val _feedback = MutableStateFlow<String?>(null)
  val feedback: StateFlow<String?> = _feedback.asStateFlow()

  val users: StateFlow<List<UserProfile>> = adminRepository.observeAllUsers().safeState(emptyList())
  val ledger: StateFlow<List<LedgerTransaction>> =
    adminRepository.observeAllLedger().safeState(emptyList())
  val withdrawals: StateFlow<List<WithdrawalRequest>> =
    adminRepository.observeAllWithdrawals().safeState(emptyList())
  val submissions: StateFlow<List<TaskSubmission>> =
    adminRepository.observeAllSubmissions().safeState(emptyList())
  val disputes: StateFlow<List<DisputeRefund>> =
    adminRepository.observeAllDisputes().safeState(emptyList())
  val tickets: StateFlow<List<SupportTicket>> =
    adminRepository.observeAllTickets().safeState(emptyList())
  val fraudFlags: StateFlow<List<FraudFlag>> =
    adminRepository.observeAllFraudFlags().safeState(emptyList())
  val auditLogs: StateFlow<List<AuditLog>> =
    adminRepository.observeAllAuditLogs().safeState(emptyList())
  val affiliateEvents: StateFlow<List<AffiliateEvent>> =
    adminRepository.observeAllAffiliateEvents().safeState(emptyList())
  val commissionConfigs: StateFlow<List<CommissionConfig>> =
    marketplaceRepository.observeCommissionConfigs().safeState(emptyList())
  val dailyCheckIns: StateFlow<List<DailyCheckInClaim>> =
    adminRepository.observeAllDailyCheckIns().safeState(emptyList())

  val analytics: StateFlow<AdminDashboardSnapshot> =
    combine(users, ledger, withdrawals, disputes) { u, l, w, d ->
        adminRepository.computeAnalytics(u, l, w, d)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminDashboardSnapshot())

  fun seedCatalog() =
    launchAdmin("Starter tasks, commission configs & affiliate offer published.") {
      adminRepository.seedInitialMarketplaceCatalog()
    }

  fun createTask(
    title: String,
    titleHi: String,
    category: String,
    description: String,
    grossInr: Double,
    commissionPercent: Double,
    slots: Long,
  ) =
    launchAdmin("New microtask published.") {
      adminRepository.createMicroTask(title, titleHi, category, description, grossInr, commissionPercent, slots)
    }

  fun approveSubmission(sub: TaskSubmission) =
    launchAdmin("Submission approved & wallet credited.") {
      adminRepository.approveTaskSubmission(sub)
    }

  fun rejectSubmission(subId: String, reason: String) =
    launchAdmin("Submission rejected.") {
      adminRepository.rejectTaskSubmission(subId, reason)
    }

  fun processWithdrawal(req: WithdrawalRequest, approve: Boolean) =
    launchAdmin(if (approve) "TEST MODE payout marked PAID_TEST." else "Withdrawal rejected.") {
      adminRepository.processWithdrawal(req, approve)
    }

  fun processDailyCheckIn(claim: DailyCheckInClaim, approve: Boolean) =
    launchAdmin(if (approve) "Daily promo bonus funded (TEST MODE)." else "Promo claim rejected.") {
      adminRepository.processDailyCheckIn(claim, approve)
    }

  fun updateCommission(cat: String, comm: Double, gw: Double, op: Double) =
    launchAdmin("Updated $cat commission to $comm%.") {
      adminRepository.updateCommissionConfig(cat, comm, gw, op)
    }

  fun verifyUser(userId: String, verify: Boolean) =
    launchAdmin("User verification updated.") {
      adminRepository.verifyUserKyc(userId, verify)
    }

  fun verifyAffiliateSale(event: AffiliateEvent, saleAmountInr: Double) =
    launchAdmin("Affiliate sale verified & promoter credited.") {
      adminRepository.verifyAffiliateSale(event, saleAmountInr)
    }

  fun resolveDispute(id: String, status: String, note: String) =
    launchAdmin("Dispute marked $status.") {
      adminRepository.resolveDispute(id, status, note)
    }

  fun replyTicket(id: String, status: String, reply: String) =
    launchAdmin("Support ticket updated.") {
      adminRepository.replySupportTicket(id, status, reply)
    }

  fun buildCsvReport(): String = adminRepository.buildAnalyticsCsv(analytics.value, ledger.value)

  fun clearFeedback() {
    _feedback.value = null
  }

  private fun launchAdmin(msg: String, block: suspend () -> Result<Any>) {
    viewModelScope.launch {
      val res = block()
      _feedback.value =
        if (res.isSuccess) msg else res.exceptionOrNull()?.localizedMessage ?: "Admin action failed."
    }
  }

  private fun <T> Flow<T>.safeState(initial: T): StateFlow<T> =
    this.catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w("AdminVM", "Admin stream error", firestoreError ?: error)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)
}
