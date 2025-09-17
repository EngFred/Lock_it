package com.engfred.lockit

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.engfred.lockit.utils.MyDeviceAdminReceiver
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.content.edit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val devicePolicyManager: DevicePolicyManager by lazy { getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val adminComponent: ComponentName by lazy { ComponentName(this, MyDeviceAdminReceiver::class.java) }

    private val prefs: SharedPreferences by lazy { getSharedPreferences("lockit_prefs",
        MODE_PRIVATE
    ) }

    private val enableAdminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Admin enabled successfully
        } else {
            // User canceled
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LockItTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = hiltViewModel()
                    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }
                    var showAdminDialog by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val isSetupComplete = viewModel.isSetupComplete()
                        startDestination = if (isSetupComplete) "app_list" else "setup"

                        if (isSetupComplete) {
                            // Check and prompt for accessibility
                            val enabled = AccessibilityUtils.isAccessibilityServiceEnabled(this@MainActivity, AppLockerService::class.java)
                            if (!enabled) {
                                AccessibilityUtils.openAccessibilitySettings(this@MainActivity)
                            }

                            // Check for device admin dialog (only if not declined before)
                            val adminDeclined = prefs.getBoolean("admin_prompt_declined", false)
                            if (!devicePolicyManager.isAdminActive(adminComponent) && !adminDeclined) {
                                showAdminDialog = true
                            }
                        }
                    }

                    if (showAdminDialog) {
                        AlertDialog(
                            onDismissRequest = { showAdminDialog = false },
                            title = { Text("Enable Device Admin Protection") },
                            text = { Text("Enabling Device Admin adds extra security by preventing unauthorized uninstall or data clearing of LockIt. This is optional but recommended for better protection.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showAdminDialog = false
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable to prevent unauthorized uninstall of LockIt")
                                    }
                                    enableAdminLauncher.launch(intent)
                                }) {
                                    Text("Enable")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showAdminDialog = false
                                    prefs.edit { putBoolean("admin_prompt_declined", true) }
                                }) {
                                    Text("Don't ask again")
                                }
                            }
                        )
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