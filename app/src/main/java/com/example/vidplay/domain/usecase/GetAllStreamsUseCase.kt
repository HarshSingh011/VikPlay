package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.Stream
import com.example.vidplay.domain.repository.StreamRepository
import com.example.vidplay.util.Resource

/**
 * Single-responsibility use case: retrieve all public streams.
 * The ViewModel calls this; the use case talks only to the repository interface.
 */
class GetAllStreamsUseCase(private val repository: StreamRepository) {

    suspend operator fun invoke(token: String): Resource<List<Stream>> {
        return repository.getAllStreams(token)
    }
}
