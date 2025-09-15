package com.engfred.lockit.presentation.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.engfred.lockit.R
import com.engfred.lockit.presentation.viewmodel.SetupViewModel
import com.engfred.lockit.service.AppLockerService
import com.engfred.lockit.utils.AccessibilityUtils
import kotlinx.coroutines.launch

/**
 * Full SetupScreen file — includes permission check re-evaluation on resume,
 * PIN UI, and the small permission helper screens.
 *
 * Replace your existing SetupScreen with this file. Keep supporting classes (AccessibilityUtils)
 * untouched — this file depends on them.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    navController: NavController,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Focus requesters for PIN fields
    val pinFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }

    // Permissions: make mutable states that will be refreshed onResume
    val isAccessibilityEnabled = remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context, AppLockerService::class.java)) }
    val hasUsageStatsPermission = remember { mutableStateOf(hasUsageAccess(context)) }

    // Lifecycle observer to re-check permissions when user returns from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-evaluate permissions on resume so UI updates immediately when user grants them
                isAccessibilityEnabled.value = AccessibilityUtils.isAccessibilityServiceEnabled(context, AppLockerService::class.java)
                hasUsageStatsPermission.value = hasUsageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // If Accessibility not granted, show permission screen
    if (!isAccessibilityEnabled.value) {
        AccessibilityPermissionScreen {
            AccessibilityUtils.openAccessibilitySettings(context)
        }
        return
    }

    // If Usage access not granted, show permission screen
    if (!hasUsageStatsPermission.value) {
        UsagePermissionScreen {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        return
    }

    // Normal setup UI (PIN creation)
    LaunchedEffect(Unit) { pinFocusRequester.requestFocus() }
    LaunchedEffect(pin.length) { if (pin.length == 6 && confirmPin.isEmpty()) confirmFocusRequester.requestFocus() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Set Up PIN", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Keep your apps private — choose a secure 6-digit PIN",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.95f)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Create a secure PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Use 6 digits. Don’t use easily guessed numbers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    // PIN inputs
                    PinField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = "Enter 6-digit PIN",
                        slots = 6,
                        focusRequester = pinFocusRequester
                    )

                    PinField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6) confirmPin = it },
                        label = "Confirm PIN (6 digits)",
                        slots = 6,
                        focusRequester = confirmFocusRequester
                    )

                    AnimatedVisibility(visible = error.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        PinStrengthIndicator(pin = pin, maxWidth = 120.dp)
                        Text("${pin.length}/6", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    FilledTonalButton(
                        onClick = {
                            error = ""
                            when {
                                pin.length != 6 -> error = "PIN must be 6 digits"
                                confirmPin.length != 6 -> error = "Confirm PIN must be 6 digits"
                                pin != confirmPin -> error = "PINs don't match"
                                else -> {
                                    coroutineScope.launch {
                                        try {
                                            val success = viewModel.setPin(pin)
                                            if (success) {
                                                navController.navigate("app_list") {
                                                    popUpTo("setup") { inclusive = true }
                                                }

                                                // If accessibility not enabled yet (rare), open settings
                                                if (!AccessibilityUtils.isAccessibilityServiceEnabled(context, AppLockerService::class.java)) {
                                                    AccessibilityUtils.openAccessibilitySettings(context)
                                                }
                                            } else {
                                                error = "Error setting PIN"
                                            }
                                        } catch (e: Exception) {
                                            error = "Error setting PIN"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set PIN", style = MaterialTheme.typography.labelLarge)
                    }

                    TextButton(onClick = { AccessibilityUtils.openAccessibilitySettings(context) }) {
                        Text("Accessibility settings", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}


/* -------------------------------------------------------
   Permission helper screens + PIN composables (self-contained)
   ------------------------------------------------------- */

@Composable
private fun AccessibilityPermissionScreen(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                tonalElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🔒", fontSize = 36.sp)
                }
            }

            Text(
                "Enable Accessibility Service",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                "LockIt needs Accessibility to detect and lock apps in real-time. Please enable it in settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun UsagePermissionScreen(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                tonalElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🔒", fontSize = 36.sp)
                }
            }

            Text(
                "Grant Usage Access Permission",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                "LockIt needs Usage Access to detect foreground apps and protect them. Please enable it in settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text("Open Settings")
            }
        }
    }
}

/**
 * A polished pin field made of segmented slots + invisible text field.
 */
@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    slots: Int = 6,
    focusRequester: FocusRequester
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() }
        ) {
            val displayed = value.padEnd(slots, ' ')
            for (i in 0 until slots) {
                val char = displayed.getOrNull(i) ?: ' '
                PinSlot(char = char, enabled = i < value.length)
            }
        }

        // Hidden TextField that receives input
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                val filtered = new.filter { it.isDigit() }
                onValueChange(filtered.take(slots))
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .height(0.dp)
                .alpha(0f),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun PinSlot(char: Char, enabled: Boolean) {
    val dotAlpha by animateFloatAsState(targetValue = if (enabled) 1f else 0f, animationSpec = tween(200))
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (char != ' ') {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface)
                    .alpha(dotAlpha)
            )
        } else {
            Box(modifier = Modifier.size(14.dp).alpha(0.02f))
        }
    }
}

/**
 * Tiny visual indicator — encourages slightly stronger PINs (UX affordance)
 */
@Composable
private fun PinStrengthIndicator(pin: String, maxWidth: Dp) {
    val strength = when {
        pin.length >= 6 -> 3
        pin.length >= 5 -> 2
        pin.length >= 4 -> 1
        else -> 0
    }
    val color = when (strength) {
        3 -> Color(0xFF2ECC71) // green
        2 -> Color(0xFFF1C40F) // amber
        1 -> Color(0xFFE67E22) // orange
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val active = index < strength
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width((maxWidth / 3f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            )
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
