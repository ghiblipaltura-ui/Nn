package com.example.ui.marketplace

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.LocalService
import com.example.data.model.Product
import com.example.data.model.ProductOrder
import com.example.data.model.ServiceBooking
import com.example.data.model.TutorBooking
import com.example.data.model.TutorCourse
import com.example.ui.home.UiState
import com.example.ui.theme.EmeraldAccent

@Composable
fun ProductMarketplaceScreen(
  currentUserId: String,
  currentUserName: String,
  productsState: UiState<List<Product>>,
  ordersState: UiState<List<ProductOrder>>,
  onCreateProduct: (String, String, String, String, Double, Long) -> Unit,
  onOrderProduct: (Product, Long, String) -> Unit,
  onUpdateTracking: (String, String) -> Unit,
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var productToBuy by remember { mutableStateOf<Product?>(null) }
  var showOrders by remember { mutableStateOf(false) }

  val products = (productsState as? UiState.Success)?.data ?: emptyList()
  val orders = (ordersState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Product Marketplace", style = MaterialTheme.typography.headlineSmall)
        Button(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
        ) { Text("+ Sell Product") }
      }
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !showOrders, onClick = { showOrders = false }, label = { Text("Catalog (${products.size})") })
        FilterChip(selected = showOrders, onClick = { showOrders = true }, label = { Text("Orders (${orders.size})") })
      }
    }

    if (!showOrders) {
      if (products.isEmpty()) {
        item { Card(Modifier.fillMaxWidth()) { Text("No products listed yet.", Modifier.padding(20.dp)) } }
      } else {
        items(products, key = { it.id }) { p ->
          Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
              Text("${p.category} • Seller: ${p.sellerName}", style = MaterialTheme.typography.labelSmall)
              Text(p.title, style = MaterialTheme.typography.titleMedium)
              Text(p.description, style = MaterialTheme.typography.bodyMedium)
              Spacer(Modifier.height(8.dp))
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("₹${p.priceInr} • Stock: ${p.stock}", style = MaterialTheme.typography.labelMedium, color = EmeraldAccent)
                if (p.sellerId != currentUserId && p.stock > 0) {
                  Button(onClick = { productToBuy = p }) { Text("Buy (TEST MODE)") }
                }
              }
            }
          }
        }
      }
    } else {
      items(orders, key = { it.id }) { o ->
        val isSeller = o.sellerId == currentUserId
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(16.dp)) {
            Text("Order #${o.id.take(6)} • ${o.trackingStatus}", style = MaterialTheme.typography.titleMedium)
            Text("Qty: ${o.quantity} • Gross ₹${o.grossAmountInr} • Fee ₹${o.platformCommissionInr} • Net ₹${o.sellerNetInr}")
            Text("Delivery Area: ${o.shippingArea}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (isSeller && o.trackingStatus == "ORDERED") {
                Button(onClick = { onUpdateTracking(o.id, "PACKED") }) { Text("Pack") }
              }
              if (isSeller && o.trackingStatus == "PACKED") {
                Button(onClick = { onUpdateTracking(o.id, "SHIPPED") }) { Text("Ship") }
              }
              if (isSeller && o.trackingStatus == "SHIPPED") {
                Button(onClick = { onUpdateTracking(o.id, "DELIVERED") }) { Text("Mark Delivered") }
              }
              if (!isSeller && o.trackingStatus == "DELIVERED") {
                Button(onClick = { onUpdateTracking(o.id, "COMPLETED") }) { Text("Confirm Receipt") }
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    var cat by remember { mutableStateOf("Handicrafts") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("299") }
    var stockStr by remember { mutableStateOf("10") }
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("List a Product") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") })
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Product Title") })
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
          OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Price (₹10+)") })
          OutlinedTextField(value = stockStr, onValueChange = { stockStr = it }, label = { Text("Stock Qty") })
        }
      },
      confirmButton = {
        val price = priceStr.toDoubleOrNull() ?: 0.0
        val stock = stockStr.toLongOrNull() ?: 1L
        Button(
          onClick = {
            onCreateProduct(currentUserName, cat, title, desc, price, stock)
            showAddDialog = false
          },
          enabled = title.trim().length >= 3 && desc.trim().length >= 5 && price >= 10.0,
        ) { Text("Publish") }
      },
      dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
    )
  }

  productToBuy?.let { p ->
    var area by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { productToBuy = null },
      title = { Text("Buy ${p.title} (TEST MODE)") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Price: ₹${p.priceInr} (8% seller platform commission)")
          OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("City / PIN Area") })
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onOrderProduct(p, 1L, area)
            productToBuy = null
          },
          enabled = area.trim().length >= 3,
        ) { Text("Place TEST Order") }
      },
      dismissButton = { TextButton(onClick = { productToBuy = null }) { Text("Cancel") } },
    )
  }
}

