package com.engfred.lockit.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.lockit.R
import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.presentation.ui.components.AppList
import com.engfred.lockit.presentation.ui.components.SearchField
import com.engfred.lockit.presentation.viewmodel.AppListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(viewModel: AppListViewModel = hiltViewModel()) {
    val apps = viewModel.apps.collectAsState().value
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Search state
    var query by remember { mutableStateOf("") }

    // Default sort: show locked apps first (best UX for app locker)
    var sortOrder by remember { mutableStateOf("Newest") }

    // Filter
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else {
            val q = query.trim().lowercase()
            apps.filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }

    // Sort: locked-first default; other options available
    val sortedApps = remember(filtered, sortOrder) {
        when (sortOrder) {
            "LockedFirst" -> filtered.sortedWith(compareByDescending<AppInfo> { it.isLocked }
                .thenByDescending { it.installTime })
            "A-Z" -> filtered.sortedBy { it.name.lowercase() }
            "Z-A" -> filtered.sortedByDescending { it.name.lowercase() }
            "Newest" -> filtered.sortedByDescending { it.installTime }
            "Oldest" -> filtered.sortedBy { it.installTime }
            else -> filtered
        }
    }

    LaunchedEffect(sortOrder) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }

    // Gradient
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Select Apps to Lock",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Protect your privacy with a secure PIN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Newest (date added)")
                                    Spacer(Modifier.width(5.dp))
                                    if (sortOrder == "Newest") Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                }
                            },
                            onClick = { sortOrder = "Newest"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Oldest (date added)")
                                    Spacer(Modifier.width(5.dp))
                                    if (sortOrder == "Oldest") Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                }
                            },
                            onClick = { sortOrder = "Oldest"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Locked first")
                                    Spacer(Modifier.width(5.dp))
                                    if (sortOrder == "LockedFirst") Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                }
                            },
                            onClick = { sortOrder = "LockedFirst"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("A to Z")
                                    Spacer(Modifier.width(5.dp))
                                    if (sortOrder == "A-Z") Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                }
                            },
                            onClick = { sortOrder = "A-Z"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Z to A")
                                    Spacer(Modifier.width(5.dp))
                                    if (sortOrder == "Z-A") Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                }
                            },
                            onClick = { sortOrder = "Z-A"; expanded = false }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(gradientBrush)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search field (rounded corners)
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                if (sortedApps.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock_open),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(bottom = 16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (apps.isEmpty()) "No apps available" else "No apps match your search",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (apps.isEmpty()) "Apps will appear here once loaded" else "Try a different search or clear the filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // App list (modular component)
                    AppList(
                        apps = sortedApps,
                        onToggle = { app -> coroutineScope.launch { viewModel.toggleLock(app) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        listState = listState
                    )
                }
            }
        }
    }
}
