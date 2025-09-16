package com.engfred.lockit.presentation.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.lockit.presentation.ui.theme.LockItTheme
import com.engfred.lockit.presentation.viewmodel.LockViewModel
import com.engfred.lockit.service.UnlockEventBus
import com.engfred.lockit.presentation.ui.components.NumericKeypad
import com.engfred.lockit.presentation.ui.components.PinDots
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import kotlin.math.roundToInt

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {

    @Inject lateinit var unlockEventBus: UnlockEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Secure flags
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Show on lock screen & turn screen on where appropriate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        // Disable back button to prevent bypass
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        })

        setContent {
            LockItTheme {
                Surface {
                    LockScreen(
                        viewModel = hiltViewModel(),
                        unlockEventBus = unlockEventBus
                    )
                }
            }
        }
    }
}

@Composable
fun LockScreen(viewModel: LockViewModel, unlockEventBus: UnlockEventBus) {
    val context = LocalContext.current
    val lockedPackage = (context as Activity).intent.getStringExtra("locked_package")
    var pin by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Prevent back
    BackHandler(enabled = true) { /* no-op */ }

    // Shake animatable (for horizontal offset)
    val shakeX = remember { Animatable(0f) }

    // Density & haptic
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Load app icon & label (if available). Prefer disk cache (cacheDir/app_icons/<pkg>.png), else fallback to PackageManager drawable.
    val (appLabel, appIconBitmap) = remember(lockedPackage) {
        var label: String? = null
        var bitmapDrawable: androidx.compose.ui.graphics.ImageBitmap? = null
        try {
            if (!lockedPackage.isNullOrBlank()) {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(lockedPackage, PackageManager.GET_META_DATA)
                label = pm.getApplicationLabel(info)?.toString()

                // Try disk cache created by repository: cacheDir/app_icons/<pkg>.png
                val cacheFile = File(context.cacheDir, "app_icons/$lockedPackage.png")
                if (cacheFile.exists()) {
                    try {
                        val bytes = cacheFile.readBytes()
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            bitmapDrawable = bmp.asImageBitmap()
                        }
                    } catch (t: Throwable) {
                        // Ignore and fallback to drawable
                    }
                }

                if (bitmapDrawable == null) {
                    // Fallback to PackageManager's icon
                    val drawable = pm.getApplicationIcon(info)
                    val bmp = drawable.toBitmap()
                    bitmapDrawable = bmp.asImageBitmap()
                }
            }
        } catch (e: Exception) {
            // Ignore — leave nulls
        }
        Pair(label, bitmapDrawable)
    }

    // Auto-validate when full length reached
    val maxDigits = 6
    LaunchedEffect(pin) {
        if (pin.length == maxDigits) {
            coroutineScope.launch {
                val ok = viewModel.validatePin(pin)
                if (ok) {
                    if (!lockedPackage.isNullOrEmpty()) unlockEventBus.emitUnlockedPackage(lockedPackage)
                    (context as Activity).finish()
                } else {
                    // Wrong PIN -> trigger haptic + shake + toast + clear
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Compute dp->px for consistent shake amplitude
                    val dxPx = with(density) { 24.dp.toPx() }

                    // Animate shake via keyframes (absolute px values)
                    shakeX.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 520
                            (-dxPx) at 0
                            (dxPx) at 80
                            (-dxPx * 0.75f) at 160
                            (dxPx * 0.45f) at 240
                            (-dxPx * 0.25f) at 320
                            (dxPx * 0.12f) at 400
                            0f at 520
                        }
                    )

                    // Show Toast instead of error text to avoid layout shifts
                    Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()

                    // Small delay, then clear
                    delay(120L)
                    pin = ""
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp), // Adjusted padding for balanced, premium spacing
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // SpaceBetween to push copyright to bottom
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f) // Take available space
        ) {
            // Show app icon & label if available — no background container, icon drawn directly
            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = appLabel ?: "App icon",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = appLabel ?: "Enter PIN to Unlock",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dots area with shake effect applied
            Box(modifier = Modifier.offset { IntOffset(shakeX.value.roundToInt(), 0) }, contentAlignment = Alignment.Center) {
                PinDots(pinLength = pin.length, slots = maxDigits, dotSize = 20.dp, spacing = 18.dp) // Larger for addictive visibility
            }

            Spacer(modifier = Modifier.height(24.dp))

            NumericKeypad(
                onNumber = { digit ->
                    if (pin.length < maxDigits) pin += digit.toString()
                },
                onBackspace = {
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                },
                onSubmit = null
            )
        }

        // Copyright text at the bottom
        Text(
            text = "© 2025 Engineer Fred",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Subtle color for professionalism
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp) // Padding to avoid edge
        )
    }
}