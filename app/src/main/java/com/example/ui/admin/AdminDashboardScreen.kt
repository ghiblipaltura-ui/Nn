package com.example.ui.admin

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import java.util.Locale

private enum class AdminSection(val label: String) {
  OVERVIEW("Analytics"),
  TASKS("Tasks & Approvals"),
  PAYOUTS("Withdrawals"),
  COMMISSIONS("Commissions"),
  GOVERNANCE("Users, Disputes & Logs"),
}

@Composable
fun AdminDashboardScreen(
  adminViewModel: AdminViewModel,
) {
  val context = LocalContext.current
  val analytics by adminViewModel.analytics.collectAsStateWithLifecycle()
  val users by adminViewModel.users.collectAsStateWithLifecycle()
  val submissions by adminViewModel.submissions.collectAsStateWithLifecycle()
  val withdrawals by adminViewModel.withdrawals.collectAsStateWithLifecycle()
  val dailyCheckIns by adminViewModel.dailyCheckIns.collectAsStateWithLifecycle()
  val configs by adminViewModel.commissionConfigs.collectAsStateWithLifecycle()
  val disputes by adminViewModel.disputes.collectAsStateWithLifecycle()
  val tickets by adminViewModel.tickets.collectAsStateWithLifecycle()
  val affiliateEvents by adminViewModel.affiliateEvents.collectAsStateWithLifecycle()
  val fraudFlags by adminViewModel.fraudFlags.collectAsStateWithLifecycle()
  val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()

  var section by remember { mutableStateOf(AdminSection.OVERVIEW) }
  var showCreateTaskDialog by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text("Owner & Admin Control Center", style = MaterialTheme.typography.headlineSmall)
      Text(
        text = "RBAC Verified • Server-side Commission & Profit Ledger",
        style = MaterialTheme.typography.labelSmall,
        color = EmeraldAccent,
      )
      Spacer(Modifier.height(10.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AdminSection.entries.forEach { s ->
          FilterChip(
            selected = section == s,
            onClick = { section = s },
            label = { Text(s.label) },
            modifier = Modifier.testTag("admin_tab_${s.name.lowercase()}"),
          )
        }
      }
    }

    when (section) {
      AdminSection.OVERVIEW -> {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyPrimary),
          ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Unit Economics & Net Profit", style = MaterialTheme.typography.titleMedium, color = GoldAccent)
              Text(
                text = "Net Profit: ${formatInr(analytics.netProfitInr)}",
                style = MaterialTheme.typography.displaySmall,
                color = EmeraldAccent,
              )
              Text(
                text = "Gross Revenue: ${formatInr(analytics.totalGrossRevenueInr)} • Platform Commission: ${formatInr(analytics.totalPlatformCommissionInr)}",
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite,
              )
              Text(
                text = "User Payouts: ${formatInr(analytics.totalUserPayoutsInr)} • Gateway Fees: ${formatInr(analytics.totalGatewayChargesInr)} • Operating Costs: ${formatInr(analytics.estimatedOperatingCostsInr)}",
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite.copy(alpha = 0.85f),
              )
              Text(
                text = "Refunds: ${formatInr(analytics.totalRefundsInr)} • Users: ${analytics.totalUsers} (${analytics.verifiedUsers} KYC) • Txns: ${analytics.totalTransactions} • Pending Payouts: ${analytics.pendingWithdrawalsCount}",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.78f),
              )
            }
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = { adminViewModel.seedCatalog() },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
              modifier = Modifier.weight(1f).testTag("admin_seed_catalog_button"),
            ) {
              Text("Seed Starter Catalog")
            }
            OutlinedButton(
              onClick = {
                val csv = adminViewModel.buildCsvReport()
                val share = Intent(Intent.ACTION_SEND).apply {
                  type = "text/csv"
                  putExtra(Intent.EXTRA_SUBJECT, "EarnMore India Profit & Ledger Report")
                  putExtra(Intent.EXTRA_TEXT, csv)
                }
                context.startActivity(Intent.createChooser(share, "Export CSV Report"))
              },
              modifier = Modifier.weight(1f).testTag("admin_export_csv_button"),
            ) {
              Text("Export CSV")
            }
          }
        }
      }

      AdminSection.TASKS -> {
        item {
          Button(
            onClick = { showCreateTaskDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
            modifier = Modifier.fillMaxWidth().testTag("admin_create_task_button"),
          ) {
            Text("+ Publish New Microtask")
          }
        }
        val pendingSubs = submissions.filter { it.status == "SUBMITTED" }
        item {
          Text("Pending Task Submissions (${pendingSubs.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (pendingSubs.isEmpty()) {
          item { Card(Modifier.fillMaxWidth()) { Text("No pending proof submissions.", Modifier.padding(16.dp)) } }
        } else {
          items(pendingSubs, key = { it.id }) { sub ->
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(14.dp)) {
                Text("Task #${sub.taskId.take(8)} • User ${sub.userId.take(8)}", style = MaterialTheme.typography.titleMedium)
                Text("Proof: ${sub.proofText}", style = MaterialTheme.typography.bodyMedium)
                if (sub.proofUrl.isNotBlank()) Text("URL: ${sub.proofUrl}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(onClick = { adminViewModel.approveSubmission(sub) }) { Text("Approve & Credit") }
                  OutlinedButton(onClick = { adminViewModel.rejectSubmission(sub.id, "Insufficient proof") }) { Text("Reject") }
                }
              }
            }
          }
        }
      }

      AdminSection.PAYOUTS -> {
        item { Text("Withdrawal Queue (${withdrawals.size})", style = MaterialTheme.typography.titleMedium) }
        if (withdrawals.isEmpty()) {
          item { Card(Modifier.fillMaxWidth()) { Text("No withdrawal requests yet.", Modifier.padding(16.dp)) } }
        } else {
          items(withdrawals, key = { it.id }) { req ->
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(14.dp)) {
                Text("${req.payoutMethod} • ${req.maskedDestination} • ${formatInr(req.amountInr)}", style = MaterialTheme.typography.titleMedium)
                Text("User: ${req.userId.take(10)} • Status: ${req.status}")
                if (req.status == "PENDING") {
                  Spacer(Modifier.height(8.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { adminViewModel.processWithdrawal(req, approve = true) }) {
                      Text("Settle (PAID_TEST)")
                    }
                    OutlinedButton(onClick = { adminViewModel.processWithdrawal(req, approve = false) }) {
                      Text("Reject")
                    }
                  }
                }
              }
            }
          }
        }
        val pendingPromos = dailyCheckIns.filter { it.status == "PENDING_ADMIN_FUNDING" }
        item { Text("Daily Promo Check-In Claims (${pendingPromos.size})", style = MaterialTheme.typography.titleMedium) }
        if (pendingPromos.isEmpty()) {
          item { Card(Modifier.fillMaxWidth()) { Text("No pending daily promo claims.", Modifier.padding(16.dp)) } }
        } else {
          items(pendingPromos, key = { it.id }) { claim ->
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(14.dp)) {
                Text("${claim.dateKey} • User ${claim.userId.take(10)} • ₹${claim.requestedBonusInr}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(onClick = { adminViewModel.processDailyCheckIn(claim, approve = true) }) {
                    Text("Fund ₹${claim.requestedBonusInr} (TEST)")
                  }
                  OutlinedButton(onClick = { adminViewModel.processDailyCheckIn(claim, approve = false) }) {
                    Text("Reject")
                  }
                }
              }
            }
          }
        }
      }

      AdminSection.COMMISSIONS -> {
        item { Text("Category Commission Rates", style = MaterialTheme.typography.titleMedium) }
        items(configs, key = { it.category }) { cfg ->
          var rateText by remember(cfg.category, cfg.commissionPercent) {
            mutableStateOf(cfg.commissionPercent.toString())
          }
          Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
              Text("${cfg.category} • Current: ${cfg.commissionPercent}% (GW ${cfg.gatewayFeePercent}%)", style = MaterialTheme.typography.titleMedium)
              Spacer(Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = rateText,
                  onValueChange = { rateText = it },
                  label = { Text("New %") },
                  modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                  val pct = rateText.toDoubleOrNull()
                  if (pct != null && pct in 0.0..50.0) {
                    adminViewModel.updateCommission(cfg.category, pct, cfg.gatewayFeePercent, cfg.operatingCostPercent)
                  }
                }) { Text("Save") }
              }
            }
          }
        }
      }

      AdminSection.GOVERNANCE -> {
        item { Text("Users & KYC Verification (${users.size})", style = MaterialTheme.typography.titleMedium) }
        items(users, key = { it.userId }) { u ->
          Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
              Column(Modifier.weight(1f)) {
                Text("${u.displayName} (${u.role})", style = MaterialTheme.typography.titleMedium)
                Text("${u.email} • KYC: ${u.kycStatus}", style = MaterialTheme.typography.bodyMedium)
              }
              OutlinedButton(onClick = { adminViewModel.verifyUser(u.userId, !u.isVerified) }) {
                Text(if (u.isVerified) "Revoke KYC" else "Verify KYC")
              }
            }
          }
        }

        val pendingAff = affiliateEvents.filter { it.status == "CLICKED" }
        if (pendingAff.isNotEmpty()) {
          item { Text("Verify Affiliate Conversions (${pendingAff.size})", style = MaterialTheme.typography.titleMedium) }
          items(pendingAff, key = { it.id }) { ev ->
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(14.dp)) {
                Text("Code: ${ev.trackingCode} • User ${ev.promoterUid.take(8)}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Button(onClick = { adminViewModel.verifyAffiliateSale(ev, 1000.0) }) {
                  Text("Verify ₹1,000 Sale (TEST)")
                }
              }
            }
          }
        }

        val openDisputes = disputes.filter { it.status == "OPEN" }
        if (openDisputes.isNotEmpty()) {
          item { Text("Open Disputes (${openDisputes.size})", style = MaterialTheme.typography.titleMedium) }
          items(openDisputes, key = { it.id }) { d ->
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(14.dp)) {
                Text("Order #${d.relatedOrderId} • ₹${d.requestedRefundInr}", style = MaterialTheme.typography.titleMedium)
                Text(d.reason)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(onClick = { adminViewModel.resolveDispute(d.id, "REFUNDED", "Refund approved by admin") }) { Text("Approve Refund") }
                  OutlinedButton(onClick = { adminViewModel.resolveDispute(d.id, "REJECTED", "Fulfillment verified") }) { Text("Reject") }
                }
              }
            }
          }
        }

        val openTickets = tickets.filter { it.status != "RESOLVED" }
        if (openTickets.isNotEmpty()) {
          item { Text("Support Tickets (${openTickets.size})", style = MaterialTheme.typography.titleMedium) }
          items(openTickets, key = { it.id }) { t ->
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(14.dp)) {
                Text("${t.subject} (${t.category})", style = MaterialTheme.typography.titleMedium)
                Text(t.message)
                Spacer(Modifier.height(6.dp))
                Button(onClick = { adminViewModel.replyTicket(t.id, "RESOLVED", "Reviewed and resolved by Admin Desk.") }) {
                  Text("Mark Resolved")
                }
              }
            }
          }
        }

        if (fraudFlags.isNotEmpty()) {
          item { Text("Fraud Alerts (${fraudFlags.size})", style = MaterialTheme.typography.titleMedium) }
          items(fraudFlags, key = { it.id }) { f ->
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(14.dp)) {
                Text("${f.severity} • ${f.reason}", style = MaterialTheme.typography.titleMedium)
                Text("User: ${f.userId} • Status: ${f.status}")
              }
            }
          }
        }

        item { Text("Recent Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium) }
        items(auditLogs.take(15), key = { it.id }) { log ->
          Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
              Text("${log.action} -> ${log.targetPath}", style = MaterialTheme.typography.labelMedium)
              Text(log.summary, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }

  if (showCreateTaskDialog) {
    var title by remember { mutableStateOf("") }
    var titleHi by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("GAME_TASK") }
    var desc by remember { mutableStateOf("") }
    var grossStr by remember { mutableStateOf("100") }
    var commStr by remember { mutableStateOf("15") }
    var slotsStr by remember { mutableStateOf("50") }

    AlertDialog(
      onDismissRequest = { showCreateTaskDialog = false },
      title = { Text("Publish New Microtask") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (English)") })
          OutlinedTextField(value = titleHi, onValueChange = { titleHi = it }, label = { Text("Title (Hindi - Optional)") })
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("GAME_TASK", "DATA_ENTRY", "CONTENT_WRITING", "IMAGE_CATEGORIZATION", "WEBSITE_TESTING", "RESEARCH").forEach { c ->
              FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c.replace("_", " ")) })
            }
          }
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Instructions") })
          OutlinedTextField(value = grossStr, onValueChange = { grossStr = it }, label = { Text("Gross Reward (₹)") })
          OutlinedTextField(value = commStr, onValueChange = { commStr = it }, label = { Text("Platform Commission (%)") })
          OutlinedTextField(value = slotsStr, onValueChange = { slotsStr = it }, label = { Text("Total Slots") })
        }
      },
      confirmButton = {
        val gross = grossStr.toDoubleOrNull() ?: 0.0
        val comm = commStr.toDoubleOrNull() ?: 15.0
        val slots = slotsStr.toLongOrNull() ?: 25L
        Button(
          onClick = {
            adminViewModel.createTask(title, titleHi, cat, desc, gross, comm, slots)
            showCreateTaskDialog = false
          },
          enabled = title.trim().length >= 3 && desc.trim().length >= 5 && gross >= 10.0,
        ) { Text("Publish") }
      },
      dismissButton = { TextButton(onClick = { showCreateTaskDialog = false }) { Text("Cancel") } },
    )
  }
}

private fun formatInr(amount: Double): String = String.format(Locale.US, "₹%.2f", amount)
