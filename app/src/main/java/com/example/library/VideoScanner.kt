package com.example.library

import android.content.Context
import android.provider.MediaStore
import java.io.File

object VideoScanner {
    fun scanVideos(context: Context): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationCol = getColumnIndexSafe(c, MediaStore.Video.Media.DURATION)
                val sizeCol = getColumnIndexSafe(c, MediaStore.Video.Media.SIZE)
                val resCol = getColumnIndexSafe(c, MediaStore.Video.Media.RESOLUTION)
                val dateCol = getColumnIndexSafe(c, MediaStore.Video.Media.DATE_MODIFIED)

                while (c.moveToNext()) {
                    val id = c.getString(idCol)
                    val title = c.getString(titleCol) ?: "Video_$id"
                    val path = c.getString(pathCol) ?: continue
                    val duration = if (durationCol != -1) c.getLong(durationCol) else 0L
                    val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L
                    val res = if (resCol != -1) c.getString(resCol) ?: "1080p" else "1080p"
                    val dateMod = if (dateCol != -1) c.getLong(dateCol) * 1000 else System.currentTimeMillis()

                    val file = File(path)
                    val folderName = file.parentFile?.name ?: "Internal Storage"

                    if (path.isNotEmpty() && File(path).exists()) {
                        list.add(
                            VideoItem(
                                id = id,
                                title = title,
                                path = path,
                                duration = duration,
                                size = size,
                                resolution = res,
                                folderName = folderName,
                                isLocal = true,
                                lastModified = dateMod
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun getColumnIndexSafe(cursor: android.database.Cursor, columnName: String): Int {
        return try {
            cursor.getColumnIndexOrThrow(columnName)
        } catch (_: Exception) {
            -1
        }
    }
}
