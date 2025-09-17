package com.engfred.lockit.presentation.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.engfred.lockit.presentation.ui.components.NumericKeypad
import com.engfred.lockit.presentation.ui.components.PinDots
import com.engfred.lockit.presentation.viewmodel.LockViewModel
import com.engfred.lockit.service.UnlockEventBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun LockScreen(viewModel: LockViewModel, unlockEventBus: UnlockEventBus) {
    val context = LocalContext.current
    val lockedPackage = (context as Activity).intent.getStringExtra("locked_package")
    var pin by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Prevent back
    BackHandler(enabled = true) { /* no-op */ }

    // Shake animatable (for horizontal offset)
    val shakeX = remember { Animatable(0f) }

    // Density & haptic
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Load app icon & label (if available). Prefer disk cache, fallback to PackageManager.
    val (appLabel, appIconBitmap) = remember(lockedPackage) {
        var label: String? = null
        var bitmapDrawable: androidx.compose.ui.graphics.ImageBitmap? = null
        try {
            if (!lockedPackage.isNullOrBlank()) {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(lockedPackage, 0) // Removed GET_META_DATA if not needed
                label = pm.getApplicationLabel(info)?.toString()

                // Try disk cache
                val cacheFile = File(context.cacheDir, "app_icons/$lockedPackage.png")
                if (cacheFile.exists()) {
                    val bytes = cacheFile.readBytes()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        bitmapDrawable = bmp.asImageBitmap()
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
            // Log and ignore
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

                    Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()

                    delay(120L)
                    pin = ""
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
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

            Box(modifier = Modifier.offset { IntOffset(shakeX.value.roundToInt(), 0) }, contentAlignment = Alignment.Center) {
                PinDots(pinLength = pin.length, slots = maxDigits, dotSize = 20.dp, spacing = 18.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            NumericKeypad(
                onNumber = { digit ->
                    if (pin.length < maxDigits) {
                        pin += digit.toString()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Optional subtle feedback
                    }
                },
                onBackspace = {
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onSubmit = null
            )
        }

        Text(
            text = "© 2025 Engineer Fred",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .semantics { role = Role.Image } // Accessibility improvement
        )
    }
}