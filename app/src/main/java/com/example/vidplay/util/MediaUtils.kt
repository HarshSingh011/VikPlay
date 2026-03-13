package com.example.vidplay.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.vidplay.domain.model.Media
import com.example.vidplay.domain.model.MusicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaUtils {
    suspend fun getVideos(contentResolver: ContentResolver): List<Media> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
        )

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val videos = mutableListOf<Media>()

        contentResolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                val name = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn)

                val video = Media(uri, name, size, mimeType)
                videos.add(video)
            }
        }

        return@withContext videos
    }

    suspend fun getMusic(contentResolver: ContentResolver): List<MusicItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
        )

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val albumArtBase = Uri.parse("content://media/external/audio/albumart")
        val items = mutableListOf<MusicItem>()

        contentResolver.query(
            collectionUri,
            projection,
            null,   // include ALL audio files, not just IS_MUSIC
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val albumId  = cursor.getLong(albumIdCol)
                val uri      = ContentUris.withAppendedId(collectionUri, id)
                val artUri   = ContentUris.withAppendedId(albumArtBase, albumId)

                items.add(
                    MusicItem(
                        uri        = uri,
                        title      = cursor.getString(titleCol)  ?: "Unknown",
                        artist     = cursor.getString(artistCol)  ?: "Unknown Artist",
                        album      = cursor.getString(albumCol)   ?: "Unknown Album",
                        duration   = cursor.getLong(durationCol),
                        size       = cursor.getLong(sizeCol),
                        albumArtUri = artUri
                    )
                )
            }
        }

        return@withContext items
    }

    /**
     * Delete a list of media URIs from the device.
     * On Android 11+ (API 30) returns a non-null IntentSender that the caller must launch
     * via ActivityResultLauncher<IntentSenderRequest> to show the system confirmation dialog.
     * On older Android the files are deleted directly and null is returned.
     */
    fun deleteMediaItems(
        contentResolver: ContentResolver,
        uris: List<android.net.Uri>
    ): android.content.IntentSender? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(contentResolver, uris).intentSender
        } else {
            uris.forEach { uri ->
                try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            }
            null
        }
    }

    suspend fun getDocuments(contentResolver: ContentResolver): List<Media> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )

        val mimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
        )

        val selection = mimeTypes.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        }

        val collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val items = mutableListOf<Media>()

        contentResolver.query(
            collectionUri,
            projection,
            selection,
            mimeTypes,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol     = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collectionUri, id)
                items.add(
                    Media(
                        uri      = uri,
                        name     = cursor.getString(nameCol) ?: "Unknown",
                        size     = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeTypeCol) ?: ""
                    )
                )
            }
        }

        return@withContext items
    }
}