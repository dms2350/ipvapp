package com.dms2350.iptvapp.presentation.ui.channels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.presentation.ui.components.ChannelItem
import com.dms2350.iptvapp.utils.DeviceUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    onChannelClick: (Channel) -> Unit,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isTV = DeviceUtils.isTV(context)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!isTV) {
            TopAppBar(
                title = { Text("Canales IPTV") },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshChannels() }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        } else {
            // Botón de refresh para TV
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Canales IPTV",
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(
                    onClick = { viewModel.refreshChannels() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualizar")
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshChannels() }) {
                        Text("Reintentar")
                    }
                }
            }
            
            else -> {
                val groupedChannels = viewModel.getChannelsGroupedByCategory()
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(if (isTV) 32.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isTV) 12.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isTV) 12.dp else 8.dp)
                ) {
                    groupedChannels.forEach { (categoryName, channels) ->
                        // Encabezado de categoría
                        item(span = { GridItemSpan(3) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        // Canales de esta categoría
                        items(channels) { channel ->
                            ChannelItem(
                                channel = channel,
                                onClick = { onChannelClick(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}
