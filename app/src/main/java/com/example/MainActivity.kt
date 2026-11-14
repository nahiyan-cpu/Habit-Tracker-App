package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthHelper
import com.example.auth.authStateFlow
import com.example.di.HabitlyApplication
import com.example.ui.screens.AuthFlowHost
import com.example.ui.screens.MainAppShell
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.HabitlyTheme
import com.example.ui.viewmodel.AppPreferencesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitlyRoot()
        }
    }
}

@Composable
fun HabitlyRoot() {
    val prefsViewModel: AppPreferencesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as HabitlyApplication
                AppPreferencesViewModel(app.container.preferencesDataStore)
            }
        }
    )

    val preferences by prefsViewModel.preferences.collectAsStateWithLifecycle()
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
        delay(650L)
        showSplash = false
    }

    HabitlyTheme(
        themeMode = preferences.themeMode,
        accentColorHex = preferences.accentColorHex
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when {
                showSplash -> {
                    SplashScreen()
                }
                !preferences.onboardingCompleted -> {
                    OnboardingScreen(
                        onFinish = { prefsViewModel.completeOnboarding() }
                    )
                }
                currentUser == null -> {
                    AuthFlowHost(onAuthSuccess = {})
                }
                else -> {
                    val user = currentUser!!
                    key(user.uid) {
                        MainAppShell(firebaseUser = user)
                    }
                }
            }
        }
    }
}
