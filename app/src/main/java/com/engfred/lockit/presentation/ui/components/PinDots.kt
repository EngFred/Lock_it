package com.engfred.lockit.presentation.ui.components

/**
 * PinDots — visual representation of a PIN as dots (like phone lock screen).
 * - pinLength: how many digits the user has entered
 * - slots: total digits (6)
 * - dotSize / spacing for styling
 */
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PinDots(
    modifier: Modifier = Modifier,
    pinLength: Int,
    slots: Int = 6,
    dotSize: Dp = 18.dp,
    spacing: Dp = 16.dp,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    emptyBorderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, // border for empty
    emptyBorderWidth: Dp = 1.5.dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (i in 0 until slots) {
            val filled = i < pinLength
            val transition = updateTransition(targetState = filled, label = "dotTransition$i")
            val scale = transition.animateFloat(label = "scale", transitionSpec = { tween(180) }) {
                if (it) 1.08f else 1f
            }.value

            // Animate fill color quickly (filled -> primary, empty -> transparent)
            val color = transition.animateColor(label = "color", transitionSpec = { tween(180) }) {
                if (it) filledColor else Color.Transparent
            }.value

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale)
                    .shadow(elevation = if (filled) 2.dp else 0.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(color = color, shape = CircleShape)
                    // show border only when empty
                    .then(
                        if (!filled) Modifier.border(width = emptyBorderWidth, color = emptyBorderColor, shape = CircleShape)
                        else Modifier
                    )
                    .semantics { contentDescription = if (filled) "filled dot" else "empty dot" }
            )
        }
    }
}
