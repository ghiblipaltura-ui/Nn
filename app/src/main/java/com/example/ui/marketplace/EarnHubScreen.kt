package com.example.ui.marketplace

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.growth.AffiliateMarketplaceScreen
import com.example.ui.growth.ReferralProgramScreen
import com.example.ui.growth.SupportAndDisputesScreen
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyPrimary

enum class EarnVertical(
  val tag: String,
  val titleEn: String,
  val titleHi: String,
  val subtitleEn: String,
  val subtitleHi: String,
  val commissionBadge: String,
  val icon: ImageVector,
) {
  TASKS(
    tag = "earn_tasks",
    titleEn = "Daily Microtasks",
    titleHi = "दैनिक माइक्रो-कार्य",
    subtitleEn = "Data entry, categorization, testing & verified research tasks",
    subtitleHi = "डेटा एंट्री, वर्गीकरण, वेबसाइट टेस्टिंग और रिसर्च कार्य",
    commissionBadge = "85% Worker Net",
    icon = Icons.Filled.TaskAlt,
  ),
  GAME_TASKS(
    tag = "earn_games",
    titleEn = "Easy Game Tasks",
    titleHi = "आसान गेम कार्य",
    subtitleEn = "Math sprint, color match, memory pairs, word quiz & reflex QA games",
    subtitleHi = "गणित, रंग मिलान, मेमोरी कार्ड, नंबर क्रम, शब्द और टैप गेम",
    commissionBadge = "6 Playable Games • 85% Net",
    icon = Icons.Filled.SportsEsports,
  ),
  FREELANCING(
    tag = "earn_freelance",
    titleEn = "Freelancing Marketplace",
    titleHi = "फ्रीलांसिंग बाज़ार",
    subtitleEn = "Graphic design, video editing, typing, translation & web gigs",
    subtitleHi = "ग्राफिक डिज़ाइन, वीडियो एडिटिंग, टाइपिंग और वेब सेवाएँ",
    commissionBadge = "10% Platform Fee",
    icon = Icons.Filled.DesignServices,
  ),
  PRODUCTS(
    tag = "earn_products",
    titleEn = "Product Marketplace",
    titleHi = "उत्पाद बाज़ार",
    subtitleEn = "List products, manage orders, tracking & transparent seller fees",
    subtitleHi = "उत्पाद बेचें, ऑर्डर ट्रैक करें और पारदर्शी कमीशन देखें",
    commissionBadge = "8% Seller Fee",
    icon = Icons.Filled.ShoppingBag,
  ),
  AFFILIATE(
    tag = "earn_affiliate",
    titleEn = "Affiliate Marketplace",
    titleHi = "एफिलिएट बाज़ार",
    subtitleEn = "Generate verified tracking links & earn on confirmed sales",
    subtitleHi = "ट्रैकिंग लिंक साझा करें और सत्यापित बिक्री पर कमाएं",
    commissionBadge = "70% Promoter Share",
    icon = Icons.Filled.Share,
  ),
  REFERRALS(
    tag = "earn_referrals",
    titleEn = "Single-Tier Referrals",
    titleHi = "रेफरल प्रोग्राम",
    subtitleEn = "₹25 bonus only after eligible verified activity (No MLM)",
    subtitleHi = "केवल सत्यापित कार्य के बाद ₹25 बोनस (कोई MLM नहीं)",
    commissionBadge = "Anti-MLM Compliant",
    icon = Icons.Filled.CardGiftcard,
  ),
  TEACHING(
    tag = "earn_teaching",
    titleEn = "Teaching Marketplace",
    titleHi = "शिक्षण बाज़ार",
    subtitleEn = "Online tutoring, live sessions, skill courses & slot booking",
    subtitleHi = "ऑनलाइन ट्यूशन, स्किल कोर्स और क्लास बुकिंग",
    commissionBadge = "12% Platform Fee",
    icon = Icons.Filled.School,
  ),
  LOCAL_SERVICES(
    tag = "earn_services",
    titleEn = "Local Services",
    titleHi = "स्थानीय सेवाएँ",
    subtitleEn = "City repair, home visits, booking & customer completion sign-off",
    subtitleHi = "स्थानीय सेवा प्रदाता, बुकिंग और कार्य पुष्टि",
    commissionBadge = "10% Platform Fee",
    icon = Icons.Filled.Build,
  ),
  SUPPORT(
    tag = "earn_support",
    titleEn = "Support & Disputes",
    titleHi = "सहायता और विवाद",
    subtitleEn = "Create support tickets or raise order refund & dispute requests",
    subtitleHi = "सपोर्ट टिकट बनाएं या ऑर्डर रिफंड/विवाद दर्ज करें",
    commissionBadge = "Audit Tracked",
    icon = Icons.AutoMirrored.Filled.HelpOutline,
  ),
}

