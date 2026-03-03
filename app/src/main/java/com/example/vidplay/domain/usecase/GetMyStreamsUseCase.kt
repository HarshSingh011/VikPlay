package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.MyStream
import com.example.vidplay.domain.repository.StreamRepository
import com.example.vidplay.util.Resource

/**
 * Single-responsibility use case: retrieve the current user's stream history.
 */
class GetMyStreamsUseCase(private val repository: StreamRepository) {

    suspend operator fun invoke(token: String): Resource<List<MyStream>> {
        return repository.getMyStreams(token)
    }
}
