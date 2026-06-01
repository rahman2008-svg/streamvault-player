package com.example.player

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.FavoriteVideo
import com.example.database.IPTVStream
import com.example.database.Playlist
import com.example.database.PlaylistItem
import com.example.database.VaultVideo
import com.example.database.VideoRepository
import com.example.database.WatchHistory
import com.example.library.VideoItem
import com.example.library.VideoScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    private val context = application.applicationContext

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VideoRepository(database.videoDao)
        
        // Seed IPTV default streams if database is empty
        viewModelScope.launch {
            repository.iptvStreams.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultStreams()
                }
            }
        }
    }

    // ==========================================
    // APP NAVIGATION & THEME STATES
    // ==========================================
    var currentTab by mutableStateOf("library") // library, playlists, streams, vault, stats, settings, about
    var themeMode by mutableIntStateOf(1)      // 0 = Light, 1 = Dark, 2 = AMOLED
    var playbackSettingAutoResume by mutableStateOf(true)
    var swipeGesturesEnabled by mutableStateOf(true)
    var isGridView by mutableStateOf(true) // Default to beautiful grid view

    // ==========================================
    // AUDIO & VIDEO GALLERY STATES
    // ==========================================
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val scannedVideos: StateFlow<List<VideoItem>> = _scannedVideos.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    // Combined screen states for Library
    val libraryVideos = combine(scannedVideos, searchQuery, selectedFolder) { videos, query, folder ->
        var list = if (videos.isEmpty()) VideoItem.DEFAULT_STREAMS else videos
        
        if (folder != null) {
            list = list.filter { it.folderName == folder }
        }
        if (query.isNotEmpty()) {
            list = list.filter { it.title.contains(query, ignoreCase = true) }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoItem.DEFAULT_STREAMS)

    // Folders derived state
    val foldersList = scannedVideos.combine(_scannedVideos) { _, _ ->
        val list = if (_scannedVideos.value.isEmpty()) VideoItem.DEFAULT_STREAMS else _scannedVideos.value
        list.map { it.folderName }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Blender Animation", "CGI VFX Test", "Demos & Samples"))

    // ==========================================
    // PERSISTENT DATA FLOWS
    // ==========================================
    val watchHistory: StateFlow<List<WatchHistory>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteVideo>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultVideos: StateFlow<List<VaultVideo>> = repository.vaultVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStats = repository.recentStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val iptvStreams = repository.iptvStreams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // ACTIVE PLAYBACK STATES
    // ==========================================
    var activeVideo by mutableStateOf<VideoItem?>(null)
    var isPlaying by mutableStateOf(false)
    var playbackSpeed by mutableFloatStateOf(1.0f)
    var isTouchLocked by mutableStateOf(false)
    var fitMode by mutableIntStateOf(0)        // 0 = Fit, 1 = Fill, 2 = Stretch, 3 = Zoom
    var isAudioOnlyMode by mutableStateOf(false) // Background audio play
    
    var currentQueue = mutableListOf<VideoItem>()
    var currentQueueIndex by mutableIntStateOf(0)

    // Gestures in player overlays
    var brightnessOverlayValue by mutableFloatStateOf(-1f) // -1 means hidden
    var volumeOverlayValue by mutableFloatStateOf(-1f)    // -1 means hidden

    // ==========================================
    // PRIVATE VAULT STATES
    // ==========================================
    var vaultPasscode by mutableStateOf("1234") // Default passcode
    var isVaultUnlocked by mutableStateOf(false)
    var vaultErrorMessage by mutableStateOf("")

    // List of keys/paths of hidden videos
    var hiddenVideoPaths = mutableStateOf<Set<String>>(emptySet())

    // ==========================================
    // CORE METHODS
    // ==========================================

    fun scanLocalVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = VideoScanner.scanVideos(context)
            // Filter out files that are hidden in the vault
            val filtered = list.filter { !hiddenVideoPaths.value.contains(it.path) }
            _scannedVideos.value = filtered
        }
    }

    fun setFolderFilter(folder: String?) {
        _selectedFolder.value = folder
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Playback Operations
    fun playVideo(video: VideoItem, queue: List<VideoItem> = listOf(video)) {
        activeVideo = video
        currentQueue = queue.toMutableList()
        currentQueueIndex = currentQueue.indexOf(video).coerceAtLeast(0)
        isPlaying = true
        isAudioOnlyMode = false
        
        // Log watch history
        addVideoToHistory(video)
    }

    fun playNext() {
        if (currentQueue.isNotEmpty() && currentQueueIndex < currentQueue.size - 1) {
            currentQueueIndex++
            activeVideo = currentQueue[currentQueueIndex]
            isPlaying = true
            addVideoToHistory(activeVideo!!)
        }
    }

    fun playPrev() {
        if (currentQueue.isNotEmpty() && currentQueueIndex > 0) {
            currentQueueIndex--
            activeVideo = currentQueue[currentQueueIndex]
            isPlaying = true
            addVideoToHistory(activeVideo!!)
        }
    }

    fun closePlayer() {
        activeVideo = null
        isPlaying = false
    }

    // Database Actions: History
    private fun addVideoToHistory(video: VideoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertHistory(
                WatchHistory(
                    videoUrl = video.path,
                    videoTitle = video.title,
                    duration = video.duration,
                    playbackPosition = 0L,
                    lastWatchedTime = System.currentTimeMillis(),
                    isLocal = video.isLocal,
                    folderName = video.folderName
                )
            )
            // Record 1 minute watched in stats for demonstration
            repository.recordWatchTime(1)
        }
    }

    fun updateHistoryProgress(url: String, progress: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val historyList = watchHistory.value
            val match = historyList.find { it.videoUrl == url }
            if (match != null) {
                repository.insertHistory(match.copy(playbackPosition = progress, lastWatchedTime = System.currentTimeMillis()))
            }
        }
    }

    fun deleteHistoryItem(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistory(url)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    // Database Actions: Favorites
    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = repository.isFavorite(video.path)
            if (exists) {
                repository.deleteFavorite(video.path)
            } else {
                repository.insertFavorite(
                    FavoriteVideo(
                        videoUrl = video.path,
                        videoTitle = video.title,
                        duration = video.duration,
                        isLocal = video.isLocal
                    )
                )
            }
        }
    }

    fun isFavorite(url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.isFavorite(url)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    // Playlist System
    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotEmpty()) {
                repository.createPlaylist(name)
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlistId)
        }
    }

    fun getPlaylistItems(playlistId: Long): StateFlow<List<PlaylistItem>> {
        return repository.getPlaylistItems(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addPlaylistItem(
                PlaylistItem(
                    playlistId = playlistId,
                    videoUrl = video.path,
                    videoTitle = video.title,
                    duration = video.duration,
                    isLocal = video.isLocal
                )
            )
        }
    }

    fun removeVideoFromPlaylist(itemId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylistItem(itemId)
        }
    }

    // Vault Private System
    fun unlockVault(pin: String): Boolean {
        return if (pin == vaultPasscode) {
            isVaultUnlocked = true
            vaultErrorMessage = ""
            true
        } else {
            isVaultUnlocked = false
            vaultErrorMessage = "Invalid Passcode!"
            false
        }
    }

    fun lockVault() {
        isVaultUnlocked = false
    }

    fun changeVaultPasscode(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            vaultPasscode = newPin
            isVaultUnlocked = false
            vaultErrorMessage = ""
        }
    }

    fun moveVideoToVault(video: VideoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Add to database vault table
            repository.insertVaultVideo(
                VaultVideo(
                    videoPath = video.path,
                    videoTitle = video.title,
                    duration = video.duration,
                    passcode = vaultPasscode
                )
            )
            // Save to absolute hidden list
            val currentHidden = hiddenVideoPaths.value.toMutableSet()
            currentHidden.add(video.path)
            hiddenVideoPaths.value = currentHidden
            
            // Re-scan gallery to remove it
            scanLocalVideos()
        }
    }

    fun restoreVideoFromVault(vaultVideo: VaultVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVaultVideo(vaultVideo.videoPath)
            
            val currentHidden = hiddenVideoPaths.value.toMutableSet()
            currentHidden.remove(vaultVideo.videoPath)
            hiddenVideoPaths.value = currentHidden
            
            // Re-scan gallery
            scanLocalVideos()
        }
    }

    // IPTV / Stream Manager
    fun addIPTVStream(name: String, url: String, group: String = "User Streams") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertIPTVStream(
                IPTVStream(
                    streamUrl = url,
                    name = name,
                    groupName = group
                )
            )
        }
    }

    fun deleteIPTVStream(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteIPTVStream(url)
        }
    }

    private suspend fun seedDefaultStreams() {
        val list = listOf(
            IPTVStream("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8", "Tears of Steel (HLS IPTV Live)", "Global OTT"),
            IPTVStream("https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8", "BipBop Adaptive Live Stream", "Test IPTV Channels"),
            IPTVStream("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4", "Crypto Bullrun Live Stock Video", "FinTech Streams"),
            IPTVStream("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubtitlesTest.mp4", "International Subtitles Test Stream", "Tech Test Streams")
        )
        list.forEach { repository.insertIPTVStream(it) }
    }

    // Simulators / Tools
    fun simulateFileRename(oldPath: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update scanned videos if present matching path
            val updated = _scannedVideos.value.map {
                if (it.path == oldPath) it.copy(title = newTitle) else it
            }
            _scannedVideos.value = updated
        }
    }

    fun simulateFileDelete(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _scannedVideos.value.filter { it.path != path }
            _scannedVideos.value = updated
        }
    }
}
