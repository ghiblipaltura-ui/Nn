package com.example.ui.growth

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.AffiliateEvent
import com.example.data.model.AffiliateOffer
import com.example.data.model.DisputeRefund
import com.example.data.model.ReferralRecord
import com.example.data.model.SupportTicket
import com.example.ui.home.UiState
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite

@Composable
fun AffiliateMarketplaceScreen(
  offersState: UiState<List<AffiliateOffer>>,
  eventsState: UiState<List<AffiliateEvent>>,
  generatedLink: String?,
  onGenerateLink: (AffiliateOffer) -> Unit,
  onClearLink: () -> Unit,
) {
  val context = LocalContext.current
  val offers = (offersState as? UiState.Success)?.data ?: emptyList()
  val events = (eventsState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Text("Affiliate Marketplace", style = MaterialTheme.typography.headlineSmall)
      Text(
        text = "Generate unique tracking links. Commissions credit only on verified merchant sales.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (generatedLink != null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
          Column(Modifier.padding(16.dp)) {
            Text("Your Unique Tracking Link", style = MaterialTheme.typography.titleMedium)
            Text(generatedLink, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_TEXT, "Check out this verified offer on EarnMore India: $generatedLink")
                }
                context.startActivity(Intent.createChooser(share, "Share Affiliate Link"))
              }) { Text("Share Link") }
              TextButton(onClick = onClearLink) { Text("Dismiss") }
            }
          }
        }
      }
    }

    items(offers, key = { it.id }) { offer ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
          Text("${offer.merchantName} • ${offer.category}", style = MaterialTheme.typography.labelSmall)
          Text(offer.title, style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Merchant Fee: ${offer.totalCommissionPercent}% • Your Share: ${offer.userSharePercent}% of commission",
            style = MaterialTheme.typography.bodyMedium,
            color = EmeraldAccent,
          )
          Spacer(Modifier.height(8.dp))
          Button(onClick = { onGenerateLink(offer) }) { Text("Generate Tracking Link") }
        }
      }
    }

    item { Text("Commission & Conversion Reports (${events.size})", style = MaterialTheme.typography.titleLarge) }
    items(events, key = { it.id }) { ev ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
          Text("Code: ${ev.trackingCode} • ${ev.status}", style = MaterialTheme.typography.titleMedium)
          Text("Sale ₹${ev.saleAmountInr} • Your Payout ₹${ev.userPayoutInr} • Platform ₹${ev.platformNetInr}")
        }
      }
    }
  }
}

@Composable
fun ReferralProgramScreen(
  myReferralCode: String,
  referralsState: UiState<List<ReferralRecord>>,
  onApplyReferralCode: (String) -> Unit,
) {
  val context = LocalContext.current
  var inputCode by remember { mutableStateOf("") }
  val referrals = (referralsState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NavyPrimary)) {
        Column(Modifier.padding(20.dp)) {
          Text("Single-Tier Referral Program", style = MaterialTheme.typography.titleLarge, color = PureWhite)
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Share your code. Rewards (₹25) unlock ONLY when your invited friend verifies their account and completes a genuine approved task. Strictly no multi-level payouts.",
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite.copy(alpha = 0.85f),
          )
          Spacer(Modifier.height(12.dp))
          Surface(color = EmeraldAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)) {
            Text(
              text = "Your Code: $myReferralCode",
              style = MaterialTheme.typography.titleMedium,
              color = EmeraldAccent,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
          }
          Spacer(Modifier.height(12.dp))
          Button(
            onClick = {
              val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                  Intent.EXTRA_TEXT,
                  "Join EarnMore India for genuine tasks & freelancing! Use my referral code: $myReferralCode",
                )
              }
              context.startActivity(Intent.createChooser(share, "Share Referral Code"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
          ) { Text("Invite Friend") }
        }
      }
    }

    item {
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Have a Friend's Referral Code?", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = inputCode,
            onValueChange = { inputCode = it.uppercase() },
            label = { Text("Enter Referral Code") },
            modifier = Modifier.fillMaxWidth(),
          )
          Button(
            onClick = { onApplyReferralCode(inputCode) },
            enabled = inputCode.trim().length >= 4,
          ) { Text("Link Referral Code") }
        }
      }
    }

    item { Text("Referral Activity (${referrals.size})", style = MaterialTheme.typography.titleLarge) }
    items(referrals, key = { it.id }) { ref ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
          Text("Code: ${ref.referralCode} • Status: ${ref.status}", style = MaterialTheme.typography.titleMedium)
          Text("Reward: ₹${ref.rewardInr}")
        }
      }
    }
  }
}

