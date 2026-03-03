package com.example.vidplay.presentation.state

import com.example.vidplay.domain.model.MyStream

/** UI state for the search results overlay. */
sealed class SearchUiState {
    object Idle    : SearchUiState()   // no search active
    object Loading : SearchUiState()
    object Empty   : SearchUiState()   // API returned []
    data class Success(val results: List<MyStream>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
