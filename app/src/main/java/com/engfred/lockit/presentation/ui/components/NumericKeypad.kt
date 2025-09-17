package com.engfred.lockit.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace

@Composable
fun NumericKeypad(
    modifier: Modifier = Modifier,
    onNumber: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp), // Increased spacing for premium feel
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { // Wider spacing for touch-friendliness
                row.forEach { n ->
                    KeyButton(number = n, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNumber(n)
                    })
                }
            }
        }

        // Last row: [spacer] [0] [backspace]
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp)) { /* Spacer to align */ }

            KeyButton(number = 0, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNumber(0)
            })

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBackspace()
                },
                modifier = Modifier
                    .size(80.dp) // Larger for better touch
                    .semantics { contentDescription = "Backspace" }
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // Softer tint for professionalism
                )
            }
        }
    }
}

@Composable
private fun KeyButton(number: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "keyScale")

    Surface(
        modifier = Modifier
            .size(70.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null // Custom indication via scale
            ) { onClick() }
            .semantics { contentDescription = "Key $number" },
        shape = CircleShape,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}