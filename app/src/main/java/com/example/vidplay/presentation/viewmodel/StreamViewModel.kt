package com.example.vidplay.presentation.viewmodel

import android.app.Application
import android.util.Log
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

class StreamViewModel(application: Application) : AndroidViewModel(application) {

    
    
    
    private val prefHelper = PreferenceHelper(application)

    private val repository = StreamRepositoryImpl(RetrofitClient.streamApiService)

    private val getAllStreamsUseCase  = GetAllStreamsUseCase(repository)
    private val getMyStreamsUseCase   = GetMyStreamsUseCase(repository)
    private val searchStreamsUseCase  = SearchStreamsUseCase(repository)
    private val startStreamUseCase    = StartStreamUseCase(repository)

    
    
    

    
    private val _allStreamsState = MutableStateFlow<StreamUiState>(StreamUiState.Loading)
    val allStreamsState: StateFlow<StreamUiState> = _allStreamsState.asStateFlow()

    
    private val _myStreamsState = MutableStateFlow<MyStreamUiState>(MyStreamUiState.Loading)
    val myStreamsState: StateFlow<MyStreamUiState> = _myStreamsState.asStateFlow()

    
    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    
    private val _startStreamState = MutableStateFlow<StartStreamUiState>(StartStreamUiState.Idle)
    val startStreamState: StateFlow<StartStreamUiState> = _startStreamState.asStateFlow()

    
    
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    
    
    
    private var cachedAllStreams: List<Stream> = emptyList()
    private var cachedMyStreams: List<MyStream> = emptyList()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) _searchState.value = SearchUiState.Idle
    }

    
    
    

    init {
        fetchAllStreams()
        fetchMyStreams()
    }

    

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

    
    fun resetStartStreamState() {
        _startStreamState.value = StartStreamUiState.Idle
    }

    

    fun endStream(streamCode: String) {
        viewModelScope.launch {
            try {
                repository.endStream(prefHelper.token, streamCode)
            } catch (_: Exception) {}
        }
    }

    
    fun onTabSelected(tab: Int) {
        _searchQuery.value = ""
        _searchState.value = SearchUiState.Idle
        if (tab == 0) fetchAllStreams() else fetchMyStreams()
    }

    fun fetchAllStreams() {
        viewModelScope.launch {
            
            if (cachedAllStreams.isEmpty()) {
                _allStreamsState.value = StreamUiState.Loading
            }
            val token = prefHelper.token
            when (val result = getAllStreamsUseCase(token)) {
                is Resource.Success -> {
                    cachedAllStreams = result.data
                    if (result.data.isEmpty()) {
                        _allStreamsState.value = StreamUiState.Success(emptyList())
                    } else {
                        _allStreamsState.value = StreamUiState.Success(result.data)
                    }
                }
                is Resource.Error   -> {
                    Log.e("StreamViewModel", "fetchAllStreams error: ${result.message}")
                    _allStreamsState.value = StreamUiState.Error(result.message)
                }
                is Resource.Loading -> {  }
            }
        }
    }

    fun fetchMyStreams() {
        viewModelScope.launch {
            
            if (cachedMyStreams.isEmpty()) {
                _myStreamsState.value = MyStreamUiState.Loading
            }
            val token = prefHelper.token
            when (val result = getMyStreamsUseCase(token)) {
                is Resource.Success -> {
                    cachedMyStreams = result.data
                    if (result.data.isEmpty()) {
                        _myStreamsState.value = MyStreamUiState.Success(emptyList())
                    } else {
                        _myStreamsState.value = MyStreamUiState.Success(result.data)
                    }
                }
                is Resource.Error   -> {
                    Log.e("StreamViewModel", "fetchMyStreams error: ${result.message}")
                    _myStreamsState.value = MyStreamUiState.Error(result.message)
                }
                is Resource.Loading -> {  }
            }
        }
    }

    
    fun filterStreams(streams: List<Stream>): List<Stream> {
        val q = _searchQuery.value.trim()
        return if (q.isBlank()) streams
        else streams.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true)
        }
    }

    
    fun filterMyStreams(streams: List<MyStream>): List<MyStream> {
        val q = _searchQuery.value.trim()
        return if (q.isBlank()) streams
        else streams.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true)
        }
    }
}
