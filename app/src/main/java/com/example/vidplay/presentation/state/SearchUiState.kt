package com.example.vidplay.presentation.state

import com.example.vidplay.domain.model.MyStream

sealed class SearchUiState {
    object Idle    : SearchUiState()   
    object Loading : SearchUiState()
    object Empty   : SearchUiState()   
    data class Success(val results: List<MyStream>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