@Composable
fun SupportAndDisputesScreen(
  ticketsState: UiState<List<SupportTicket>>,
  disputesState: UiState<List<DisputeRefund>>,
  onSubmitTicket: (String, String, String) -> Unit,
  onSubmitDispute: (String, String, String, Double) -> Unit,
) {
  var showTicketDialog by remember { mutableStateOf(false) }
  var showDisputeDialog by remember { mutableStateOf(false) }
  val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()
  val disputes = (disputesState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Text("Support & Dispute Resolution", style = MaterialTheme.typography.headlineSmall)
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { showTicketDialog = true }) { Text("New Support Ticket") }
        OutlinedButton(onClick = { showDisputeDialog = true }) { Text("Raise Dispute / Refund") }
      }
    }

    item { Text("My Support Tickets (${tickets.size})", style = MaterialTheme.typography.titleLarge) }
    items(tickets, key = { it.id }) { t ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
          Text("${t.subject} • ${t.status}", style = MaterialTheme.typography.titleMedium)
          Text(t.message, style = MaterialTheme.typography.bodyMedium)
          if (t.adminReply.isNotBlank()) {
            Text("Admin Reply: ${t.adminReply}", color = EmeraldAccent, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }

    item { Text("My Disputes & Refunds (${disputes.size})", style = MaterialTheme.typography.titleLarge) }
    items(disputes, key = { it.id }) { d ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
          Text("Order #${d.relatedOrderId} • ${d.status} • ₹${d.requestedRefundInr}", style = MaterialTheme.typography.titleMedium)
          Text(d.reason, style = MaterialTheme.typography.bodyMedium)
          if (d.adminResolutionNote.isNotBlank()) {
            Text("Resolution: ${d.adminResolutionNote}", color = EmeraldAccent)
          }
        }
      }
    }
  }

  if (showTicketDialog) {
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var msg by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showTicketDialog = false },
      title = { Text("Create Support Ticket") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("GENERAL", "PAYOUT", "ACCOUNT").forEach { c ->
              FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
            }
          }
          OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") })
          OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("Describe your issue") })
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSubmitTicket(subject, category, msg)
            showTicketDialog = false
          },
          enabled = subject.trim().length >= 3 && msg.trim().length >= 5,
        ) { Text("Submit") }
      },
      dismissButton = { TextButton(onClick = { showTicketDialog = false }) { Text("Cancel") } },
    )
  }

  if (showDisputeDialog) {
    var orderId by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("FREELANCE") }
    var reason by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("100") }
    AlertDialog(
      onDismissRequest = { showDisputeDialog = false },
      title = { Text("Raise Dispute / Refund") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = orderId, onValueChange = { orderId = it }, label = { Text("Order / Booking ID") })
          OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
          OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Refund Amount (₹)") })
          OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") })
        }
      },
      confirmButton = {
        val amt = amountStr.toDoubleOrNull() ?: 0.0
        Button(
          onClick = {
            onSubmitDispute(orderId, category, reason, amt)
            showDisputeDialog = false
          },
          enabled = orderId.trim().isNotEmpty() && reason.trim().length >= 5 && amt > 0.0,
        ) { Text("Submit Dispute") }
      },
      dismissButton = { TextButton(onClick = { showDisputeDialog = false }) { Text("Cancel") } },
    )
  }
}
