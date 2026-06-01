package com.example.database

import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao) {

    val watchHistory: Flow<List<WatchHistory>> = videoDao.getWatchHistory()
    val favorites: Flow<List<FavoriteVideo>> = videoDao.getFavorites()
    val playlists: Flow<List<Playlist>> = videoDao.getPlaylists()
    val vaultVideos: Flow<List<VaultVideo>> = videoDao.getVaultVideos()
    val recentStats: Flow<List<DailyWatchStats>> = videoDao.getRecentStats()
    val iptvStreams: Flow<List<IPTVStream>> = videoDao.getIPTVStreams()

    // History methods
    suspend fun insertHistory(history: WatchHistory) {
        videoDao.insertHistory(history)
    }

    suspend fun deleteHistory(url: String) {
        videoDao.deleteHistory(url)
    }

    suspend fun clearHistory() {
        videoDao.clearHistory()
    }

    // Favorites methods
    suspend fun isFavorite(url: String): Boolean {
        return videoDao.isFavorite(url)
    }

    suspend fun insertFavorite(favorite: FavoriteVideo) {
        videoDao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(url: String) {
        videoDao.deleteFavorite(url)
    }

    // Playlists methods
    suspend fun createPlaylist(name: String): Long {
        return videoDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        videoDao.deletePlaylist(playlistId)
    }

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>> {
        return videoDao.getPlaylistItems(playlistId)
    }

    suspend fun addPlaylistItem(item: PlaylistItem) {
        videoDao.insertPlaylistItem(item)
    }

    suspend fun clearPlaylistItems(playlistId: Long) {
        videoDao.clearPlaylistItems(playlistId)
    }

    suspend fun deletePlaylistItem(itemId: Long) {
        videoDao.deletePlaylistItem(itemId)
    }

    // Vault methods
    suspend fun insertVaultVideo(video: VaultVideo) {
        videoDao.insertVaultVideo(video)
    }

    suspend fun deleteVaultVideo(path: String) {
        videoDao.deleteVaultVideo(path)
    }

    // Stats methods
    suspend fun recordWatchTime(minutes: Long) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val existing = videoDao.getStatsForDate(today)
        if (existing != null) {
            videoDao.insertStats(existing.copy(minutesWatched = existing.minutesWatched + minutes))
        } else {
            videoDao.insertStats(DailyWatchStats(dateStr = today, minutesWatched = minutes))
        }
    }

    // IPTV streams
    suspend fun insertIPTVStream(stream: IPTVStream) {
        videoDao.insertIPTVStream(stream)
    }

    suspend fun deleteIPTVStream(url: String) {
        videoDao.deleteIPTVStream(url)
    }
}