@Composable
fun TeachingMarketplaceScreen(
  currentUserId: String,
  currentUserName: String,
  coursesState: UiState<List<TutorCourse>>,
  bookingsState: UiState<List<TutorBooking>>,
  onCreateCourse: (String, String, String, String, Double, String) -> Unit,
  onBookCourse: (TutorCourse, String) -> Unit,
  onUpdateBooking: (String, String) -> Unit,
) {
  var showAdd by remember { mutableStateOf(false) }
  var courseToBook by remember { mutableStateOf<TutorCourse?>(null) }
  val courses = (coursesState as? UiState.Success)?.data ?: emptyList()
  val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Teaching Marketplace", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)) {
          Text("+ Teach")
        }
      }
    }
    items(courses, key = { it.id }) { c ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
          Text("${c.subject} • ${c.mode} • Tutor: ${c.tutorName}", style = MaterialTheme.typography.labelSmall)
          Text(c.title, style = MaterialTheme.typography.titleMedium)
          Text(c.description, style = MaterialTheme.typography.bodyMedium)
          Spacer(Modifier.height(8.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("₹${c.hourlyRateInr}/session", style = MaterialTheme.typography.labelMedium, color = EmeraldAccent)
            if (c.tutorId != currentUserId) {
              Button(onClick = { courseToBook = c }) { Text("Book Slot") }
            }
          }
        }
      }
    }
    if (bookings.isNotEmpty()) {
      item { Text("My Class Bookings", style = MaterialTheme.typography.titleLarge) }
      items(bookings, key = { it.id }) { b ->
        val isTutor = b.tutorId == currentUserId
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(16.dp)) {
            Text("Slot: ${b.scheduledSlot} • ${b.status}", style = MaterialTheme.typography.titleMedium)
            Text("Gross ₹${b.grossAmountInr} • Tutor Net ₹${b.tutorNetInr}")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (isTutor && b.status == "BOOKED") {
                Button(onClick = { onUpdateBooking(b.id, "CONFIRMED") }) { Text("Confirm") }
              }
              if (!isTutor && b.status == "CONFIRMED") {
                Button(onClick = { onUpdateBooking(b.id, "COMPLETED") }) { Text("Complete Class") }
              }
            }
          }
        }
      }
    }
  }

  if (showAdd) {
    var subject by remember { mutableStateOf("Spoken English") }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("250") }
    var mode by remember { mutableStateOf("LIVE_1ON1") }
    AlertDialog(
      onDismissRequest = { showAdd = false },
      title = { Text("Create Class or Skill Course") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") })
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") })
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Syllabus Summary") })
          OutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = { Text("Rate (₹50+)") })
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("LIVE_1ON1", "COHORT_CLASS", "SKILL_COURSE").forEach { m ->
              FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
            }
          }
        }
      },
      confirmButton = {
        val rate = rateStr.toDoubleOrNull() ?: 0.0
        Button(
          onClick = {
            onCreateCourse(currentUserName, subject, title, desc, rate, mode)
            showAdd = false
          },
          enabled = title.trim().length >= 3 && desc.trim().length >= 5 && rate >= 50.0,
        ) { Text("Publish") }
      },
      dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
    )
  }

  courseToBook?.let { c ->
    var slot by remember { mutableStateOf("Sat 6:00 PM IST") }
    AlertDialog(
      onDismissRequest = { courseToBook = null },
      title = { Text("Book: ${c.title} (TEST MODE)") },
      text = {
        OutlinedTextField(value = slot, onValueChange = { slot = it }, label = { Text("Preferred Date & Time") })
      },
      confirmButton = {
        Button(onClick = {
          onBookCourse(c, slot)
          courseToBook = null
        }, enabled = slot.trim().length >= 3) { Text("Confirm Booking") }
      },
      dismissButton = { TextButton(onClick = { courseToBook = null }) { Text("Cancel") } },
    )
  }
}

