package com.example.vidplay.presentation.state

import com.example.vidplay.domain.model.Stream

sealed class StreamUiState {

    
    object Loading : StreamUiState()

    
    data class Success(val streams: List<Stream>) : StreamUiState()

    
    data class Error(val message: String) : StreamUiState()
}
