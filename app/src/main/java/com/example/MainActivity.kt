package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.ui.MainAppShell
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.theme.EarnMoreTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      EarnMoreTheme {
        EarnMoreRoot()
      }
    }
  }
}

@Composable
fun EarnMoreRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var preferredLanguage by remember { mutableStateOf("en") }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  LaunchedEffect(Unit) {
    if (currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { currentUser = Firebase.auth.currentUser },
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      language = preferredLanguage,
      onLanguageChange = { preferredLanguage = it },
      onAuthSuccess = { currentUser = Firebase.auth.currentUser },
    )
  } else {
    key(user.uid) {
      MainAppShell(
        currentUserId = user.uid,
        language = preferredLanguage,
        onLanguageChange = { preferredLanguage = it },
      )
    }
  }
}
