package com.engfred.lockit.presentation.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.engfred.lockit.presentation.ui.components.NumericKeypad
import com.engfred.lockit.presentation.ui.components.PermissionView
import com.engfred.lockit.presentation.ui.components.PinDots
import com.engfred.lockit.presentation.ui.components.StepIndicator
import com.engfred.lockit.presentation.ui.components.StepStatus
import com.engfred.lockit.presentation.viewmodel.SetupViewModel
import com.engfred.lockit.service.AppLockerService
import com.engfred.lockit.utils.AccessibilityUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(navController: NavController, viewModel: SetupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var primaryPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val maxDigits = 6

    // active step: 0 = enter primary, 1 = confirm
    var activeField by rememberSaveable { mutableIntStateOf(0) }

    // Permissions: re-evaluate on resume
    val isAccessibilityEnabled = rememberSaveable { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context, AppLockerService::class.java)) }
    val hasUsageStatsPermission = rememberSaveable { mutableStateOf(hasUsageAccess(context)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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

    // Helper for step statuses
    fun currentStatuses(): List<StepStatus> {
        return when {
            !isAccessibilityEnabled.value -> listOf(StepStatus.CURRENT, StepStatus.PENDING)
            !hasUsageStatsPermission.value -> listOf(StepStatus.GRANTED, StepStatus.CURRENT)
            else -> listOf(StepStatus.GRANTED, StepStatus.GRANTED)
        }
    }

    // PERMISSION FLOWS (each centers its content; step indicator anchored bottom)
    if (!isAccessibilityEnabled.value) {
        val steps = listOf("Accessibility", "Usage Access")
        val statuses = currentStatuses()

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PermissionView(
                    title = "Enable Accessibility Service",
                    description = "LockIt needs Accessibility to detect and lock apps in real-time. Please enable it in settings.",
                    onOpenSettings = { AccessibilityUtils.openAccessibilitySettings(context) }
                )
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp), contentAlignment = Alignment.Center) {
                StepIndicator(steps = steps, statuses = statuses, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            }
        }
        return
    }

    if (!hasUsageStatsPermission.value) {
        val steps = listOf("Accessibility", "Usage Access")
        val statuses = currentStatuses()

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PermissionView(
                    title = "Grant Usage Access Permission",
                    description = "LockIt needs Usage Access to detect foreground apps and protect them. Please enable it in settings.",
                    onOpenSettings = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp), contentAlignment = Alignment.Center) {
                StepIndicator(steps = steps, statuses = statuses, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            }
        }
        return
    }

    // When primary is fully entered, switch to confirm automatically
    LaunchedEffect(primaryPin) {
        if (primaryPin.length == maxDigits) {
            // clear any previous confirm and reset error when moving to confirm
            error = null
            confirmPin = ""
            activeField = 1
        }
    }

    //show a toast in case of an error
    LaunchedEffect(error) {
        if (error != null) {
            // show error toast
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            error = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Set Up PIN", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Keep your apps private — choose a secure 6-digit PIN",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.95f)
                    )
                }
            })
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                StepIndicator(
                    steps = listOf("Accessibility", "Usage Access"),
                    statuses = listOf(StepStatus.GRANTED, StepStatus.GRANTED),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ).verticalScroll(rememberScrollState())
        ) {
            // Centered Card with single-field PIN flow (only one label/dots visible at a time)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Create a secure PIN", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Use 6 digits. Don’t use easily guessed numbers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // CROSSFADE between Enter and Confirm UI to keep transitions smooth
                        Crossfade(targetState = activeField) { field ->
                            when (field) {
                                0 -> {
                                    // Enter primary PIN
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Enter PIN", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        PinDots(pinLength = primaryPin.length, slots = maxDigits)
                                    }
                                }
                                1 -> {
                                    // Confirm PIN
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Confirm PIN", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        PinDots(pinLength = confirmPin.length, slots = maxDigits)
                                    }
                                }
                                else -> { /* no-op */ }
                            }
                        }

                        // Optional small back control when confirming
                        AnimatedVisibility(visible = activeField == 1) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                TextButton(onClick = {
                                    // go back to editing primary
                                    error = null
                                    activeField = 0
                                    // remove last digit from primary so user can continue typing if desired
                                    if (primaryPin.isNotEmpty()) primaryPin = primaryPin.dropLast(1)
                                }) {
                                    Text("Back")
                                }
                            }
                        }

                        // Numeric keypad: inputs go to the currently active field only
                        NumericKeypad(
                            onNumber = { d ->
                                if (activeField == 0) {
                                    if (primaryPin.length < maxDigits) primaryPin += d.toString()
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

                        // Show Set PIN only when confirming (activeField == 1)
                        AnimatedVisibility(visible = activeField == 1) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Set PIN")
                            }
                        }
                    }
                }
            }

        }
    }
}

/**
 * Utility to check Usage Access (same as your original helper).
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
