package com.example.ui.marketplace

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FreelanceGig
import com.example.data.model.FreelanceOrder
import com.example.data.model.MicroTask
import com.example.data.model.TaskSubmission
import com.example.ui.home.UiState
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyPrimary
import java.util.Locale

@Composable
fun DailyTasksScreen(
  tasksState: UiState<List<MicroTask>>,
  submissionsState: UiState<List<TaskSubmission>>,
  isHindi: Boolean,
  onSubmitProof: (String, String, String) -> Unit,
) {
  val categories =
    listOf("ALL", "GAME_TASK", "DATA_ENTRY", "CONTENT_WRITING", "IMAGE_CATEGORIZATION", "WEBSITE_TESTING", "RESEARCH")
  var selectedCategory by remember { mutableStateOf("ALL") }
  var taskToSubmit by remember { mutableStateOf<MicroTask?>(null) }
  val submissions = (submissionsState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = if (isHindi) "दैनिक माइक्रो-कार्य" else "Daily Task Marketplace",
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        categories.forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat.replace("_", " ")) },
          )
        }
      }
    }

    when (tasksState) {
      is UiState.Loading ->
        item {
          Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
      is UiState.Error ->
        item { Text(tasksState.message, color = MaterialTheme.colorScheme.error) }
      is UiState.Success -> {
        val filtered =
          if (selectedCategory == "ALL") tasksState.data
          else tasksState.data.filter { it.category == selectedCategory }
        if (filtered.isEmpty()) {
          item {
            Card(modifier = Modifier.fillMaxWidth()) {
              Text(
                text =
                  if (isHindi) "इस श्रेणी में अभी कोई कार्य नहीं है।"
                  else "No open tasks in this category right now.",
                modifier = Modifier.padding(20.dp),
              )
            }
          }
        } else {
          items(filtered, key = { it.id }) { task ->
            val existingSub = submissions.firstOrNull { it.taskId == task.id }
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Text(
                      text = task.category.replace("_", " "),
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                  Text(
                    text = "Earn ₹${String.format(Locale.US, "%.2f", task.workerNetInr)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldAccent,
                  )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                  text = if (isHindi && task.titleHi.isNotBlank()) task.titleHi else task.title,
                  style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                  text = task.description,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                  text = "Gross ₹${task.rewardGrossInr} • Platform Fee ${task.commissionPercent}% • Slots ${task.slotsFilled}/${task.slotsTotal}",
                  style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(10.dp))
                if (existingSub != null) {
                  Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Text(
                      text = "Status: ${existingSub.status} ${if (existingSub.reviewNote.isNotBlank()) "• ${existingSub.reviewNote}" else ""}",
                      style = MaterialTheme.typography.labelMedium,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                  }
                } else {
                  Button(
                    onClick = { taskToSubmit = task },
                    modifier = Modifier.fillMaxWidth().testTag("task_submit_${task.id}"),
                  ) {
                    Text(if (isHindi) "कार्य प्रमाण जमा करें" else "Submit Work Proof")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  taskToSubmit?.let { task ->
    var proofText by remember { mutableStateOf("") }
    var proofUrl by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { taskToSubmit = null },
      title = { Text("Submit Proof: ${task.title}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = proofText,
            onValueChange = { proofText = it },
            label = { Text("Detailed Work Proof") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = proofUrl,
            onValueChange = { proofUrl = it },
            label = { Text("Optional Screenshot / Sheet URL") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSubmitProof(task.id, proofText, proofUrl)
            taskToSubmit = null
          },
          enabled = proofText.trim().length >= 3,
        ) {
          Text("Submit")
        }
      },
      dismissButton = { TextButton(onClick = { taskToSubmit = null }) { Text("Cancel") } },
    )
  }
}

@Composable
fun FreelancingScreen(
  currentUserId: String,
  currentUserName: String,
  gigsState: UiState<List<FreelanceGig>>,
  ordersState: UiState<List<FreelanceOrder>>,
  isHindi: Boolean,
  onCreateGig: (String, String, String, String, Double, Long) -> Unit,
  onOrderGig: (FreelanceGig, String) -> Unit,
  onUpdateOrder: (String, String, String?) -> Unit,
) {
  val gigCategories =
    listOf("GRAPHIC_DESIGN", "VIDEO_EDITING", "LOGO_DESIGN", "TYPING", "TRANSLATION", "WEB_DEVELOPMENT", "WRITING")
  var showCreateDialog by remember { mutableStateOf(false) }
  var gigToOrder by remember { mutableStateOf<FreelanceGig?>(null) }
  var showOrdersTab by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isHindi) "फ्रीलांसिंग बाज़ार" else "Freelancing Marketplace",
          style = MaterialTheme.typography.headlineSmall,
        )
        Button(
          onClick = { showCreateDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
          modifier = Modifier.testTag("create_gig_button"),
        ) {
          Text(if (isHindi) "+ सेवा जोड़ें" else "+ Post Gig")
        }
      }
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = !showOrdersTab,
          onClick = { showOrdersTab = false },
          label = { Text("Browse Gigs") },
        )
        FilterChip(
          selected = showOrdersTab,
          onClick = { showOrdersTab = true },
          label = { Text("My Orders") },
        )
      }
    }

    if (!showOrdersTab) {
      when (gigsState) {
        is UiState.Loading ->
          item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        is UiState.Error ->
          item { Text(gigsState.message, color = MaterialTheme.colorScheme.error) }
        is UiState.Success -> {
          if (gigsState.data.isEmpty()) {
            item {
              Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "No freelance gigs posted yet. Tap '+ Post Gig' to offer design, editing, typing, or dev services!",
                  modifier = Modifier.padding(20.dp),
                )
              }
            }
          } else {
            items(gigsState.data, key = { it.id }) { gig ->
              Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text(gig.category.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
                  Spacer(Modifier.height(4.dp))
                  Text(gig.title, style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = "By ${gig.sellerName} • ${gig.deliveryDays} days delivery",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(Modifier.height(6.dp))
                  Text(gig.description, style = MaterialTheme.typography.bodyMedium)
                  Spacer(Modifier.height(10.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "₹${String.format(Locale.US, "%.2f", gig.priceInr)}",
                      style = MaterialTheme.typography.titleMedium,
                      color = EmeraldAccent,
                    )
                    if (gig.sellerId != currentUserId) {
                      Button(onClick = { gigToOrder = gig }) { Text("Hire (TEST MODE)") }
                    } else {
                      Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                      ) {
                        Text(
                          text = "Your Listing",
                          style = MaterialTheme.typography.labelSmall,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
      if (orders.isEmpty()) {
        item {
          Card(modifier = Modifier.fillMaxWidth()) {
            Text("No freelance orders yet.", modifier = Modifier.padding(20.dp))
          }
        }
      } else {
        items(orders, key = { it.id }) { order ->
          val isSeller = order.sellerId == currentUserId
          Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "Order #${order.id.take(6)} • ${order.status}",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Gross ₹${order.grossAmountInr} • Fee ₹${order.platformCommissionInr} • Seller Net ₹${order.sellerNetInr}",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldAccent,
              )
              Spacer(Modifier.height(4.dp))
              Text("Brief: ${order.requirements}", style = MaterialTheme.typography.bodyMedium)
              if (order.deliveryNote.isNotBlank()) {
                Text("Delivery: ${order.deliveryNote}", style = MaterialTheme.typography.bodyMedium)
              }
              Spacer(Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isSeller && order.status == "PLACED") {
                  Button(onClick = { onUpdateOrder(order.id, "IN_PROGRESS", null) }) {
                    Text("Start Work")
                  }
                }
                if (isSeller && order.status in listOf("PLACED", "IN_PROGRESS")) {
                  Button(onClick = { onUpdateOrder(order.id, "DELIVERED", "Work delivered for review.") }) {
                    Text("Mark Delivered")
                  }
                }
                if (!isSeller && order.status == "DELIVERED") {
                  Button(onClick = { onUpdateOrder(order.id, "COMPLETED", null) }) {
                    Text("Accept & Complete")
                  }
                  OutlinedButton(onClick = { onUpdateOrder(order.id, "DISPUTED", null) }) {
                    Text("Dispute")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    var category by remember { mutableStateOf(gigCategories.first()) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("500") }
    var daysStr by remember { mutableStateOf("3") }
    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("Publish Freelance Gig") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            gigCategories.forEach { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat.replace("_", " ")) },
              )
            }
          }
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Gig Title") })
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
          OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Price (₹50+)") })
          OutlinedTextField(value = daysStr, onValueChange = { daysStr = it }, label = { Text("Delivery Days") })
        }
      },
      confirmButton = {
        val price = priceStr.toDoubleOrNull() ?: 0.0
        val days = daysStr.toLongOrNull() ?: 3L
        Button(
          onClick = {
            onCreateGig(currentUserName, category, title, desc, price, days)
            showCreateDialog = false
          },
          enabled = title.trim().length >= 3 && desc.trim().length >= 5 && price >= 50.0,
        ) {
          Text("Publish")
        }
      },
      dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } },
    )
  }

  gigToOrder?.let { gig ->
    var req by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { gigToOrder = null },
      title = { Text("Order: ${gig.title} (TEST MODE)") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Price: ₹${gig.priceInr} (Transparent 10% platform commission applies)")
          OutlinedTextField(
            value = req,
            onValueChange = { req = it },
            label = { Text("Project Requirements") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onOrderGig(gig, req)
            gigToOrder = null
          },
          enabled = req.trim().length >= 3,
        ) {
          Text("Place TEST Order")
        }
      },
      dismissButton = { TextButton(onClick = { gigToOrder = null }) { Text("Cancel") } },
    )
  }
}
