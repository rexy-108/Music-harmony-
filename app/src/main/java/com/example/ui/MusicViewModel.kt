package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthEngine
import com.example.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(
    application: Application,
    private val repository: MusicRepository,
    private val geminiService: GeminiSongService
) : AndroidViewModel(application) {

    // Audio synth instance
    private val synthEngine = AudioSynthEngine()
    private var progressJob: Job? = null

    // Room DB Flows
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistWithSongs>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Song>> = _searchQuery
        .combine(allSongs) { query, songs ->
            if (query.isBlank()) {
                songs
            } else {
                songs.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.singer.contains(query, ignoreCase = true) ||
                    it.genre.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback States
    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong = _currentPlayingSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0)
    val playbackProgress = _playbackProgress.asStateFlow()

    private var _playbackQueue = mutableListOf<Song>()
    val playbackQueue: List<Song> get() = _playbackQueue

    private var _currentQueueIndex = 0
    val currentQueueIndex: Int get() = _currentQueueIndex

    // AI Discovery States
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering = _isDiscovering.asStateFlow()

    private val _discoveryResult = MutableStateFlow<String?>(null)
    val discoveryResult = _discoveryResult.asStateFlow()

    init {
        // Collect current playing state and synchronize chiptune synthesizer
        viewModelScope.launch {
            _currentPlayingSong.combine(_isPlaying) { song, playing ->
                Pair(song, playing)
            }.collect { (song, playing) ->
                if (song != null && playing) {
                    synthEngine.startPlaying(song.synthPattern, viewModelScope)
                    startProgressTimer()
                } else {
                    synthEngine.stopPlaying()
                    stopProgressTimer()
                }
            }
        }
    }

    // Playback Actions
    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        viewModelScope.launch {
            if (queue.isNotEmpty()) {
                _playbackQueue = queue.toMutableList()
                _currentQueueIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            } else {
                _playbackQueue = mutableListOf(song)
                _currentQueueIndex = 0
            }
            _currentPlayingSong.value = song
            _playbackProgress.value = 0
            _isPlaying.value = true
        }
    }

    fun togglePlayPause() {
        if (_currentPlayingSong.value == null) {
            // Play first available song if none playing
            val songs = allSongs.value
            if (songs.isNotEmpty()) {
                playSong(songs.first(), songs)
            }
            return
        }
        _isPlaying.value = !_isPlaying.value
    }

    fun playNext() {
        if (_playbackQueue.isEmpty()) return
        _currentQueueIndex = (_currentQueueIndex + 1) % _playbackQueue.size
        val nextSong = _playbackQueue[_currentQueueIndex]
        _currentPlayingSong.value = nextSong
        _playbackProgress.value = 0
        _isPlaying.value = true
    }

    fun playPrevious() {
        if (_playbackQueue.isEmpty()) return
        _currentQueueIndex = if (_currentQueueIndex - 1 < 0) _playbackQueue.size - 1 else _currentQueueIndex - 1
        val prevSong = _playbackQueue[_currentQueueIndex]
        _currentPlayingSong.value = prevSong
        _playbackProgress.value = 0
        _isPlaying.value = true
    }

    fun seekTo(seconds: Int) {
        val maxDuration = _currentPlayingSong.value?.durationSeconds ?: 0
        _playbackProgress.value = seconds.coerceIn(0, maxDuration)
    }

    private fun startProgressTimer() {
        stopProgressTimer()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _playbackProgress.value
                val maxDuration = _currentPlayingSong.value?.durationSeconds ?: 180
                if (current >= maxDuration) {
                    // Auto play next song
                    playNext()
                } else {
                    _playbackProgress.value = current + 1
                }
            }
        }
    }

    private fun stopProgressTimer() {
        progressJob?.cancel()
        progressJob = null
    }

    // Playlist Actions
    fun createPlaylist(name: String, description: String, songIds: List<String>) {
        viewModelScope.launch {
            repository.createPlaylist(name, description, "My Account", songIds)
        }
    }

    fun updatePlaylist(playlistId: Int, name: String, description: String, songIds: List<String>) {
        viewModelScope.launch {
            repository.updatePlaylistWithSongs(playlistId, name, description, "My Account", songIds)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    // Custom song inputs ("add any song custom manual")
    fun addManualSong(title: String, singer: String, album: String, genre: String, duration: Int, lyrics: String, synthStyle: String) {
        viewModelScope.launch {
            val shortId = "manual_song_${System.currentTimeMillis() % 1000000}"
            val finalImage = when (genre.lowercase()) {
                "synthwave" -> "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300"
                "ballad", "classical" -> "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=300"
                "ambient" -> "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=300"
                "rock" -> "https://images.unsplash.com/photo-1487180144351-b8472da7a4c3?w=300"
                else -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"
            }
            val song = Song(
                id = shortId,
                title = title,
                singer = singer,
                album = album.ifEmpty { "Single" },
                genre = genre,
                durationSeconds = duration,
                lyrics = lyrics.ifEmpty { "Enjoy this beautifully customized melody!" },
                imageUrl = finalImage,
                synthPattern = synthStyle,
                isPreloaded = false
            )
            repository.insertSong(song)
        }
    }

    // Search query trigger
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // AI dynamic crawler discovery
    fun discoverWorldSong(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveryResult.value = "Scanning global networks and crawling metadata..."
            
            val song = geminiService.discoverSongFromWorld(prompt)
            if (song != null) {
                repository.insertSong(song)
                _discoveryResult.value = "Bingo! Discovered '${song.title}' by ${song.singer}. Added to Library!"
                _searchQuery.value = "" // Show in library
                playSong(song, allSongs.value + song)
            } else {
                _discoveryResult.value = "Unable to fetch from global indexes. Please try again!"
            }
            
            delay(4000)
            _isDiscovering.value = false
            _discoveryResult.value = null
        }
    }

    // Sharing Feature: Export
    fun sharePlaylist(context: Context, playlistAndSongs: PlaylistWithSongs) {
        val code = playlistAndSongs.playlist.shareCode.ifEmpty {
            repository.generateShareCode(
                playlistAndSongs.playlist.name,
                playlistAndSongs.playlist.description,
                playlistAndSongs.playlist.creator,
                playlistAndSongs.songs.map { it.id }
            )
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Music Harmony Shared Playlist", code)
        clipboard.setPrimaryClip(clip)
    }

    // Sharing Feature: Import
    fun importPlaylist(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (code.isBlank()) {
            onError("Paste some exported code first!")
            return
        }
        viewModelScope.launch {
            try {
                val imported = repository.importPlaylistFromShareCode(code)
                if (imported != null) {
                    onSuccess()
                } else {
                    onError("Invalid code! Make sure it starts with 'PLAYLIST_SHARE:' or 'PLAYLIST_EXPORT:'")
                }
            } catch (e: Exception) {
                onError("Failed to parse the code: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        synthEngine.stopPlaying()
    }
}

// ViewModelFactory for clean Dependency injection
class MusicViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            val dbScope = CoroutineScope(Dispatchers.IO)
            val database = MusicDatabase.getInstance(application, dbScope)
            val repository = MusicRepository(database.musicDao())
            val geminiService = GeminiSongService()
            return MusicViewModel(application, repository, geminiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
