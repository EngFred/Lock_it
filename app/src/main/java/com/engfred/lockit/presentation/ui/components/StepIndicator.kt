package com.engfred.lockit.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Full SetupScreen with a two-step permission indicator (Accessibility -> Usage Access).
 * Replace your existing SetupScreen.kt contents with this file.
 */

enum class StepStatus { GRANTED, CURRENT, PENDING }

@Composable
fun StepIndicator(
    steps: List<String>,
    statuses: List<StepStatus>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val status = statuses.getOrNull(index) ?: StepStatus.PENDING

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val circleSize = 36.dp
                val backgroundColor = when (status) {
                    StepStatus.GRANTED -> MaterialTheme.colorScheme.primary
                    StepStatus.CURRENT -> MaterialTheme.colorScheme.secondary
                    StepStatus.PENDING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }

                Surface(
                    modifier = Modifier.size(circleSize),
                    shape = CircleShape,
                    tonalElevation = if (status == StepStatus.PENDING) 0.dp else 4.dp,
                    color = backgroundColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (status) {
                            StepStatus.GRANTED -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "granted",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            else -> Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (status == StepStatus.PENDING)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(80.dp)
                )
            }

            if (index < steps.lastIndex) {
                val nextStatus = statuses.getOrNull(index + 1) ?: StepStatus.PENDING
                val connectorColor = when {
                    status == StepStatus.GRANTED && nextStatus == StepStatus.GRANTED -> MaterialTheme.colorScheme.primary
                    status == StepStatus.GRANTED && nextStatus != StepStatus.GRANTED -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }

                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(28.dp)
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically)
                        .background(connectorColor)
                )
            }
        }
    }
}