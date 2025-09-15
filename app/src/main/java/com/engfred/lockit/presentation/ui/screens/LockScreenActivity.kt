package com.engfred.lockit.presentation.ui.screens

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import com.engfred.lockit.R
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {

    @Inject lateinit var unlockEventBus: UnlockEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // secure flags
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // show on lock screen & turn screen on where appropriate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        // disable back button to prevent bypass
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
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // prevent back
    BackHandler(enabled = true) { /* no-op */ }

    // shake animatable (for horizontal offset)
    val shakeX = remember { Animatable(0f) }

    // density & haptic
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Load app icon & label (if available)
    val (appLabel, appIconBitmap) = remember(lockedPackage) {
        var label: String? = null
        var bitmapDrawable: androidx.compose.ui.graphics.ImageBitmap? = null
        try {
            if (!lockedPackage.isNullOrBlank()) {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(lockedPackage, PackageManager.GET_META_DATA)
                label = pm.getApplicationLabel(info)?.toString()
                val drawable = pm.getApplicationIcon(info)
                val bmp = drawable.toBitmap()
                bitmapDrawable = bmp.asImageBitmap()
            }
        } catch (e: Exception) {
            // ignore — leave nulls
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
                    // wrong PIN -> trigger haptic + shake + clear
                    error = "Incorrect PIN"

                    // haptic
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // compute dp->px for consistent shake amplitude
                    val dxPx = with(density) { 24.dp.toPx() }

                    // animate shake via keyframes (absolute px values)
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

                    // small delay, then clear
                    delay(120L)
                    pin = ""
                    error = null
                }
            }
        } else {
            error = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Show app icon + label if available
        if (appIconBitmap != null) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = appLabel ?: "App icon",
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = appLabel ?: "Enter PIN to Unlock",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Dots area with shake effect applied
        Box(modifier = Modifier.offset { IntOffset(shakeX.value.roundToInt(), 0) }, contentAlignment = Alignment.Center) {
            PinDots(pinLength = pin.length, slots = maxDigits, dotSize = 18.dp, spacing = 14.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (!error.isNullOrEmpty()) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

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
}
