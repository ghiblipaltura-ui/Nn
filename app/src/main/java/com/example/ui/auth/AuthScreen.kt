package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite

@Composable
fun AuthScreen(
  language: String,
  onLanguageChange: (String) -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var consentAccepted by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showLegalDialog by remember { mutableStateOf(false) }

  val isHindi = language == "hi"

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(NavyDeep, NavyPrimary, MaterialTheme.colorScheme.background)
          )
        )
        .safeDrawingPadding()
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        // Language switcher
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = EmeraldAccent.copy(alpha = 0.18f),
            shape = RoundedCornerShape(999.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = EmeraldAccent,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = "TEST MODE • ₹0 MVP",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !isHindi,
              onClick = { onLanguageChange("en") },
              label = { Text("EN") },
              leadingIcon = {
                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.testTag("lang_en_chip"),
            )
            FilterChip(
              selected = isHindi,
              onClick = { onLanguageChange("hi") },
              label = { Text("हिन्दी") },
              modifier = Modifier.testTag("lang_hi_chip"),
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        // Hero Banner
        Image(
          painter = painterResource(id = R.drawable.img_earnmore_hero_1794678454149),
          contentDescription = "EarnMore India marketplace illustration",
          modifier =
            Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)),
          contentScale = ContentScale.Crop,
        )

        Spacer(Modifier.height(20.dp))

        Text(
          text = "EarnMore India",
          style = MaterialTheme.typography.displaySmall,
          color = PureWhite,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text =
            if (isHindi) "असली काम। पारदर्शी कमीशन। वास्तविक कौशल बाज़ार।"
            else "Genuine Work. Transparent Commissions. Real Skills Marketplace.",
          style = MaterialTheme.typography.bodyLarge,
          color = PureWhite.copy(alpha = 0.85f),
        )

        Spacer(Modifier.height(20.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = if (isHindi) "सुरक्षित खाता प्रवेश" else "Verified Marketplace Access",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            TrustPoint(
              icon = Icons.Filled.AccountBalanceWallet,
              text =
                if (isHindi) "अपरिवर्तनीय वॉलेट लेज़र और पारदर्शी कमीशन विवरण"
                else "Immutable ledger & transparent commission breakdown on every order",
            )
            Spacer(Modifier.height(8.dp))
            TrustPoint(
              icon = Icons.Filled.Security,
              text =
                if (isHindi) "सिंगल-टियर रेफरल (कोई MLM या पोंज़ी स्कीम नहीं)"
                else "Single-tier genuine referrals only (strictly no MLM or Ponzi payouts)",
            )

            Spacer(Modifier.height(16.dp))

            // DPDP Act & Legal Consent Checkbox
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top,
            ) {
              Checkbox(
                checked = consentAccepted,
                onCheckedChange = { consentAccepted = it },
                modifier = Modifier.testTag("legal_consent_checkbox"),
              )
              Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                Text(
                  text =
                    if (isHindi)
                      "मैं नियम एवं शर्तें, गोपनीयता नीति (DPDP 2023), रिफंड नीति और TEST MODE भुगतान नियमों से सहमत हूँ।"
                    else
                      "I accept the Terms, Privacy Policy (DPDP Act 2023), Refund Policy, and TEST MODE payment disclosures.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                  onClick = { showLegalDialog = true },
                  modifier = Modifier.testTag("view_legal_policies_button"),
                ) {
                  Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(if (isHindi) "कानूनी नीतियां पढ़ें" else "Read Legal & Tax Policies")
                }
              }
            }

            if (errorMessage != null) {
              Spacer(Modifier.height(8.dp))
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = errorMessage!!,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }

            Spacer(Modifier.height(16.dp))

            Button(
              onClick = {
                if (!consentAccepted) {
                  errorMessage =
                    if (isHindi) "जारी रखने के लिए कृपया कानूनी सहमति स्वीकार करें।"
                    else "Please accept the legal & privacy consent to continue."
                  return@Button
                }
                errorMessage = null
                isLoading = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isLoading = false
                    errorMessage = msg
                  },
                  scope = scope,
                  onAuthCancelled = { isLoading = false },
                )
              },
              enabled = !isLoading,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = EmeraldAccent,
                  contentColor = PureWhite,
                ),
              modifier =
                Modifier.fillMaxWidth().height(54.dp).testTag("google_sign_in_button"),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = PureWhite,
                  strokeWidth = 2.5.dp,
                )
              } else {
                Text(
                  text = if (isHindi) "Google से साइन इन करें" else "Sign in with Google",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
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
      title = { Text(if (isHindi) "कानूनी एवं अनुपालन नीतियां" else "Legal, Privacy & Tax Policies") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(
            text =
              "1. Terms & Genuine Work: EarnMore India connects verified workers, freelancers, tutors, and service providers. No guaranteed income claims are made.\n\n" +
                "2. Privacy (DPDP Act 2023): We collect only essential profile data. We never store UPI PINs, bank passwords, or card CVVs.\n\n" +
                "3. Anti-Pyramid Compliance: Referrals are strictly single-tier and unlock only after verified eligible activity.\n\n" +
                "4. Refund & Dispute Policy: Escrowed order funds are protected and subject to admin dispute review.\n\n" +
                "5. GST & Income Tax: Users are responsible for applicable Indian income tax declarations; platform fees track GST readiness.",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showLegalDialog = false }) { Text("OK") }
      },
    )
  }
}

@Composable
private fun TrustPoint(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = EmeraldAccent,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(10.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
