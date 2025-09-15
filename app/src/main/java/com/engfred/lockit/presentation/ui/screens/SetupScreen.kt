package com.engfred.lockit.presentation.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.engfred.lockit.presentation.ui.components.NumericKeypad
import com.engfred.lockit.presentation.ui.components.PinDots
import com.engfred.lockit.presentation.viewmodel.SetupViewModel
import com.engfred.lockit.service.AppLockerService
import com.engfred.lockit.utils.AccessibilityUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(navController: NavController, viewModel: SetupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var primaryPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val maxDigits = 6

    // active field: 0 = primary, 1 = confirm
    var activeField by remember { mutableIntStateOf(0) }

    // Permissions: re-evaluate on resume (unchanged)
    val isAccessibilityEnabled = remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context, AppLockerService::class.java)) }
    val hasUsageStatsPermission = remember { mutableStateOf(hasUsageAccess(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(context, AppLockerService::class.java)
                hasUsageStatsPermission.value = hasUsageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!isAccessibilityEnabled.value) {
        AccessibilityPermissionScreen { AccessibilityUtils.openAccessibilitySettings(context) }
        return
    }
    if (!hasUsageStatsPermission.value) {
        UsagePermissionScreen { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        return
    }

    // auto-switch to confirm when primary filled
    LaunchedEffect(primaryPin) {
        if (primaryPin.length == maxDigits) {
            activeField = 1
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Set Up PIN", style = MaterialTheme.typography.titleLarge)
                    Text("Keep your apps private — choose a secure 6-digit PIN", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.95f))
                }
            })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.04f), MaterialTheme.colorScheme.background))
        )) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).align(Alignment.Center), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Create a secure PIN", style = MaterialTheme.typography.headlineSmall)
                        Text("Use 6 digits. Don’t use easily guessed numbers.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Primary field
                    Text("Enter PIN", style = MaterialTheme.typography.bodyMedium)
                    PinDots(pinLength = primaryPin.length, slots = maxDigits)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Confirm field
                    Text("Confirm PIN", style = MaterialTheme.typography.bodyMedium)
                    PinDots(pinLength = confirmPin.length, slots = maxDigits)

                    AnimatedVisibility(visible = !error.isNullOrEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Numpad controls: add digit to active field, support backspace + field switching
                    NumericKeypad(
                        onNumber = { d ->
                            if (activeField == 0) {
                                if (primaryPin.length < maxDigits) primaryPin += d.toString()
                                if (primaryPin.length == maxDigits) activeField = 1
                            } else {
                                if (confirmPin.length < maxDigits) confirmPin += d.toString()
                            }
                        },
                        onBackspace = {
                            if (activeField == 1 && confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                            } else if (activeField == 1 && confirmPin.isEmpty()) {
                                // go back to primary if confirm empty
                                activeField = 0
                                if (primaryPin.isNotEmpty()) primaryPin = primaryPin.dropLast(1)
                            } else if (activeField == 0 && primaryPin.isNotEmpty()) {
                                primaryPin = primaryPin.dropLast(1)
                            }
                        },
                        onSubmit = null
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FilledTonalButton(
                        onClick = {
                            error = null
                            when {
                                primaryPin.length != maxDigits -> error = "PIN must be 6 digits"
                                confirmPin.length != maxDigits -> error = "Confirm PIN must be 6 digits"
                                primaryPin != confirmPin -> error = "PINs don't match"
                                else -> {
                                    coroutineScope.launch {
                                        try {
                                            val success = viewModel.setPin(primaryPin)
                                            if (success) {
                                                navController.navigate("app_list") { popUpTo("setup") { inclusive = true } }
                                            } else {
                                                error = "Error saving PIN"
                                            }
                                        } catch (e: Exception) {
                                            error = "Error saving PIN"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set PIN")
                    }

                    TextButton(onClick = { AccessibilityUtils.openAccessibilitySettings(context) }) {
                        Text("Accessibility settings")
                    }
                }
            }
        }
    }
}

/**
 * Helper permission screens reused from your previous file. Keep these as-is or reuse the versions you already have.
 */
@Composable
private fun AccessibilityPermissionScreen(onOpenSettings: () -> Unit) {
    // same content as before (shortened in this snippet because unchanged)
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(96.dp), shape = CircleShape, tonalElevation = 6.dp) {
                Box(contentAlignment = Alignment.Center) { Text("🔒") }
            }
            Text("Enable Accessibility Service", style = MaterialTheme.typography.titleMedium)
            Text("LockIt needs Accessibility to detect and lock apps in real-time. Please enable it in settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(0.7f)) { Text("Open Settings") }
        }
    }
}

@Composable
private fun UsagePermissionScreen(onOpenSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(96.dp), shape = CircleShape, tonalElevation = 6.dp) { Box(contentAlignment = Alignment.Center) { Text("🔒") } }
            Text("Grant Usage Access Permission", style = MaterialTheme.typography.titleMedium)
            Text("LockIt needs Usage Access to detect foreground apps and protect them. Please enable it in settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(0.7f)) { Text("Open Settings") }
        }
    }
}

/**
 * Check whether the app has Usage Access permission.
 */
private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
