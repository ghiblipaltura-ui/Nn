package com.example.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LedgerTransaction
import com.example.data.model.Wallet
import com.example.data.model.WithdrawalRequest
import com.example.data.security.PaymentGatewayAdapter
import com.example.ui.home.UiState
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import java.util.Locale

@Composable
fun WalletScreen(
  walletState: UiState<Wallet?>,
  ledgerState: UiState<List<LedgerTransaction>>,
  withdrawalsState: UiState<List<WithdrawalRequest>>,
  isHindi: Boolean,
  onRequestWithdrawal: (Double, String, String) -> Unit,
) {
  var showWithdrawDialog by remember { mutableStateOf(false) }
  var activeTab by remember { mutableStateOf(0) } // 0 = Ledger, 1 = Payouts

  val wallet = (walletState as? UiState.Success)?.data
  val available = wallet?.availableBalanceInr ?: 0.0

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Surface(
            color = GoldAccent.copy(alpha = 0.22f),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "TEST MODE • Simulated Indian UPI / Bank Sandbox",
              style = MaterialTheme.typography.labelSmall,
              color = GoldAccent,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Spacer(Modifier.height(10.dp))
          Text(
            text = if (isHindi) "उपलब्ध शेष" else "Available Balance",
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite.copy(alpha = 0.8f),
          )
          Text(
            text = formatInr(available),
            style = MaterialTheme.typography.displaySmall,
            color = PureWhite,
          )
          Spacer(Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Pending: ${formatInr(wallet?.pendingBalanceInr ?: 0.0)}",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite.copy(alpha = 0.85f),
            )
            Text(
              text = "Withdrawn: ${formatInr(wallet?.totalWithdrawnInr ?: 0.0)}",
              style = MaterialTheme.typography.bodyMedium,
              color = EmeraldAccent,
            )
          }
          Spacer(Modifier.height(14.dp))
          Button(
            onClick = { showWithdrawDialog = true },
            enabled = available >= 100.0,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
            modifier = Modifier.fillMaxWidth().testTag("wallet_withdraw_button"),
          ) {
            Text(
              if (available >= 100.0) {
                if (isHindi) "निकासी अनुरोध भेजें (TEST MODE)" else "Request Withdrawal (TEST MODE)"
              } else {
                if (isHindi) "न्यूनतम ₹100 शेष आवश्यक" else "Minimum ₹100 Available Required"
              }
            )
          }
        }
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = activeTab == 0,
          onClick = { activeTab = 0 },
          label = { Text(if (isHindi) "कमीशन लेज़र" else "Commission Ledger") },
        )
        FilterChip(
          selected = activeTab == 1,
          onClick = { activeTab = 1 },
          label = { Text(if (isHindi) "निकासी इतिहास" else "Payout History") },
        )
      }
    }

    if (activeTab == 0) {
      when (ledgerState) {
        is UiState.Loading ->
          item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        is UiState.Error ->
          item { Text(ledgerState.message, color = MaterialTheme.colorScheme.error) }
        is UiState.Success -> {
          if (ledgerState.data.isEmpty()) {
            item {
              Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text =
                    if (isHindi) "अभी कोई सत्यापित लेन-देन नहीं है।"
                    else "No ledger transactions yet. Complete tasks, gigs, or referrals to see transparent breakdowns.",
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(20.dp),
                )
              }
            }
          } else {
            items(ledgerState.data, key = { it.id }) { tx ->
              Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = tx.description,
                      style = MaterialTheme.typography.titleMedium,
                      modifier = Modifier.weight(1f),
                    )
                    Text(
                      text = (if (tx.type == "DEBIT_WITHDRAWAL") "-" else "+") + formatInr(tx.netAmountInr),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color =
                        if (tx.type == "DEBIT_WITHDRAWAL") MaterialTheme.colorScheme.error
                        else EmeraldAccent,
                    )
                  }
                  Spacer(Modifier.height(6.dp))
                  Text(
                    text =
                      "Gross: ${formatInr(tx.grossAmountInr)} • Platform Fee: ${formatInr(tx.platformCommissionInr)} • Gateway: ${formatInr(tx.gatewayFeeInr)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = "${tx.category} • ${tx.status} • ${if (tx.isTestMode) "TEST MODE" else "LIVE"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }
    } else {
      when (withdrawalsState) {
        is UiState.Loading ->
          item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        is UiState.Error ->
          item { Text(withdrawalsState.message, color = MaterialTheme.colorScheme.error) }
        is UiState.Success -> {
          if (withdrawalsState.data.isEmpty()) {
            item {
              Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = if (isHindi) "कोई निकासी अनुरोध नहीं है।" else "No withdrawal requests yet.",
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(20.dp),
                )
              }
            }
          } else {
            items(withdrawalsState.data, key = { it.id }) { req ->
              Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text("${req.payoutMethod} • ${req.maskedDestination}", style = MaterialTheme.typography.titleMedium)
                    Text(
                      text = formatInr(req.amountInr),
                      style = MaterialTheme.typography.labelMedium,
                      color = NavyPrimary,
                    )
                  }
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = "Status: ${req.status}" + if (req.reviewNote.isNotBlank()) " • ${req.reviewNote}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showWithdrawDialog) {
    var amountStr by remember { mutableStateOf("100") }
    var destination by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("UPI_TEST") }
    val isValidDestination =
      if (method == "UPI_TEST") {
        destination.trim().matches(Regex("^[a-zA-Z0-9._-]{2,}@[a-zA-Z0-9.-]{2,}$"))
      } else {
        destination.trim().length >= 5
      }
    AlertDialog(
      onDismissRequest = { showWithdrawDialog = false },
      title = { Text(if (isHindi) "UPI निकासी अनुरोध (TEST MODE)" else "UPI Payout Request (TEST MODE)") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text =
              if (isHindi) {
                "उपलब्ध शेष: ${formatInr(available)}। UPI प्राथमिक माध्यम है। सुरक्षा नियम: कभी भी UPI PIN, पासवर्ड या CVV दर्ज न करें। वास्तविक बैंक ट्रांसफर लाइव पेमेंट गेटवे के बाद सक्रिय होगा।"
              } else {
                "Available: ${formatInr(available)}. UPI is the default payout method. Never enter UPI PINs, bank passwords, or CVVs. Live UPI payouts require a funded payment gateway."
              },
            style = MaterialTheme.typography.bodyMedium,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = method == "UPI_TEST",
              onClick = { method = "UPI_TEST" },
              label = { Text("UPI ID (Default)") },
            )
            FilterChip(
              selected = method == "BANK_TEST",
              onClick = { method = "BANK_TEST" },
              label = { Text("Bank (TEST)") },
            )
          }
          OutlinedTextField(
            value = amountStr,
            onValueChange = { amountStr = it },
            label = { Text("Amount (₹100 - ₹50,000)") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = {
              Text(
                if (method == "UPI_TEST") "UPI ID (e.g. rahul@upi / 9876543210@ybl)"
                else "Account Last 4 + IFSC"
              )
            },
            isError = destination.isNotBlank() && !isValidDestination,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        Button(
          onClick = {
            onRequestWithdrawal(amount, method, PaymentGatewayAdapter.maskUpiOrAccount(destination))
            showWithdrawDialog = false
          },
          enabled = amount in 100.0..available && isValidDestination,
        ) {
          Text(if (isHindi) "UPI निकासी भेजें" else "Request UPI Payout")
        }
      },
      dismissButton = { TextButton(onClick = { showWithdrawDialog = false }) { Text("Cancel") } },
    )
  }
}

private fun formatInr(amount: Double): String = String.format(Locale.US, "₹%.2f", amount)