@Composable
fun LocalServicesScreen(
  currentUserId: String,
  currentUserName: String,
  servicesState: UiState<List<LocalService>>,
  bookingsState: UiState<List<ServiceBooking>>,
  onCreateService: (String, String, String, String, Double) -> Unit,
  onBookService: (LocalService, String, String) -> Unit,
  onUpdateBooking: (String, String, Boolean?) -> Unit,
) {
  var showAdd by remember { mutableStateOf(false) }
  var serviceToBook by remember { mutableStateOf<LocalService?>(null) }
  val services = (servicesState as? UiState.Success)?.data ?: emptyList()
  val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Local Services", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)) {
          Text("+ Offer Service")
        }
      }
    }
    items(services, key = { it.id }) { s ->
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
          Text("${s.serviceType} • ${s.city} • ${s.providerName}", style = MaterialTheme.typography.labelSmall)
          Text(s.description, style = MaterialTheme.typography.bodyMedium)
          Spacer(Modifier.height(8.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("₹${s.rateInr}", style = MaterialTheme.typography.labelMedium, color = EmeraldAccent)
            if (s.providerId != currentUserId) {
              Button(onClick = { serviceToBook = s }) { Text("Request Service") }
            }
          }
        }
      }
    }
    if (bookings.isNotEmpty()) {
      item { Text("Service Requests & Confirmations", style = MaterialTheme.typography.titleLarge) }
      items(bookings, key = { it.id }) { b ->
        val isProvider = b.providerId == currentUserId
        Card(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(16.dp)) {
            Text("Area: ${b.cityArea} • ${b.status}", style = MaterialTheme.typography.titleMedium)
            Text("Gross ₹${b.grossAmountInr} • Provider Net ₹${b.providerNetInr} • Customer Confirmed: ${b.customerConfirmed}")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (isProvider && b.status == "REQUESTED") {
                Button(onClick = { onUpdateBooking(b.id, "ACCEPTED", null) }) { Text("Accept") }
              }
              if (isProvider && b.status == "ACCEPTED") {
                Button(onClick = { onUpdateBooking(b.id, "WORK_DONE", null) }) { Text("Mark Work Done") }
              }
              if (!isProvider && b.status == "WORK_DONE") {
                Button(onClick = { onUpdateBooking(b.id, "COMPLETED", true) }) { Text("Confirm Completion") }
              }
            }
          }
        }
      }
    }
  }

  if (showAdd) {
    var type by remember { mutableStateOf("Electrician / Repair") }
    var city by remember { mutableStateOf("Pune") }
    var desc by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("350") }
    AlertDialog(
      onDismissRequest = { showAdd = false },
      title = { Text("Register Local Service") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Service Type") })
          OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") })
          OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Service Details") })
          OutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = { Text("Visit Rate (₹50+)") })
        }
      },
      confirmButton = {
        val rate = rateStr.toDoubleOrNull() ?: 0.0
        Button(
          onClick = {
            onCreateService(currentUserName, type, city, desc, rate)
            showAdd = false
          },
          enabled = desc.trim().length >= 5 && rate >= 50.0,
        ) { Text("Publish") }
      },
      dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
    )
  }

  serviceToBook?.let { s ->
    var area by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { serviceToBook = null },
      title = { Text("Request ${s.serviceType} (TEST MODE)") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Locality / Area") })
          OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Work Notes") })
        }
      },
      confirmButton = {
        Button(onClick = {
          onBookService(s, area, notes)
          serviceToBook = null
        }, enabled = area.trim().length >= 2) { Text("Send Request") }
      },
      dismissButton = { TextButton(onClick = { serviceToBook = null }) { Text("Cancel") } },
    )
  }
}
