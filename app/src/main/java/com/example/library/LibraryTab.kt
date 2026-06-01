package com.example.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.player.MainViewModel
import java.io.File
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val videos by viewModel.libraryVideos.collectAsStateWithLifecycle()
    val folders by viewModel.foldersList.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var activeOptionVideo by remember { mutableStateOf<VideoItem?>(null) }
    var showDetailsDialog by remember { mutableStateOf<VideoItem?>(null) }
    var renameVideoDialog by remember { mutableStateOf<VideoItem?>(null) }
    var showPlaylistPickerFor by remember { mutableStateOf<VideoItem?>(null) }

    // Check & request storage permissions
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
            if (granted) {
                viewModel.scanLocalVideos()
            }
        }
    )

    // Trigger initial scan if permission granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.scanLocalVideos()
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (hasPermission) {
                        viewModel.scanLocalVideos()
                    } else {
                        launcher.launch(permissionToRequest)
                    }
                },
                icon = { Icon(Icons.Filled.Refresh, "Scan Media") },
                text = { Text("Rescan Storage") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("scan_floating_button")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // SEARCH BAR COMPONENT
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search videos...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Filled.Search, "Search Icon", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, "Clear query")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("library_search_input")
            )

            // FOLDER SELECTOR SLIDER
            Text(
                text = "Media Folders",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFolder == null,
                        onClick = { viewModel.setFolderFilter(null) },
                        label = { Text("All Media Files") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(folders) { folder ->
                    FilterChip(
                        selected = selectedFolder == folder,
                        onClick = { viewModel.setFolderFilter(folder) },
                        label = { Text(folder) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // GRID VS LIST TOGGLE AND TITLE ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedFolder == null) "All Discovered Videos" else "Folder: $selectedFolder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Pill-shaped layout selection toggle
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.isGridView = false },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (!viewModel.isGridView) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (!viewModel.isGridView) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.size(34.dp).testTag("list_view_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "List Mode",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.isGridView = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (viewModel.isGridView) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (viewModel.isGridView) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.size(34.dp).testTag("grid_view_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "Grid Mode",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // LIST RENDER AREA
            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VideoLibrary,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No videos discovered during scan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Grant storage permission or try refreshing. Demo online streams are available below.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                if (viewModel.isGridView) {
                    // ADAPTIVE GRID LAYOUT
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(videos) { video ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { viewModel.playVideo(video, videos) },
                                        onLongClick = { activeOptionVideo = video }
                                    )
                                    .testTag("video_card_${video.id}")
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Simulated high-fidelity beautiful thumbnail
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.77f) // 16:9 Cinema Aspect Ratio
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Visual subtle abstract player accents
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(100.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play Icon Overlay",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Storage or Network media source type badges
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .align(Alignment.TopStart)
                                                .background(
                                                    if (video.isLocal) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (video.isLocal) Icons.Filled.Folder else Icons.Filled.CloudQueue,
                                                    contentDescription = null,
                                                    tint = if (video.isLocal) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = if (video.isLocal) "OFFLINE" else "STREAM",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (video.isLocal) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        // Video Resolution Tag Badge
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .align(Alignment.BottomStart)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = video.resolution,
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Video Duration Badge
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .align(Alignment.BottomEnd)
                                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = video.formattedDuration,
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Content Text Info below thumbnail
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = video.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${video.folderName} • ${video.formattedSize}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { activeOptionVideo = video },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(start = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "Options",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // PRESTIGE GLOSSY LIST VIEW
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(videos) { video ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = { viewModel.playVideo(video, videos) },
                                        onLongClick = { activeOptionVideo = video }
                                    )
                                    .testTag("video_card_${video.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Stunning left rectangular movie container
                                    Box(
                                        modifier = Modifier
                                            .size(width = 96.dp, height = 60.dp)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                                    )
                                                ),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (video.isLocal) Icons.Filled.Movie else Icons.Filled.CloudQueue,
                                            contentDescription = "Video Type",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )

                                        // Small duration overlayed on mini-thumbnail bottom-right
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = video.formattedDuration,
                                                fontSize = 8.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${video.folderName} • ${video.formattedSize}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = video.resolution,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    IconButton(onClick = { activeOptionVideo = video }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ACTIONS OPTIONS BOTTOM SHEET MENU
    activeOptionVideo?.let { video ->
        ModalBottomSheet(
            onDismissRequest = { activeOptionVideo = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                DropdownMenuItem(
                    text = { Text("Play Video") },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, "Play") },
                    onClick = {
                        viewModel.playVideo(video, videos)
                        activeOptionVideo = null
                    }
                )

                DropdownMenuItem(
                    text = { Text("Toggle Favorite") },
                    leadingIcon = { Icon(Icons.Filled.Star, "Stars") },
                    onClick = {
                        viewModel.toggleFavorite(video)
                        activeOptionVideo = null
                    }
                )

                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    leadingIcon = { Icon(Icons.Filled.PlaylistAdd, "Add to Playlist") },
                    onClick = {
                        showPlaylistPickerFor = video
                        activeOptionVideo = null
                    }
                )

                DropdownMenuItem(
                    text = { Text("Hide in Private Vault") },
                    leadingIcon = { Icon(Icons.Filled.Lock, "Hide") },
                    onClick = {
                        viewModel.moveVideoToVault(video)
                        activeOptionVideo = null
                    }
                )

                DropdownMenuItem(
                    text = { Text("Rename Virtual") },
                    leadingIcon = { Icon(Icons.Filled.Edit, "Rename") },
                    onClick = {
                        renameVideoDialog = video
                        activeOptionVideo = null
                    }
                )

                DropdownMenuItem(
                    text = { Text("Video Details & Info") },
                    leadingIcon = { Icon(Icons.Filled.Info, "Details") },
                    onClick = {
                        showDetailsDialog = video
                        activeOptionVideo = null
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete Virtual Card", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Filled.Delete, "Delete", tint = Color.Red) },
                    onClick = {
                        viewModel.simulateFileDelete(video.path)
                        activeOptionVideo = null
                    }
                )
            }
        }
    }

    // DETAILED DIALOG POPUP
    showDetailsDialog?.let { video ->
        AlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = { Text(video.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "📁 Folder: ${video.folderName}", fontSize = 13.sp)
                    Text(text = "📏 Codec / Res: ${video.resolution}", fontSize = 13.sp)
                    Text(text = "⏱️ Duration: ${video.formattedDuration}", fontSize = 13.sp)
                    Text(text = "💾 File Size: ${video.formattedSize}", fontSize = 13.sp)
                    Text(text = "🔗 Media Path: ${video.path}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    // RENAME DIALOG POPUP
    renameVideoDialog?.let { video ->
        var renameText by remember { mutableStateOf(video.title) }
        AlertDialog(
            onDismissRequest = { renameVideoDialog = null },
            title = { Text("Rename Video") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Video Title") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotEmpty()) {
                        viewModel.simulateFileRename(video.path, renameText)
                    }
                    renameVideoDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameVideoDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PLAYLIST SELECTOR PICKER DIALOG
    showPlaylistPickerFor?.let { video ->
        val playlistsList by viewModel.playlists.collectAsStateWithLifecycle()
        var newPlaylistName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showPlaylistPickerFor = null },
            title = { Text("Add Video to Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (playlistsList.isEmpty()) {
                        Text("No playlists created yet. Create one below first:")
                    } else {
                        Text("Select a Playlist:")
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 180.dp)
                                .fillMaxWidth()
                        ) {
                            items(playlistsList) { playlist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.addVideoToPlaylist(playlist.id, video)
                                            showPlaylistPickerFor = null
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = playlist.name,
                                        modifier = Modifier.padding(12.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Create New Playlist") },
                        placeholder = { Text("e.g. Action Movies") }
                    )

                    Button(
                        onClick = {
                            if (newPlaylistName.isNotEmpty()) {
                                viewModel.createPlaylist(newPlaylistName)
                                newPlaylistName = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("New Playlist")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPickerFor = null }) {
                    Text("Done")
                }
            }
        )
    }
}