@Composable
fun EarnHubScreen(
  currentUserId: String,
  currentUserName: String,
  myReferralCode: String,
  isHindi: Boolean,
  activeVertical: EarnVertical?,
  onSelectVertical: (EarnVertical?) -> Unit,
  earnViewModel: EarnViewModel,
) {
  if (activeVertical != null) {
    BackHandler { onSelectVertical(null) }
  }

  val tasksState by earnViewModel.tasks.collectAsStateWithLifecycle()
  val submissionsState by earnViewModel.submissions.collectAsStateWithLifecycle()
  val gigsState by earnViewModel.gigs.collectAsStateWithLifecycle()
  val freelanceOrdersState by earnViewModel.freelanceOrders.collectAsStateWithLifecycle()
  val productsState by earnViewModel.products.collectAsStateWithLifecycle()
  val productOrdersState by earnViewModel.productOrders.collectAsStateWithLifecycle()
  val coursesState by earnViewModel.courses.collectAsStateWithLifecycle()
  val tutorBookingsState by earnViewModel.tutorBookings.collectAsStateWithLifecycle()
  val localServicesState by earnViewModel.localServices.collectAsStateWithLifecycle()
  val serviceBookingsState by earnViewModel.serviceBookings.collectAsStateWithLifecycle()
  val affiliateOffersState by earnViewModel.affiliateOffers.collectAsStateWithLifecycle()
  val affiliateEventsState by earnViewModel.affiliateEvents.collectAsStateWithLifecycle()
  val generatedLink by earnViewModel.generatedAffiliateLink.collectAsStateWithLifecycle()
  val referralsState by earnViewModel.referrals.collectAsStateWithLifecycle()
  val ticketsState by earnViewModel.supportTickets.collectAsStateWithLifecycle()
  val disputesState by earnViewModel.disputes.collectAsStateWithLifecycle()

  Column(modifier = Modifier.fillMaxSize()) {
    if (activeVertical != null) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = { onSelectVertical(null) },
            modifier = Modifier.testTag("back_to_earn_hub"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Earn Hub",
            )
          }
          Spacer(Modifier.width(4.dp))
          Text(
            text = if (isHindi) activeVertical.titleHi else activeVertical.titleEn,
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }

      when (activeVertical) {
        EarnVertical.TASKS ->
          DailyTasksScreen(
            tasksState = tasksState,
            submissionsState = submissionsState,
            isHindi = isHindi,
            onSubmitProof = earnViewModel::submitTaskProof,
          )
        EarnVertical.GAME_TASKS ->
          EasyGameTasksScreen(
            tasksState = tasksState,
            submissionsState = submissionsState,
            isHindi = isHindi,
            onSubmitProof = earnViewModel::submitTaskProof,
          )
        EarnVertical.FREELANCING ->
          FreelancingScreen(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            gigsState = gigsState,
            ordersState = freelanceOrdersState,
            isHindi = isHindi,
            onCreateGig = earnViewModel::createGig,
            onOrderGig = earnViewModel::orderGig,
            onUpdateOrder = earnViewModel::updateFreelanceOrder,
          )
        EarnVertical.PRODUCTS ->
          ProductMarketplaceScreen(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            productsState = productsState,
            ordersState = productOrdersState,
            onCreateProduct = earnViewModel::createProduct,
            onOrderProduct = earnViewModel::orderProduct,
            onUpdateTracking = earnViewModel::updateProductTracking,
          )
        EarnVertical.AFFILIATE ->
          AffiliateMarketplaceScreen(
            offersState = affiliateOffersState,
            eventsState = affiliateEventsState,
            generatedLink = generatedLink,
            onGenerateLink = earnViewModel::generateAffiliateLink,
            onClearLink = earnViewModel::clearGeneratedLink,
          )
        EarnVertical.REFERRALS ->
          ReferralProgramScreen(
            myReferralCode = myReferralCode,
            referralsState = referralsState,
            onApplyReferralCode = earnViewModel::applyReferralCode,
          )
        EarnVertical.TEACHING ->
          TeachingMarketplaceScreen(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            coursesState = coursesState,
            bookingsState = tutorBookingsState,
            onCreateCourse = earnViewModel::createCourse,
            onBookCourse = earnViewModel::bookCourse,
            onUpdateBooking = earnViewModel::updateTutorBooking,
          )
        EarnVertical.LOCAL_SERVICES ->
          LocalServicesScreen(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            servicesState = localServicesState,
            bookingsState = serviceBookingsState,
            onCreateService = earnViewModel::createService,
            onBookService = earnViewModel::bookService,
            onUpdateBooking = earnViewModel::updateServiceBooking,
          )
        EarnVertical.SUPPORT ->
          SupportAndDisputesScreen(
            ticketsState = ticketsState,
            disputesState = disputesState,
            onSubmitTicket = earnViewModel::submitTicket,
            onSubmitDispute = earnViewModel::submitDispute,
          )
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          Text(
            text = if (isHindi) "कमाई सुपर-हब" else "EarnMore Super Hub",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text =
              if (isHindi) "सत्यापित कार्य, फ्रीलांसिंग, उत्पाद, शिक्षण और स्थानीय सेवाएं।"
              else "Genuine skill-based work with transparent platform commissions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        items(EarnVertical.entries, key = { it.name }) { vertical ->
          Card(
            onClick = { onSelectVertical(vertical) },
            modifier = Modifier.fillMaxWidth().testTag(vertical.tag),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                color = NavyPrimary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
              ) {
                Icon(
                  imageVector = vertical.icon,
                  contentDescription = vertical.titleEn,
                  tint = NavyPrimary,
                  modifier = Modifier.padding(12.dp).size(26.dp),
                )
              }
              Spacer(Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (isHindi) vertical.titleHi else vertical.titleEn,
                  style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                  text = if (isHindi) vertical.subtitleHi else vertical.subtitleEn,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                  color = EmeraldAccent.copy(alpha = 0.14f),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text(
                    text = vertical.commissionBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
