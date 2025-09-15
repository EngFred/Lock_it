package com.engfred.lockit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.engfred.lockit.presentation.ui.navigation.AppNavGraph
import com.engfred.lockit.presentation.ui.theme.LockItTheme
import com.engfred.lockit.presentation.viewmodel.MainViewModel
import com.engfred.lockit.service.AppLockerService
import com.engfred.lockit.utils.AccessibilityUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LockItTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = hiltViewModel()
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val isSetupComplete = viewModel.isSetupComplete()
                        startDestination = if (isSetupComplete) "app_list" else "setup"

                        if (isSetupComplete) {
                            val enabled = AccessibilityUtils.isAccessibilityServiceEnabled(this@MainActivity, AppLockerService::class.java)
                            if (!enabled) {
                                AccessibilityUtils.openAccessibilitySettings(this@MainActivity)
                            }
                            // If enabled, system will bind and call onServiceConnected() for the AccessibilityService.
                        }
                    }

                    if (startDestination != null) {
                        AppNavGraph(navController = navController, startDestination = startDestination!!)
                    } else {
                        Box(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
