package com.vibeplay.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeplay.music.domain.model.Album
import com.vibeplay.music.domain.model.Artist
import com.vibeplay.music.domain.model.MusicFolder
import com.vibeplay.music.domain.model.Song
import com.vibeplay.music.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    FOLDERS("Folders")
}

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val songs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val folders: List<MusicFolder> = emptyList(),
    val searchHistory: List<String> = emptyList()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SearchFilter.ALL)

    private val debouncedQuery = _query.debounce(150L)

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _selectedFilter,
        debouncedQuery.flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else musicRepository.searchSongs(q.trim())
        },
        musicRepository.observeArtists(),
        musicRepository.observeAlbums(),
        musicRepository.observeFolders(),
        musicRepository.observeSearchHistory()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val rawQuery = values[0] as String
        val filter = values[1] as SearchFilter
        @Suppress("UNCHECKED_CAST")
        val matchedSongs = values[2] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val allArtists = values[3] as List<Artist>
        @Suppress("UNCHECKED_CAST")
        val allAlbums = values[4] as List<Album>
        @Suppress("UNCHECKED_CAST")
        val allFolders = values[5] as List<MusicFolder>
        @Suppress("UNCHECKED_CAST")
        val history = values[6] as List<String>

        val q = rawQuery.trim()
        val matchedArtists = if (q.isBlank()) emptyList() else {
            allArtists.filter { it.name.contains(q, ignoreCase = true) }
        }
        val matchedAlbums = if (q.isBlank()) emptyList() else {
            allAlbums.filter {
                it.name.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
            }
        }
        val matchedFolders = if (q.isBlank()) emptyList() else {
            allFolders.filter {
                it.name.contains(q, ignoreCase = true) || it.path.contains(q, ignoreCase = true)
            }
        }

        SearchUiState(
            query = rawQuery,
            selectedFilter = filter,
            songs = matchedSongs,
            artists = matchedArtists,
            albums = matchedAlbums,
            folders = matchedFolders,
            searchHistory = history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun selectFilter(filter: SearchFilter) {
        _selectedFilter.value = filter
    }

    fun commitSearchQuery(queryToSave: String = _query.value) {
        if (queryToSave.isNotBlank()) {
            viewModelScope.launch {
                musicRepository.saveSearchQuery(queryToSave)
            }
        }
    }

    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            musicRepository.deleteSearchQuery(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            musicRepository.clearSearchHistory()
        }
    }
}
