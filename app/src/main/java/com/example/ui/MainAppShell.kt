package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.DailyCheckInClaim
import com.example.data.model.LedgerTransaction
import com.example.data.model.UserProfile
import com.example.data.model.Wallet
import java.text.SimpleDateFormat
import java.util.Date
import com.example.data.repository.AdminRepository
import com.example.data.repository.GrowthAndSupportRepository
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.auth.signOutUser
import com.example.ui.home.HomeDashboardViewModel
import com.example.ui.home.UiState
import com.example.ui.marketplace.EarnHubScreen
import com.example.ui.marketplace.EarnVertical
import com.example.ui.marketplace.EarnViewModel
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import com.example.ui.wallet.WalletScreen
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

private enum class MainTab(val route: String) {
  HOME("home"),
  EARN("earn"),
  WALLET("wallet"),
  PROFILE("profile"),
  ADMIN("admin"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
  currentUserId: String,
  language: String,
  onLanguageChange: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val databaseId = remember(context) { context.getString(R.string.firestore_database_id) }
  val firestore = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }

  val homeViewModel: HomeDashboardViewModel =
    viewModel(
      key = "home_$currentUserId",
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
            HomeDashboardViewModel(
              userRepository = UserRepository(db),
              walletRepository = WalletRepository(db),
              taskRepository = TaskRepository(db),
              currentUserId = currentUserId,
              preferredLanguage = language,
            )
          }
        },
    )

  val earnViewModel: EarnViewModel =
    viewModel(
      key = "earn_$currentUserId",
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
            EarnViewModel(
              taskRepository = TaskRepository(db),
              marketplaceRepository = MarketplaceRepository(db),
              growthRepository = GrowthAndSupportRepository(db),
            )
          }
        },
    )

  val isAdmin by
    produceState(initialValue = false, key1 = currentUserId) {
      value = AdminRepository(firestore).checkIsAdmin()
    }

  val profileState by homeViewModel.userProfile.collectAsStateWithLifecycle()
  val walletState by homeViewModel.wallet.collectAsStateWithLifecycle()
  val ledgerState by homeViewModel.ledgerTransactions.collectAsStateWithLifecycle()
  val withdrawalsState by homeViewModel.withdrawalRequests.collectAsStateWithLifecycle()
  val checkInsState by homeViewModel.dailyCheckIns.collectAsStateWithLifecycle()
  val homeFeedback by homeViewModel.actionMessage.collectAsStateWithLifecycle()
  val earnFeedback by earnViewModel.feedback.collectAsStateWithLifecycle()

  var currentTab by remember { mutableStateOf(MainTab.HOME) }
  var activeEarnVertical by remember { mutableStateOf<EarnVertical?>(null) }
  var showLegalDialog by remember { mutableStateOf(false) }

  if (currentTab != MainTab.HOME && activeEarnVertical == null) {
    BackHandler { currentTab = MainTab.HOME }
  }

  val visibleTabs =
    remember(isAdmin) {
      if (isAdmin) MainTab.entries else MainTab.entries.filter { it != MainTab.ADMIN }
    }

  LaunchedEffect(visibleTabs) {
    if (currentTab !in visibleTabs) currentTab = MainTab.HOME
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(homeFeedback) {
    val msg = homeFeedback
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      homeViewModel.clearActionMessage()
    }
  }
  LaunchedEffect(earnFeedback) {
    val msg = earnFeedback
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      earnViewModel.clearFeedback()
    }
  }

  val isHindi = language == "hi"
  val profile = (profileState as? UiState.Success)?.data
  val userName = profile?.displayName ?: "EarnMore Member"

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpanded = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (isExpanded) {
        NavigationRail(
          containerColor = NavyPrimary,
          contentColor = PureWhite,
        ) {
          Spacer(Modifier.height(16.dp))
          visibleTabs.forEach { tab ->
            NavigationRailItem(
              selected = currentTab == tab,
              onClick = {
                if (tab != MainTab.EARN) activeEarnVertical = null
                currentTab = tab
              },
              icon = { TabIcon(tab) },
              label = { Text(tabLabel(tab, isHindi)) },
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
          TopAppBar(
            title = {
              Column {
                Text("EarnMore India", style = MaterialTheme.typography.titleLarge)
                Text(
                  text = "TEST MODE • Cloud Firestore Synced",
                  style = MaterialTheme.typography.labelSmall,
                  color = EmeraldAccent,
                )
              }
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = NavyPrimary,
                titleContentColor = PureWhite,
              ),
          )
        },
        bottomBar = {
          if (!isExpanded) {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
              visibleTabs.forEach { tab ->
                NavigationBarItem(
                  selected = currentTab == tab,
                  onClick = {
                    if (tab != MainTab.EARN) activeEarnVertical = null
                    currentTab = tab
                  },
                  icon = { TabIcon(tab) },
                  label = { Text(tabLabel(tab, isHindi)) },
                  modifier = Modifier.testTag("nav_${tab.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Box(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .background(MaterialTheme.colorScheme.background),
          contentAlignment = Alignment.TopCenter,
        ) {
          Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            when (currentTab) {
              MainTab.HOME ->
                HomeDashboardSection(
                  walletState = walletState,
                  ledgerState = ledgerState,
                  checkInsState = checkInsState,
                  isHindi = isHindi,
                  onClaimDailyPromo = homeViewModel::claimDailyPromoBonus,
                  onJumpToVertical = { vertical ->
                    activeEarnVertical = vertical
                    currentTab = MainTab.EARN
                  },
                  onNavigateToWallet = { currentTab = MainTab.WALLET },
                )
              MainTab.EARN ->
                EarnHubScreen(
                  currentUserId = currentUserId,
                  currentUserName = userName,
                  myReferralCode = profile?.referralCode ?: "...",
                  isHindi = isHindi,
                  activeVertical = activeEarnVertical,
                  onSelectVertical = { activeEarnVertical = it },
                  earnViewModel = earnViewModel,
                )
              MainTab.WALLET ->
                WalletScreen(
                  walletState = walletState,
                  ledgerState = ledgerState,
                  withdrawalsState = withdrawalsState,
                  isHindi = isHindi,
                  onRequestWithdrawal = homeViewModel::requestTestModeWithdrawal,
                )
              MainTab.PROFILE ->
                ProfileSection(
                  profileState = profileState,
                  language = language,
                  onLanguageChange = { newLang ->
                    onLanguageChange(newLang)
                    homeViewModel.updateProfileLanguage(
                      language = newLang,
                      displayName = profile?.displayName ?: "EarnMore Member",
                      phone = profile?.phone ?: "",
                    )
                  },
                  onOpenSupport = {
                    activeEarnVertical = EarnVertical.SUPPORT
                    currentTab = MainTab.EARN
                  },
                  onOpenLegal = { showLegalDialog = true },
                  onSignOut = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                  },
                )
              MainTab.ADMIN -> {
                if (isAdmin) {
                  val adminViewModel: AdminViewModel =
                    viewModel(
                      key = "admin_$currentUserId",
                      factory =
                        viewModelFactory {
                          initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                            AdminViewModel(
                              adminRepository = AdminRepository(db),
                              marketplaceRepository = MarketplaceRepository(db),
                            )
                          }
                        },
                    )
                  val adminFeedback by adminViewModel.feedback.collectAsStateWithLifecycle()
                  LaunchedEffect(adminFeedback) {
                    val msg = adminFeedback
                    if (msg != null) {
                      snackbarHostState.showSnackbar(msg)
                      adminViewModel.clearFeedback()
                    }
                  }
                  AdminDashboardScreen(adminViewModel = adminViewModel)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showLegalDialog) {
    AlertDialog(
      onDismissRequest = { showLegalDialog = false },
      title = { Text("Legal, Privacy & Compliance") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("1. Genuine Work Only (No Fake Money)", style = MaterialTheme.typography.titleMedium)
          Text("All balances originate from verified client orders, approved tasks, affiliate conversions, or eligible single-tier referrals. No earnings are guaranteed.")
          Text("2. DPDP Act 2023 Privacy Policy", style = MaterialTheme.typography.titleMedium)
          Text("EarnMore India stores only your display name, email, KYC verification status, and masked payout identifiers. UPI PINs, bank passwords, and card CVVs are never collected or stored.")
          Text("3. Transparent Commission & Tax Notes", style = MaterialTheme.typography.titleMedium)
          Text("Every order displays Gross Value, Platform Fee, Gateway Fee, and Provider Net before confirmation. Users and sellers are responsible for applicable Indian Income Tax / GST reporting.")
          Text("4. Refund & Cancellation Policy", style = MaterialTheme.typography.titleMedium)
          Text("Disputes raised prior to completion are reviewed by admins against submitted delivery evidence and settled back per marketplace audit rules.")
        }
      },
      confirmButton = {
        TextButton(onClick = { showLegalDialog = false }) { Text("Close") }
      },
    )
  }
}

@Composable
private fun TabIcon(tab: MainTab) {
  val icon =
    when (tab) {
      MainTab.HOME -> Icons.Filled.Home
      MainTab.EARN -> Icons.Filled.WorkOutline
      MainTab.WALLET -> Icons.Filled.AccountBalanceWallet
      MainTab.PROFILE -> Icons.Filled.Person
      MainTab.ADMIN -> Icons.Filled.AdminPanelSettings
    }
  Icon(icon, contentDescription = tab.route)
}

private fun tabLabel(tab: MainTab, isHindi: Boolean): String =
  when (tab) {
    MainTab.HOME -> if (isHindi) "होम" else "Home"
    MainTab.EARN -> if (isHindi) "कमाई" else "Earn"
    MainTab.WALLET -> if (isHindi) "वॉलेट" else "Wallet"
    MainTab.PROFILE -> if (isHindi) "प्रोफ़ाइल" else "Profile"
    MainTab.ADMIN -> if (isHindi) "एडमिन" else "Admin"
  }

@Composable
private fun HomeDashboardSection(
  walletState: UiState<Wallet?>,
  ledgerState: UiState<List<LedgerTransaction>>,
  checkInsState: UiState<List<DailyCheckInClaim>>,
  isHindi: Boolean,
  onClaimDailyPromo: () -> Unit,
  onJumpToVertical: (EarnVertical) -> Unit,
  onNavigateToWallet: () -> Unit,
) {
  val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
  val todayClaim = (checkInsState as? UiState.Success)?.data?.firstOrNull { it.dateKey == todayKey }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      val wallet = (walletState as? UiState.Success)?.data
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
      ) {
        Column(
          modifier =
            Modifier.background(Brush.linearGradient(listOf(NavyDeep, NavyPrimary))).padding(20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (isHindi) "उपलब्ध शेष" else "Available Balance",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite.copy(alpha = 0.8f),
            )
            Surface(color = EmeraldAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(999.dp)) {
              Text(
                text = "VERIFIED LEDGER",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldAccent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = formatInr(wallet?.availableBalanceInr ?: 0.0),
            style = MaterialTheme.typography.displaySmall,
            color = PureWhite,
          )
          Spacer(Modifier.height(14.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat(if (isHindi) "कुल सत्यापित" else "Total Earned", formatInr(wallet?.totalEarnedInr ?: 0.0))
            MiniStat(if (isHindi) "लंबित कमाई" else "Pending", formatInr(wallet?.pendingBalanceInr ?: 0.0))
            MiniStat(if (isHindi) "पूर्ण कार्य" else "Tasks Done", "${wallet?.completedTasksCount ?: 0L}")
          }
          Spacer(Modifier.height(16.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = { onJumpToVertical(EarnVertical.TASKS) },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
              modifier = Modifier.weight(1f).testTag("home_browse_tasks_button"),
            ) {
              Text(if (isHindi) "दैनिक कार्य" else "Daily Tasks")
            }
            OutlinedButton(
              onClick = onNavigateToWallet,
              modifier = Modifier.weight(1f).testTag("home_open_wallet_button"),
            ) {
              Text(if (isHindi) "वॉलेट विवरण" else "Wallet Ledger", color = PureWhite)
            }
          }
        }
      }
    }

    item {
      Text(
        text = if (isHindi) "त्वरित बाज़ार शॉर्टकट" else "Quick Marketplace Shortcuts",
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { onJumpToVertical(EarnVertical.GAME_TASKS) },
          modifier = Modifier.weight(1f).testTag("home_game_tasks_button"),
        ) { Text(if (isHindi) "आसान गेम" else "Game Tasks") }
        OutlinedButton(
          onClick = { onJumpToVertical(EarnVertical.FREELANCING) },
          modifier = Modifier.weight(1f),
        ) { Text(if (isHindi) "फ्रीलांस" else "Freelance") }
        OutlinedButton(
          onClick = { onJumpToVertical(EarnVertical.REFERRALS) },
          modifier = Modifier.weight(1f),
        ) { Text(if (isHindi) "रेफरल" else "Referrals") }
      }
    }

    item {
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = if (isHindi) "दैनिक चेक-इन प्रमो बोनस (एडमिन स्वीकृति)" else "Daily Check-In Promo Request (₹100 Max)",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text =
              if (isHindi) {
                "बिना काम के तुरंत मुफ़्त ₹100 ऑटो-क्रेडिट नहीं होता (शून्य-बजट और एंटी-फ्रॉड नियम)। आप रोज़ 1 प्रमो अनुरोध भेज सकते हैं—एडमिन द्वारा बजट फंड होने पर वॉलेट में जुड़ता है और UPI (TEST MODE) से ₹100+ निकलता है।"
              } else {
                "Anti-fraud & ₹0-budget policy: login bonuses are never auto-minted out of thin air. Submit 1 promo request/day; once Admin funds & verifies it, withdraw via UPI (TEST MODE)."
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (todayClaim != null) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = "Today ($todayKey): ${todayClaim.status} ${if (todayClaim.reviewNote.isNotBlank()) "• ${todayClaim.reviewNote}" else ""}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          } else {
            Button(
              onClick = onClaimDailyPromo,
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
              modifier = Modifier.fillMaxWidth().testTag("daily_promo_claim_button"),
            ) {
              Text(if (isHindi) "आज का ₹100 प्रमो अनुरोध भेजें" else "Request Today's ₹100 Promo")
            }
          }
        }
      }
    }

    item {
      Text(
        text = if (isHindi) "हाल के लेन-देन" else "Recent Ledger Transactions",
        style = MaterialTheme.typography.titleLarge,
      )
    }

    when (ledgerState) {
      is UiState.Loading ->
        item {
          Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
      is UiState.Error ->
        item {
          Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
            Text(ledgerState.message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
          }
        }
      is UiState.Success -> {
        if (ledgerState.data.isEmpty()) {
          item {
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(8.dp))
                Text(if (isHindi) "अभी कोई लेन-देन नहीं" else "No verified transactions yet", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = if (isHindi) "पूर्ण कार्य या ऑर्डर यहाँ दिखाई देंगे।" else "Verified task rewards and completed order payouts appear here.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(ledgerState.data.take(5), key = { it.id }) { tx ->
            Card(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(tx.description, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                  Text(formatInr(tx.netAmountInr), style = MaterialTheme.typography.labelMedium, color = EmeraldAccent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                  text = "Gross ${formatInr(tx.grossAmountInr)} • Platform Fee ${formatInr(tx.platformCommissionInr)} • ${tx.status}",
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

@Composable
private fun ProfileSection(
  profileState: UiState<UserProfile?>,
  language: String,
  onLanguageChange: (String) -> Unit,
  onOpenSupport: () -> Unit,
  onOpenLegal: () -> Unit,
  onSignOut: () -> Unit,
) {
  val profile = (profileState as? UiState.Success)?.data
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Verified, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
              Text(profile?.displayName ?: "EarnMore Member", style = MaterialTheme.typography.titleLarge)
              Text(profile?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Spacer(Modifier.height(12.dp))
          Text("Referral Code: ${profile?.referralCode ?: "Generating..."}", style = MaterialTheme.typography.labelMedium)
          Text(
            text = "Role: ${profile?.role ?: "USER"} • KYC: ${profile?.kycStatus ?: "UNVERIFIED"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(16.dp))
          Text("App Language / भाषा", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = language != "hi", onClick = { onLanguageChange("en") }, label = { Text("English") })
            FilterChip(selected = language == "hi", onClick = { onLanguageChange("hi") }, label = { Text("हिन्दी") })
          }
          Spacer(Modifier.height(14.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenSupport, modifier = Modifier.weight(1f).testTag("profile_support_button")) {
              Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Support")
            }
            OutlinedButton(onClick = onOpenLegal, modifier = Modifier.weight(1f).testTag("profile_legal_button")) {
              Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Legal & DPDP")
            }
          }
          Spacer(Modifier.height(12.dp))
          OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().testTag("sign_out_button")) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (language == "hi") "साइन आउट" else "Sign Out")
          }
        }
      }
    }
  }
}

@Composable
private fun MiniStat(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelSmall, color = PureWhite.copy(alpha = 0.72f))
    Text(value, style = MaterialTheme.typography.titleMedium, color = PureWhite)
  }
}

private fun formatInr(amount: Double): String = String.format(Locale.US, "₹%.2f", amount)
