package com.engfred.lockit.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun NumericKeypad(
    modifier: Modifier = Modifier,
    onNumber: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { n ->
                    KeyButton(number = n, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNumber(n)
                    })
                }
            }
        }

        // last row: [spacer] [0] [backspace]
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp)) { /* spacer to align */ }

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
                    .size(72.dp)
                    .semantics { contentDescription = "Backspace" }
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun KeyButton(number: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clickable { onClick() }
            .semantics { contentDescription = "Key $number" },
        shape = CircleShape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
