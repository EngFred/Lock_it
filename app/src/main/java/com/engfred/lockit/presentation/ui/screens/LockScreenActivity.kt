package com.engfred.lockit.presentation.ui.screens

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.lockit.presentation.ui.theme.LockItTheme
import com.engfred.lockit.presentation.viewmodel.LockViewModel
import com.engfred.lockit.service.UnlockEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import javax.inject.Inject

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
                androidx.compose.material3.Surface {
                    // pass unlockEventBus to composable
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
fun LockScreen(viewModel: com.engfred.lockit.presentation.viewmodel.LockViewModel, unlockEventBus: UnlockEventBus) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val lockedPackage = (context as Activity).intent.getStringExtra("locked_package")

    // Prevent system back (for extra safety within compose)
    BackHandler(enabled = true) { /* no-op */ }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter PIN to Unlock", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { value -> if (value.length <= 6 && value.all { it.isDigit() }) pin = value },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                if (viewModel.validatePin(pin)) {
                    if (!lockedPackage.isNullOrEmpty()) {
                        // non-suspending emit (fire-and-forget)
                        unlockEventBus.emitUnlockedPackage(lockedPackage)
                    }
                    (context as Activity).finish()
                } else {
                    error = "Incorrect PIN"
                    pin = ""
                }
            }
        }) {
            Text("Unlock")
        }
    }
}

