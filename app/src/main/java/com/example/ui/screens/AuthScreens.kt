package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.ui.navigation.AuthRoute

@Composable
fun AuthFlowHost(onAuthSuccess: () -> Unit) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AuthRoute.Login
    ) {
        composable<AuthRoute.Login> {
            LoginScreen(
                onAuthSuccess = onAuthSuccess,
                onNavigateRegister = { navController.navigate(AuthRoute.Register) },
                onNavigateForgotPassword = { navController.navigate(AuthRoute.ForgotPassword) }
            )
        }
        composable<AuthRoute.Register> {
            RegisterScreen(
                onAuthSuccess = onAuthSuccess,
                onBack = { navController.popBackStack() }
            )
        }
        composable<AuthRoute.ForgotPassword> {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmailInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(id = R.drawable.img_habitly_icon),
                contentDescription = "Habitly icon",
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Welcome to Habitly",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to sync your habits, streaks, analytics, and private journal securely across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuthBenefitRow(
                        icon = Icons.Rounded.CloudDone,
                        title = "Offline-First & Cloud Sync",
                        subtitle = "Track offline anytime; automatic backup to Cloud Firestore."
                    )
                    AuthBenefitRow(
                        icon = Icons.Rounded.Lock,
                        title = "Private & Isolated Data",
                        subtitle = "Your journal and habits are protected by per-user Firebase rules."
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            GoogleSignInButton(
                label = "Sign in with Google",
                onAuthSuccess = {
                    errorMessage = null
                    onAuthSuccess()
                },
                onAuthError = { errorMessage = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onNavigateRegister,
                    modifier = Modifier.testTag("create_account_link")
                ) {
                    Text("Create Account")
                }
                TextButton(
                    onClick = onNavigateForgotPassword,
                    modifier = Modifier.testTag("forgot_password_link")
                ) {
                    Text("Account Recovery")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { showEmailInfoDialog = true },
                modifier = Modifier.testTag("email_provider_info_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("About sign-in providers")
            }
        }
    }

    if (showEmailInfoDialog) {
        AlertDialog(
            onDismissRequest = { showEmailInfoDialog = false },
            title = { Text("Authentication Providers") },
            text = {
                Text(
                    "Habitly uses Google Sign-In via Android Credential Manager as its active provisioned identity provider. " +
                        "If you prefer Email/Password authentication in your own deployment, enable the Email/Password sign-in method in the Firebase Console."
                )
            },
            confirmButton = {
                TextButton(onClick = { showEmailInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun RegisterScreen(
    onAuthSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .widthIn(max = 520.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("register_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to login")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Create Your Habitly Account",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "New accounts are automatically created and provisioned in Cloud Firestore the first time you sign in with Google.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            GoogleSignInButton(
                label = "Create Account with Google",
                onAuthSuccess = {
                    errorMessage = null
                    onAuthSuccess()
                },
                onAuthError = { errorMessage = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_register_button")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .widthIn(max = 520.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("recovery_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to login")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Account & Password Recovery",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Because Habitly authenticates securely with your Google Account, password recovery and 2-step verification are managed directly by Google Account Recovery.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://accounts.google.com/signin/recovery")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("open_google_recovery_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open Google Account Recovery")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Return to Sign In")
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    label: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            GoogleAuthHelper.onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isLoading = false
                    onAuthError(msg)
                },
                scope = coroutineScope,
                onAuthCancelled = { isLoading = false }
            )
        },
        enabled = !isLoading,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AuthBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
