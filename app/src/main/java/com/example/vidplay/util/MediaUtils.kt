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
import java.util.Locale

object MediaUtils {
    suspend fun getVideos(contentResolver: ContentResolver): List<Media> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
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
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                val name = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val duration = cursor.getLong(durationColumn)

                val video = Media(uri, name, size, mimeType, duration)
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
            null,   
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

    private val documentExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "rtf", "csv", "odt", "ods", "odp", "md", "json", "xml"
    )

    private fun looksLikeDocument(name: String, mimeType: String): Boolean {
        val normalizedMime = mimeType.lowercase(Locale.ROOT)
        if (normalizedMime.startsWith("video/") || normalizedMime.startsWith("audio/") || normalizedMime.startsWith("image/")) {
            return false
        }
        if (normalizedMime == "application/vnd.android.package-archive") {
            return false
        }

        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension in documentExtensions) {
            return true
        }

        return normalizedMime.startsWith("application/") || normalizedMime.startsWith("text/")
    }

    suspend fun getDownloads(contentResolver: ContentResolver): List<Media> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )

        val isQPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val filesCollectionUri = if (isQPlus) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val downloadsCollectionUri = if (isQPlus) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val selection = if (isQPlus) {
            "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
        }
        val selectionArgs = if (isQPlus) {
            arrayOf("Download/%", "Downloads/%")
        } else {
            arrayOf("%/Download/%", "%/Downloads/%")
        }

        val itemsByUri = linkedMapOf<String, Media>()

        fun queryInto(collectionUri: Uri, localSelection: String?, localArgs: Array<String>?) {
            contentResolver.query(
                collectionUri,
                projection,
                localSelection,
                localArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val mediaUri = ContentUris.withAppendedId(collectionUri, id)
                    val key = mediaUri.toString()
                    if (itemsByUri.containsKey(key)) continue

                    itemsByUri[key] = Media(
                        uri = mediaUri,
                        name = cursor.getString(nameCol) ?: "Unknown",
                        size = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeTypeCol) ?: "application/octet-stream"
                    )
                }
            }
        }

        try {
            queryInto(filesCollectionUri, selection, selectionArgs)
            if (itemsByUri.isEmpty()) {
                queryInto(downloadsCollectionUri, null, null)
            }
        } catch (_: SecurityException) {
            // Device-specific scoped-storage policies can restrict this query; caller handles empty state.
        }

        return@withContext itemsByUri.values.toList()
    }

    suspend fun getDocuments(contentResolver: ContentResolver): List<Media> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )

        val isQPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val filesCollectionUri = if (isQPlus) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val downloadsCollectionUri = if (isQPlus) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val folderSelection = if (isQPlus) {
            "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
        }
        val folderSelectionArgs = if (isQPlus) {
            arrayOf("Documents/%", "Download/%", "Downloads/%")
        } else {
            arrayOf("%/Documents/%", "%/Download/%", "%/Downloads/%")
        }

        val itemsByUri = linkedMapOf<String, Media>()

        fun queryInto(collectionUri: Uri, localSelection: String?, localArgs: Array<String>?) {
            contentResolver.query(
                collectionUri,
                projection,
                localSelection,
                localArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: "Unknown"
                    val mimeType = cursor.getString(mimeTypeCol) ?: ""
                    if (!looksLikeDocument(name, mimeType)) continue

                    val id = cursor.getLong(idCol)
                    val mediaUri = ContentUris.withAppendedId(collectionUri, id)
                    val key = mediaUri.toString()
                    if (itemsByUri.containsKey(key)) continue

                    itemsByUri[key] = Media(
                        uri = mediaUri,
                        name = name,
                        size = cursor.getLong(sizeCol),
                        mimeType = mimeType
                    )
                }
            }
        }

        try {
            queryInto(filesCollectionUri, folderSelection, folderSelectionArgs)
            queryInto(downloadsCollectionUri, null, null)
            queryInto(filesCollectionUri, null, null)
        } catch (_: SecurityException) {
            // Device-specific scoped-storage policies can restrict this query; caller handles empty state.
        }

        return@withContext itemsByUri.values.toList()
    }
}

object VideoThumbnailLoader {
    // 50 MB in-memory cache for thumbnails
    val cache = object : android.util.LruCache<Uri, android.graphics.Bitmap>(50 * 1024 * 1024) {
        override fun sizeOf(key: Uri, bitmap: android.graphics.Bitmap): Int {
            return bitmap.byteCount
        }
    }

    fun getThumbnailFromCache(uri: Uri): android.graphics.Bitmap? {
        return cache.get(uri)
    }

    suspend fun getThumbnail(context: android.content.Context, uri: Uri): android.graphics.Bitmap? {
        cache.get(uri)?.let { return it }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, android.util.Size(320, 180), null)
                } else {
                    val path = getRealPathFromUri(context, uri) ?: return@withContext null
                    android.media.ThumbnailUtils.createVideoThumbnail(path, android.provider.MediaStore.Images.Thumbnails.MINI_KIND)
                }
                if (bitmap != null) {
                    cache.put(uri, bitmap)
                }
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getRealPathFromUri(context: android.content.Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    path = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA))
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return path
    }
}