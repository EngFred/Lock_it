package com.engfred.lockit.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engfred.lockit.domain.model.AppInfo

/**
 * Reusable app list. It calls AppItem for each entry and draws a divider between items,
 * but not after the last item.
 */
@Composable
fun AppList(
    apps: List<AppInfo>,
    onToggle: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null
) {
    LazyColumn(
        state = listState ?: androidx.compose.foundation.lazy.LazyListState(),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(
            items = apps,
            key = { _, item -> item.packageName }
        ) { index, app ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.99f),
                exit = fadeOut() + scaleOut(targetScale = 0.99f)
            ) {
                AppItem(
                    app = app,
                    onToggle = onToggle
                )
            }

            // draw divider for all but last item
            if (index < apps.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
