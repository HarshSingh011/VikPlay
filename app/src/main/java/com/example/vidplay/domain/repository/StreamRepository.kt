package com.example.vidplay.domain.repository

import com.example.vidplay.domain.model.Stream
import com.example.vidplay.util.Resource

/**
 * Contract that the data layer must fulfil.
 * The domain layer depends on this abstraction, never on the concrete impl.
 */
interface StreamRepository {

    /** Fetch every public stream from the remote source. */
    suspend fun getAllStreams(token: String): Resource<List<Stream>>

    /** Fetch only the streams owned by the authenticated user. */
    suspend fun getMyStreams(token: String): Resource<List<Stream>>
}
