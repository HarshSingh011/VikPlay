package com.example.vidplay.domain.repository

import com.example.vidplay.domain.model.ActiveStream
import com.example.vidplay.domain.model.MyStream
import com.example.vidplay.domain.model.Stream
import com.example.vidplay.util.Resource

/**
 * Contract that the data layer must fulfil.
 * The domain layer depends on this abstraction, never on the concrete impl.
 */
interface StreamRepository {

    /** Fetch every public stream from the remote source. */
    suspend fun getAllStreams(token: String): Resource<List<Stream>>

    /** Fetch the authenticated user's stream history. */
    suspend fun getMyStreams(token: String): Resource<List<MyStream>>

    /** Search streams by keyword; live_only=true by default. */
    suspend fun searchStreams(token: String, query: String): Resource<List<MyStream>>

    /** Start a new live stream and return the created stream (includes stream_key). */
    suspend fun startStream(
        token: String,
        title: String,
        description: String?,
        thumbnailUrl: String?
    ): Resource<ActiveStream>
}
