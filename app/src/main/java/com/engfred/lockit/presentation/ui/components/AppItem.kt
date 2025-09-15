package com.engfred.lockit.presentation.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.engfred.lockit.R
import com.engfred.lockit.domain.model.AppInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItem(app: AppInfo, onToggle: suspend () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Try to load package icon synchronously (non-composable resources).
    // If it fails, fall back to a drawable resource converted to a BitmapPainter,
    // and as a last resort use a neutral ColorPainter to avoid invoking @Composable functions.
    val iconPainter: Painter = remember(app.packageName) {
        try {
            val pm = context.packageManager
            val drawable: Drawable = pm.getApplicationIcon(app.packageName)
            val bmp = drawable.toBitmap()
            BitmapPainter(bmp.asImageBitmap())
        } catch (e: Exception) {
            // fallback to drawable resource loaded via ContextCompat
            val fallback = ContextCompat.getDrawable(context, R.drawable.ic_lock_open)
            if (fallback != null) {
                val bmp = fallback.toBitmap()
                BitmapPainter(bmp.asImageBitmap())
            } else {
                // Last resort: neutral color painter (no composable call here)
                ColorPainter(colorScheme.onSurfaceVariant)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch { onToggle() }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (app.isLocked) "Locked" else "Unlocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = app.isLocked,
                onCheckedChange = { coroutineScope.launch { onToggle() } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
