package com.engfred.lockit.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.engfred.lockit.R
import com.engfred.lockit.domain.model.AppInfo
import com.engfred.lockit.presentation.ui.components.AppList
import com.engfred.lockit.presentation.ui.components.SearchField
import com.engfred.lockit.presentation.viewmodel.AppListViewModel
import com.engfred.lockit.presentation.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Search state
    var query by rememberSaveable { mutableStateOf("") }

    // Default sort: show locked apps first (best UX for app locker)
    var sortOrder by rememberSaveable { mutableStateOf("Newest") }

    // Filter and sort based on data from UiState.Success
    val (filtered, sortedApps) = remember(uiState, query, sortOrder) {
        when (uiState) {
            is UiState.Success -> {
                val apps = (uiState as UiState.Success<List<AppInfo>>).data
                val filtered = if (query.isBlank()) apps else {
                    val q = query.trim().lowercase()
                    apps.filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
                }
                val sorted = when (sortOrder) {
                    "LockedFirst" -> filtered.sortedWith(compareByDescending<AppInfo> { it.isLocked }.thenByDescending { it.installTime })
                    "A-Z" -> filtered.sortedBy { it.name.lowercase() }
                    "Z-A" -> filtered.sortedByDescending { it.name.lowercase() }
                    "Newest" -> filtered.sortedByDescending { it.installTime }
                    "Oldest" -> filtered.sortedBy { it.installTime }
                    else -> filtered
                }
                Pair(filtered, sorted)
            }
            else -> Pair(emptyList(), emptyList())
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
            TopAppBar(
                title = {
                    Column {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { navController?.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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

                when (uiState) {
                    is UiState.Loading -> {
                        // Loading state with indicator
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
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading apps...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (sortedApps.isEmpty()) {
                            // True empty state (no apps or no matches after loading)
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
                                        text = if (filtered.isEmpty()) "No apps match your search" else "No apps available",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (filtered.isEmpty()) "Try a different search or clear the filter" else "Apps will appear here once loaded",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // App list
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
                    is UiState.Error -> {
                        // Error state (rare, but handled)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (uiState as UiState.Error).message,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}