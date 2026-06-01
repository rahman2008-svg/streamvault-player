package com.example.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey val videoUrl: String,
    val videoTitle: String,
    val duration: Long = 0L,
    val playbackPosition: Long = 0L,
    val lastWatchedTime: Long = System.currentTimeMillis(),
    val isLocal: Boolean = true,
    val folderName: String = ""
)

@Entity(tableName = "favorites")
data class FavoriteVideo(
    @PrimaryKey val videoUrl: String,
    val videoTitle: String,
    val duration: Long = 0,
    val isLocal: Boolean = true,
    val addedTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items")
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val videoUrl: String,
    val videoTitle: String,
    val duration: Long = 0,
    val isLocal: Boolean = true
)

@Entity(tableName = "vault_videos")
data class VaultVideo(
    @PrimaryKey val videoPath: String,
    val videoTitle: String,
    val duration: Long = 0,
    val passcode: String = ""
)

@Entity(tableName = "daily_statistics")
data class DailyWatchStats(
    @PrimaryKey val dateStr: String, // YYYY-MM-DD
    val minutesWatched: Long = 0L
)

@Entity(tableName = "iptv_streams")
data class IPTVStream(
    @PrimaryKey val streamUrl: String,
    val name: String,
    val groupName: String = "Default",
    val logoUrl: String = ""
)

// ==========================================
// DATABASE DAO
// ==========================================

@Dao
interface VideoDao {
    // History
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTime DESC")
    fun getWatchHistory(): Flow<List<WatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistory)

    @Query("DELETE FROM watch_history WHERE videoUrl = :url")
    suspend fun deleteHistory(url: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedTime DESC")
    fun getFavorites(): Flow<List<FavoriteVideo>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoUrl = :url)")
    suspend fun isFavorite(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteVideo)

    @Query("DELETE FROM favorites WHERE videoUrl = :url")
    suspend fun deleteFavorite(url: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY id ASC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    suspend fun deletePlaylistItem(itemId: Long)

    // Vault
    @Query("SELECT * FROM vault_videos ORDER BY videoTitle ASC")
    fun getVaultVideos(): Flow<List<VaultVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultVideo(video: VaultVideo)

    @Query("DELETE FROM vault_videos WHERE videoPath = :path")
    suspend fun deleteVaultVideo(path: String)

    // Daily Stats
    @Query("SELECT * FROM daily_statistics ORDER BY dateStr DESC LIMIT 7")
    fun getRecentStats(): Flow<List<DailyWatchStats>>

    @Query("SELECT * FROM daily_statistics WHERE dateStr = :date LIMIT 1")
    suspend fun getStatsForDate(date: String): DailyWatchStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: DailyWatchStats)

    // IPTV Streams
    @Query("SELECT * FROM iptv_streams ORDER BY name ASC")
    fun getIPTVStreams(): Flow<List<IPTVStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIPTVStream(stream: IPTVStream)

    @Query("DELETE FROM iptv_streams WHERE streamUrl = :url")
    suspend fun deleteIPTVStream(url: String)
}

// ==========================================
// DATABASE HOLDER
// ==========================================

@Database(
    entities = [
        WatchHistory::class,
        FavoriteVideo::class,
        Playlist::class,
        PlaylistItem::class,
        VaultVideo::class,
        DailyWatchStats::class,
        IPTVStream::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val videoDao: VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streamvault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
