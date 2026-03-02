package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.Stream
import com.example.vidplay.domain.repository.StreamRepository
import com.example.vidplay.util.Resource

/**
 * Single-responsibility use case: retrieve only the current user's streams.
 */
class GetMyStreamsUseCase(private val repository: StreamRepository) {

    suspend operator fun invoke(token: String): Resource<List<Stream>> {
        return repository.getMyStreams(token)
    }
}
