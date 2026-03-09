package com.example.vidplay.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vidplay.data.repository.StreamRepositoryImpl
import com.example.vidplay.data.source.remote.RetrofitClient
import com.example.vidplay.domain.model.MyStream
import com.example.vidplay.domain.model.Stream
import com.example.vidplay.domain.usecase.GetAllStreamsUseCase
import com.example.vidplay.domain.usecase.GetMyStreamsUseCase
import com.example.vidplay.domain.usecase.SearchStreamsUseCase
import com.example.vidplay.domain.usecase.StartStreamUseCase
import com.example.vidplay.presentation.state.MyStreamUiState
import com.example.vidplay.presentation.state.SearchUiState
import com.example.vidplay.presentation.state.StartStreamUiState
import com.example.vidplay.presentation.state.StreamUiState
import com.example.vidplay.util.PreferenceHelper
import com.example.vidplay.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for stream-listing screens.
 *
 * Uses [AndroidViewModel] so it can access [PreferenceHelper] to read the
 * saved token without passing Context through the UI.
 *
 * Dependency graph is constructed here (manual DI).
 * Swap these constructors for injected interfaces once Hilt/Dagger is added.
 */
class StreamViewModel(application: Application) : AndroidViewModel(application) {

    // ------------------------------------------------------------------ //
    // Manual DI wiring (data → domain → viewmodel)
    // ------------------------------------------------------------------ //
    private val prefHelper = PreferenceHelper(application)

    private val repository = StreamRepositoryImpl(RetrofitClient.streamApiService)

    private val getAllStreamsUseCase  = GetAllStreamsUseCase(repository)
    private val getMyStreamsUseCase   = GetMyStreamsUseCase(repository)
    private val searchStreamsUseCase  = SearchStreamsUseCase(repository)
    private val startStreamUseCase    = StartStreamUseCase(repository)

    // ------------------------------------------------------------------ //
    // State exposed to the UI
    // ------------------------------------------------------------------ //

    /** All-streams tab state */
    private val _allStreamsState = MutableStateFlow<StreamUiState>(StreamUiState.Loading)
    val allStreamsState: StateFlow<StreamUiState> = _allStreamsState.asStateFlow()

    /** My-streams (history) tab state */
    private val _myStreamsState = MutableStateFlow<MyStreamUiState>(MyStreamUiState.Loading)
    val myStreamsState: StateFlow<MyStreamUiState> = _myStreamsState.asStateFlow()

    /** Search results state */
    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    /** Start-stream state */
    private val _startStreamState = MutableStateFlow<StartStreamUiState>(StartStreamUiState.Idle)
    val startStreamState: StateFlow<StartStreamUiState> = _startStreamState.asStateFlow()

    // ------------------------------------------------------------------ //
    // Search query stored in the ViewModel so it survives recomposition
    // ------------------------------------------------------------------ //
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        // Clear results when user clears the text field
        if (query.isBlank()) _searchState.value = SearchUiState.Idle
    }

    // ------------------------------------------------------------------ //
    // Actions
    // ------------------------------------------------------------------ //

    init {
        fetchAllStreams()
        fetchMyStreams()
    }

    /**
     * Call the search API with current query.
     * Results replace whatever is displayed in the active tab.
     */
    fun searchStreams() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            val token = prefHelper.token
            when (val result = searchStreamsUseCase(token, query)) {
                is Resource.Success -> {
                    if (result.data.isEmpty()) _searchState.value = SearchUiState.Empty
                    else _searchState.value = SearchUiState.Success(result.data)
                }
                is Resource.Error   -> _searchState.value = SearchUiState.Error(result.message)
                is Resource.Loading -> {}
            }
        }
    }

    /** Start a new live stream via the API. */
    fun startStream(title: String, description: String?, thumbnailUrl: String?) {
        viewModelScope.launch {
            _startStreamState.value = StartStreamUiState.Loading
            val token = prefHelper.token
            when (val result = startStreamUseCase(token, title, description, thumbnailUrl)) {
                is Resource.Success -> _startStreamState.value = StartStreamUiState.Success(result.data)
                is Resource.Error   -> _startStreamState.value = StartStreamUiState.Error(result.message)
                is Resource.Loading -> {}
            }
        }
    }

    /** Reset start-stream state back to Idle (call when leaving LiveStreamingScreen). */
    fun resetStartStreamState() {
        _startStreamState.value = StartStreamUiState.Idle
    }

    /**
     * End an active live stream via the API.
     * Fire-and-forget: called when the broadcaster leaves LiveStreamingScreen.
     * Errors are silently ignored so navigation is never blocked.
     */
    fun endStream(streamCode: String) {
        viewModelScope.launch {
            try {
                repository.endStream(prefHelper.token, streamCode)
            } catch (_: Exception) {}
        }
    }

    /** Called when user switches tab — clears search and reloads the tab's own data. */
    fun onTabSelected(tab: Int) {
        _searchQuery.value = ""
        _searchState.value = SearchUiState.Idle
        if (tab == 0) fetchAllStreams() else fetchMyStreams()
    }

    fun fetchAllStreams() {
        viewModelScope.launch {
            _allStreamsState.value = StreamUiState.Loading
            val token = prefHelper.token
            when (val result = getAllStreamsUseCase(token)) {
                is Resource.Success -> _allStreamsState.value = StreamUiState.Success(result.data)
                is Resource.Error   -> _allStreamsState.value = StreamUiState.Error(result.message)
                is Resource.Loading -> { /* handled by initial state */ }
            }
        }
    }

    fun fetchMyStreams() {
        viewModelScope.launch {
            _myStreamsState.value = MyStreamUiState.Loading
            val token = prefHelper.token
            when (val result = getMyStreamsUseCase(token)) {
                is Resource.Success -> _myStreamsState.value = MyStreamUiState.Success(result.data)
                is Resource.Error   -> _myStreamsState.value = MyStreamUiState.Error(result.message)
                is Resource.Loading -> { /* handled by initial state */ }
            }
        }
    }

    /** Convenience: filter live streams by the current search query. */
    fun filterStreams(streams: List<Stream>): List<Stream> {
        val q = _searchQuery.value.trim()
        return if (q.isBlank()) streams
        else streams.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true)
        }
    }

    /** Convenience: filter history streams by the current search query. */
    fun filterMyStreams(streams: List<MyStream>): List<MyStream> {
        val q = _searchQuery.value.trim()
        return if (q.isBlank()) streams
        else streams.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true)
        }
    }
}
