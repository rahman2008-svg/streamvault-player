package com.example.streaming

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.library.VideoItem
import com.example.player.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingTab(viewModel: MainViewModel) {
    val streams by viewModel.iptvStreams.collectAsStateWithLifecycle()
    
    var directUrl by remember { mutableStateOf("") }
    var directTitle by remember { mutableStateOf("") }
    
    var showAddStreamDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // QUICK PLAY SECTION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚡ Instant Link Stream",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = directTitle,
                    onValueChange = { directTitle = it },
                    label = { Text("Stream Title") },
                    placeholder = { Text("e.g. Live Sports Track") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = directUrl,
                    onValueChange = { directUrl = it },
                    label = { Text("Direct HTTP / M3U8 URL") },
                    placeholder = { Text("https://example.com/live.m3u8") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        if (directUrl.isNotEmpty()) {
                            val title = directTitle.ifEmpty { "Network Stream Live" }
                            val tempVideo = VideoItem(
                                id = "direct_stream_${System.currentTimeMillis()}",
                                title = title,
                                path = directUrl,
                                isLocal = false,
                                folderName = "Stream Player"
                            )
                            viewModel.playVideo(tempVideo)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, "Start stream")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Instant Broadcast Play", fontWeight = FontWeight.Bold)
                }
            }
        }

        // CHANNELS BROWSER TITLE
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My IPTV Streams",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Button(
                onClick = { showAddStreamDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, "Add channel", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Stream", fontSize = 12.sp)
            }
        }

        // STREAM DATA LIST BROWSER
        if (streams.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved IPTV streams available.\nCreate some bookmarks above to persist them.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(streams) { stream ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val video = VideoItem(
                                    id = "iptv_${stream.streamUrl.hashCode()}",
                                    title = stream.name,
                                    path = stream.streamUrl,
                                    isLocal = false,
                                    folderName = stream.groupName
                                )
                                viewModel.playVideo(video)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tv,
                                    contentDescription = "IPTV icon",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stream.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = stream.groupName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            IconButton(onClick = { viewModel.deleteIPTVStream(stream.streamUrl) }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove Stream",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ADD PERSISTENT CHANNEL DIALOG
    if (showAddStreamDialog) {
        var chanName by remember { mutableStateOf("") }
        var chanUrl by remember { mutableStateOf("") }
        var chanGroup by remember { mutableStateOf("User Streams") }

        AlertDialog(
            onDismissRequest = { showAddStreamDialog = false },
            title = { Text("Add IPTV Stream Bookmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chanName,
                        onValueChange = { chanName = it },
                        label = { Text("Channel Name") }
                    )
                    OutlinedTextField(
                        value = chanUrl,
                        onValueChange = { chanUrl = it },
                        label = { Text("Channel Stream URL") }
                    )
                    OutlinedTextField(
                        value = chanGroup,
                        onValueChange = { chanGroup = it },
                        label = { Text("Group Category") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (chanName.isNotEmpty() && chanUrl.isNotEmpty()) {
                        viewModel.addIPTVStream(chanName, chanUrl, chanGroup)
                    }
                    showAddStreamDialog = false
                    chanName = ""
                    chanUrl = ""
                    chanGroup = "User Streams"
                }) {
                    Text("Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStreamDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
