package com.example.library

import java.io.Serializable

data class VideoItem(
    val id: String,
    val title: String,
    val path: String, // Can be a local path or a streaming URL
    val duration: Long = 0L,
    val size: Long = 0L,
    val resolution: String = "1080p",
    val folderName: String = "Online Streams",
    val isLocal: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
) : Serializable {

    val formattedDuration: String
        get() {
            if (duration <= 0) return "00:00"
            val totalSeconds = duration / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }

    companion object {
        val DEFAULT_STREAMS = listOf(
            VideoItem(
                id = "stream_bunny",
                title = "Big Buck Bunny",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                duration = 596000L,
                size = 276134947L,
                resolution = "1920x1080",
                folderName = "Blender Animation",
                isLocal = false
            ),
            VideoItem(
                id = "stream_sintel",
                title = "Sintel (Sci-Fi Fantasy)",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                duration = 652000L,
                size = 127623947L,
                resolution = "1920x818",
                folderName = "Blender Animation",
                isLocal = false
            ),
            VideoItem(
                id = "stream_tears",
                title = "Tears of Steel",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                duration = 734000L,
                size = 312154947L,
                resolution = "1920x800",
                folderName = "CGI VFX Test",
                isLocal = false
            ),
            VideoItem(
                id = "stream_elephants",
                title = "Elephants Dream",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                duration = 653000L,
                size = 134261947L,
                resolution = "1024x576",
                folderName = "Blender Animation",
                isLocal = false
            ),
            VideoItem(
                id = "stream_subtitles",
                title = "Subtitles Demo Showcase",
                path = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubtitlesTest.mp4",
                duration = 59000L,
                size = 10459345L,
                resolution = "1280x720",
                folderName = "Demos & Samples",
                isLocal = false
            )
        )
    }
}
