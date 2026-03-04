package com.example.vidplay.domain.usecase

import com.example.vidplay.domain.model.ActiveStream
import com.example.vidplay.domain.repository.StreamRepository
import com.example.vidplay.util.Resource

/** Single-responsibility use case: start a new live stream. */
class StartStreamUseCase(private val repository: StreamRepository) {

    suspend operator fun invoke(
        token: String,
        title: String,
        description: String?,
        thumbnailUrl: String?
    ): Resource<ActiveStream> = repository.startStream(token, title, description, thumbnailUrl)
}
