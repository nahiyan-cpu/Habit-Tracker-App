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
import com.example.ui.screens.ActiveSessionUser
import com.example.ui.screens.AuthFlowHost
import com.example.ui.screens.MainAppShell
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.HabitlyTheme
import com.example.ui.viewmodel.AppPreferencesViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

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
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as HabitlyApplication }

    val prefsViewModel: AppPreferencesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) as HabitlyApplication
                AppPreferencesViewModel(application.container.preferencesDataStore)
            }
        }
    )

    val preferences by prefsViewModel.preferences.collectAsStateWithLifecycle()
    val firebaseAuth = remember(app) { app.container.auth }
    val authFlow = remember(firebaseAuth) {
        firebaseAuth?.authStateFlow() ?: flowOf<FirebaseUser?>(null)
    }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = firebaseAuth?.currentUser)

    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(firebaseAuth) {
        if (firebaseAuth != null) {
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
        delay(650L)
        showSplash = false
    }

    val isCloudAvailable = firebaseAuth != null
    val sessionUser: ActiveSessionUser? = when {
        currentUser != null -> {
            val u = currentUser!!
            ActiveSessionUser(
                uid = u.uid,
                displayName = u.displayName ?: "Habitly Member",
                email = u.email ?: "",
                photoUrl = u.photoUrl?.toString(),
                isCloudBacked = true
            )
        }
        !isCloudAvailable && preferences.localModeEnabled -> {
            ActiveSessionUser(
                uid = "local_habitly_user",
                displayName = "Habitly Explorer",
                email = "Local Device Storage",
                photoUrl = null,
                isCloudBacked = false
            )
        }
        else -> null
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
                sessionUser == null -> {
                    AuthFlowHost(
                        isCloudAvailable = isCloudAvailable,
                        onContinueOffline = { prefsViewModel.setLocalModeEnabled(true) },
                        onAuthSuccess = {}
                    )
                }
                else -> {
                    key(sessionUser.uid) {
                        MainAppShell(
                            sessionUser = sessionUser,
                            onLocalSignOut = { prefsViewModel.setLocalModeEnabled(false) }
                        )
                    }
                }
            }
        }
    }
}
