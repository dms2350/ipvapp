package com.dms2350.iptvapp.presentation.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.videolan.libvlc.util.VLCVideoLayout
import com.dms2350.iptvapp.domain.model.Channel

@Composable
fun PlayerScreen(
    channel: Channel,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(channel) {
        viewModel.setCurrentChannel(channel)
        viewModel.playChannel(channel)
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                when {
                    (keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.MediaPrevious) && keyEvent.type == KeyEventType.KeyDown -> {
                        viewModel.previousChannel()
                        true
                    }
                    (keyEvent.key == Key.DirectionRight || keyEvent.key == Key.MediaNext) && keyEvent.type == KeyEventType.KeyDown -> {
                        viewModel.nextChannel()
                        true
                    }
                    keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyDown -> {
                        onBackClick()
                        true
                    }
                    keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyDown -> {
                        viewModel.toggleChannelInfo()
                        true
                    }
                    keyEvent.key == Key.Spacebar && keyEvent.type == KeyEventType.KeyDown -> {
                        viewModel.pauseResume()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else if (uiState.error != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = Color.White
                )
                Button(
                    onClick = { viewModel.forceReconnect() }
                ) {
                    Text("Reconectar")
                }
            }
        } else {
            AndroidView(
                factory = { context ->
                    VLCVideoLayout(context).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        keepScreenOn = true
                    }
                },
                update = { vlcLayout ->
                    try {
                        viewModel.mediaPlayer.attachViews(vlcLayout, null, true, false)
                    } catch (e: Exception) {
                        println("VLC: Error: ${e.message}")
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        viewModel.toggleChannelInfo()
                    }
            )
        }
        
        // Overlay de información del canal
        uiState.currentChannel?.let { channel ->
            if (uiState.showChannelInfo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = channel.name,
                            color = Color.White
                        )
                        if (channel.description != null) {
                            Text(
                                text = channel.description,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
        
        // Controles flotantes
        if (uiState.showControls) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = { viewModel.previousChannel() }) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = { viewModel.pauseResume() }) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pausa/Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = { viewModel.nextChannel() }) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Siguiente",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
