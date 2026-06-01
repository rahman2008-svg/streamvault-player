package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.about.AboutTab
import com.example.library.LibraryTab
import com.example.player.MainViewModel
import com.example.player.StatsTab
import com.example.player.VideoPlayerView
import com.example.playlist.PlaylistsTab
import com.example.settings.SettingsTab
import com.example.streaming.StreamingTab
import com.example.ui.theme.MyApplicationTheme
import com.example.vault.VaultTab

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme(themeMode = viewModel.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val activePlayerVideo = viewModel.activeVideo

                    if (activePlayerVideo != null) {
                        // EXOPLAYER HIGH-RISK FULL SCREEN LAYER
                        VideoPlayerView(
                            video = activePlayerVideo,
                            viewModel = viewModel,
                            onClose = { viewModel.closePlayer() }
                        )
                    } else {
                        // PRIMARY TAB CONTAINER RENDER VIEW
                        MainAppContent(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val activeTab = viewModel.currentTab

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "StreamVault",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Stats Button
                    IconButton(
                        onClick = { viewModel.currentTab = "stats" },
                        modifier = Modifier.testTag("top_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = "Watch Stats",
                            tint = if (activeTab == "stats") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // About Profile Button
                    IconButton(
                        onClick = { viewModel.currentTab = "about" },
                        modifier = Modifier.testTag("top_about_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "About Prince",
                            tint = if (activeTab == "about") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Standard Navigation Bar (Supports Library, Playlists, Stream channels, Vault settings)
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                val navItems = listOf(
                    Triple("library", "Gallery", Icons.Filled.VideoLibrary),
                    Triple("playlists", "Playlists", Icons.Filled.QueueMusic),
                    Triple("streaming", "Streams", Icons.Filled.Tv),
                    Triple("vault", "Locked Vault", Icons.Filled.Lock),
                    Triple("settings", "Preferences", Icons.Filled.Settings)
                )

                navItems.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = activeTab == route,
                        onClick = { viewModel.currentTab = route },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(icon, contentDescription = label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "library" -> LibraryTab(viewModel = viewModel)
                "playlists" -> PlaylistsTab(viewModel = viewModel)
                "streaming" -> StreamingTab(viewModel = viewModel)
                "vault" -> VaultTab(viewModel = viewModel)
                "stats" -> StatsTab(viewModel = viewModel)
                "settings" -> SettingsTab(viewModel = viewModel)
                "about" -> AboutTab()
                else -> LibraryTab(viewModel = viewModel)
            }
        }
    }
}
