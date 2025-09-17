package com.engfred.lockit.presentation.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.engfred.lockit.presentation.ui.components.NumericKeypad
import com.engfred.lockit.presentation.ui.components.PinDots
import com.engfred.lockit.presentation.viewmodel.ChangePinViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(navController: NavController? = null, viewModel: ChangePinViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var oldPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmNewPin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val maxDigits = 6

    // active step: 0 = enter old, 1 = enter new, 2 = confirm new
    var activeField by rememberSaveable { mutableIntStateOf(0) }

    // When old/new is fully entered, switch automatically
    LaunchedEffect(oldPin) {
        if (oldPin.length == maxDigits && activeField == 0) {
            coroutineScope.launch {
                val valid = viewModel.validateOldPin(oldPin)
                if (valid) {
                    error = null
                    newPin = ""
                    activeField = 1
                } else {
                    error = "Incorrect old PIN"
                    oldPin = ""
                }
            }
        }
    }

    LaunchedEffect(newPin) {
        if (newPin.length == maxDigits && activeField == 1) {
            error = null
            confirmNewPin = ""
            activeField = 2
        }
    }

    // Show toast for error
    LaunchedEffect(error) {
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            error = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change PIN") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .verticalScroll(rememberScrollState())
        ) {
            // Centered Card with single-field PIN flow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    // CROSSFADE between fields
                    Crossfade(targetState = activeField) { field ->
                        when (field) {
                            0 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Enter your Old PIN", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PinDots(pinLength = oldPin.length, slots = maxDigits)
                                }
                            }
                            1 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Enter a new 6-Digit PIN. Don’t use easily guessed numbers but ensure you can remember it.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PinDots(pinLength = newPin.length, slots = maxDigits)
                                }
                            }
                            2 -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Confirm your new 6-Digit PIN", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PinDots(pinLength = confirmNewPin.length, slots = maxDigits)
                                }
                            }
                        }
                    }

                    // Back button when not on first step
                    AnimatedVisibility(visible = activeField > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = {
                                error = null
                                if (activeField == 1) {
                                    activeField = 0
                                    if (oldPin.isNotEmpty()) oldPin = oldPin.dropLast(1)
                                } else if (activeField == 2) {
                                    activeField = 1
                                    if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
                                }
                            }) {
                                Text("Back")
                            }
                        }
                    }

                    NumericKeypad(
                        onNumber = { d ->
                            when (activeField) {
                                0 -> if (oldPin.length < maxDigits) oldPin += d.toString()
                                1 -> if (newPin.length < maxDigits) newPin += d.toString()
                                2 -> if (confirmNewPin.length < maxDigits) confirmNewPin += d.toString()
                            }
                        },
                        onBackspace = {
                            when (activeField) {
                                0 -> if (oldPin.isNotEmpty()) oldPin = oldPin.dropLast(1)
                                1 -> if (newPin.isNotEmpty()) newPin = newPin.dropLast(1) else activeField = 0
                                2 -> if (confirmNewPin.isNotEmpty()) confirmNewPin = confirmNewPin.dropLast(1) else activeField = 1
                            }
                        },
                        onSubmit = null
                    )

                    // Show Update PIN only when confirming
                    AnimatedVisibility(visible = activeField == 2) {
                        FilledTonalButton(
                            onClick = {
                                error = null
                                when {
                                    newPin.length != maxDigits -> error = "New PIN must be 6 digits"
                                    confirmNewPin.length != maxDigits -> error = "Confirm PIN must be 6 digits"
                                    newPin != confirmNewPin -> error = "PINs don't match"
                                    else -> {
                                        coroutineScope.launch {
                                            val success = viewModel.updatePin(newPin)
                                            if (success) {
                                                Toast.makeText(context, "PIN updated successfully", Toast.LENGTH_SHORT).show()
                                                navController?.popBackStack()
                                            } else {
                                                error = "Error updating PIN"
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
                            Text("Update PIN")
                        }
                    }
                }
            }
        }
    }
}