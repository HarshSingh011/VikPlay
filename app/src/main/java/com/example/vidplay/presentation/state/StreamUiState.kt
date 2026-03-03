package com.example.vidplay.presentation.state

import com.example.vidplay.domain.model.Stream

/**
 * Represents every possible state the AllStreamShownScreen can be in.
 * A sealed class ensures the UI handles all cases exhaustively.
 */
sealed class StreamUiState {

    /** Initial state / network call in progress. */
    object Loading : StreamUiState()

    /** Network call succeeded — [streams] holds the filtered list shown on screen. */
    data class Success(val streams: List<Stream>) : StreamUiState()

    /** Network call (or any unexpected failure) produced an error message. */
    data class Error(val message: String) : StreamUiState()
}
