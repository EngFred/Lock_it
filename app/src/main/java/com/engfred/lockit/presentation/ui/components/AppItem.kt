package com.engfred.lockit.presentation.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.lockit.R
import com.engfred.lockit.domain.model.AppInfo
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.graphics.drawable.toBitmap

/**
 * Plain list item: icon (no container), vertical divider between icon & text, text + switch.
 * onToggle is NOT suspend — caller should handle coroutine launching.
 */
@Composable
fun AppItem(app: AppInfo, onToggle: (AppInfo) -> Unit) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val iconPainter: Painter = remember(app.packageName, app.icon) {
        try {
            app.icon?.let { bytes ->
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) BitmapPainter(bmp.asImageBitmap()) else null
            } ?: run {
                val fallback = ContextCompat.getDrawable(context, R.drawable.ic_lock_open)
                fallback?.let { bmp -> BitmapPainter(bmp.toBitmap().asImageBitmap()) }
            } ?: ColorPainter(colorScheme.onSurfaceVariant)
        } catch (e: Exception) {
            ColorPainter(colorScheme.onSurfaceVariant)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(app) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon (no background container)
        Image(
            painter = iconPainter,
            contentDescription = "${app.name} icon",
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
        )

        // Vertical divider between icon and content
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .wrapContentHeight()
        ) {
            // simple color using surfaceVariant to look subtle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.surfaceVariant,
                shape = RoundedCornerShape(0.dp)
            ) {}
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (app.isLocked) "Locked" else "Unlocked",
                style = MaterialTheme.typography.bodySmall,
                color = if (app.isLocked) colorScheme.primary else colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Toggle
        Switch(
            checked = app.isLocked,
            onCheckedChange = { onToggle(app) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorScheme.primary,
                checkedTrackColor = colorScheme.primaryContainer,
                uncheckedThumbColor = colorScheme.onSurfaceVariant,
                uncheckedTrackColor = colorScheme.surfaceVariant
            )
        )
    }
}
