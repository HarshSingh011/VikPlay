package com.example.vidplay.presentation.state

import com.example.vidplay.domain.model.MyStream

sealed class MyStreamUiState {
    object Loading : MyStreamUiState()
    data class Success(val streams: List<MyStream>) : MyStreamUiState()
    data class Error(val message: String) : MyStreamUiState()
}
