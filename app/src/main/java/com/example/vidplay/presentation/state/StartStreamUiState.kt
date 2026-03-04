package com.example.vidplay.presentation.state

import com.example.vidplay.domain.model.ActiveStream

sealed class StartStreamUiState {
    object Idle    : StartStreamUiState()
    object Loading : StartStreamUiState()
    data class Success(val stream: ActiveStream) : StartStreamUiState()
    data class Error(val message: String)        : StartStreamUiState()
}
