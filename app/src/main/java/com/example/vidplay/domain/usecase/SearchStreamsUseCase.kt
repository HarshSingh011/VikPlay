package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.MyStream
import com.example.vidplay.domain.repository.StreamRepository
import com.example.vidplay.util.Resource

/** Search live/history streams by query string. */
class SearchStreamsUseCase(private val repository: StreamRepository) {

    suspend operator fun invoke(token: String, query: String): Resource<List<MyStream>> {
        return repository.searchStreams(token, query)
    }
}
