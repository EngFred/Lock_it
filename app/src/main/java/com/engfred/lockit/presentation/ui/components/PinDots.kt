package com.engfred.lockit.presentation.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * PinDots — visual representation of a PIN as dots (like phone lock screen).
 * - pinLength: how many digits the user has entered
 * - slots: total digits (6)
 * - dotSize / spacing for styling
 */
@Composable
fun PinDots(
    modifier: Modifier = Modifier,
    pinLength: Int,
    slots: Int = 6,
    dotSize: Dp = 16.dp,
    spacing: Dp = 12.dp,
    filledColor: Color = MaterialTheme.colorScheme.onSurface,
    emptyColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (i in 0 until slots) {
            val filled = i < pinLength
            // subtle animation for fill/unfill
            val transition = updateTransition(targetState = filled, label = "dotTransition$i")
            val scale = transition.animateFloat(label = "scale", transitionSpec = { tween(140) }) { if (it) 1.05f else 1f }.value
            val alpha = transition.animateFloat(label = "alpha", transitionSpec = { tween(140) }) { if (it) 1f else 0.5f }.value

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale)
                    .background(color = if (filled) filledColor else emptyColor, shape = CircleShape)
                    .alpha(alpha)
                    .semantics { contentDescription = if (filled) "filled dot" else "empty dot" }
            )
        }
    }
}
