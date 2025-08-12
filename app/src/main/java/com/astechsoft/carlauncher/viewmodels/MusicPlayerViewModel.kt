package com.astechsoft.carlauncher.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaPlayer: MediaPlayer? = null
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    private val _audioFiles = mutableStateListOf<AudioFile>()
    private var currentIndex = 0

    private val _selectedAudio = MutableStateFlow<AudioFile?>(null)
    val selectedAudio: StateFlow<AudioFile?> = _selectedAudio

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration

    private val _upcomingSongs = MutableStateFlow<List<AudioFile>>(emptyList())
    val upcomingSongs: StateFlow<List<AudioFile>> = _upcomingSongs

    private val _playlists = mutableStateListOf<Playlist>()
    val playlists: SnapshotStateList<Playlist> get() = _playlists

    private val handler = Handler(Looper.getMainLooper())
    private val updatePositionRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    _currentPosition.value = it.currentPosition.toLong()
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private val playlistStorage = PlaylistStorage(context)

    private lateinit var mediaSession: MediaSessionCompat

    init {
        initMediaSession()

        val savedPlaylists = playlistStorage.loadPlaylists()
        if (savedPlaylists.isEmpty()) {
            _playlists.add(Playlist("Favoriler", emptyList()))
        } else {
            _playlists.addAll(savedPlaylists)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(getApplication(), "MusicPlayerSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playPause(true)
                }

                override fun onPause() {
                    playPause(false)
                }

                override fun onSkipToNext() {
                    playNext()
                }

                override fun onSkipToPrevious() {
                    playPrevious()
                }
            })

            isActive = true
        }
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, mediaPlayer?.currentPosition?.toLong() ?: 0L, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    data class AudioFile(
        val title: String,
        val artist: String?,
        val uriString: String,
        val album: String? = null,
        val duration: Long? = null
    ) {
        val uri: Uri
            get() = Uri.parse(uriString)
    }

    data class Playlist(
        val name: String,
        val songs: List<AudioFile>,
        val imageUriString: String? = null
    ) {
        val imageUri: Uri?
            get() = imageUriString?.let { Uri.parse(it) }
    }

    fun setAudioFiles(files: List<AudioFile>) {
        _audioFiles.clear()
        _audioFiles.addAll(files)

        _upcomingSongs.value = files

        if (_audioFiles.isNotEmpty()) {
            currentIndex = 0
            setAudio(_audioFiles[currentIndex], forcePlay = true)
        }
    }

    fun setAudio(audio: AudioFile, forcePlay: Boolean = false) {
        if (!forcePlay && _selectedAudio.value?.uri == audio.uri) return

        releasePlayer()

        _selectedAudio.value = audio
        _currentPosition.value = 0L
        _isPlaying.value = false
        _totalDuration.value = 0L

        try {
            MediaPlayer().apply {
                setDataSource(context, audio.uri)

                setOnPreparedListener { player ->
                    _totalDuration.value = player.duration.toLong()
                    player.start()
                    _isPlaying.value = true
                    handler.post(updatePositionRunnable)

                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    handler.removeCallbacks(updatePositionRunnable)
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                    playNext()
                }

                prepareAsync()
                mediaPlayer = this
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _selectedAudio.value = null
            _isPlaying.value = false
        }
    }

    fun playPause(play: Boolean? = null) {
        mediaPlayer?.let {
            val shouldPlay = play ?: !it.isPlaying
            if (shouldPlay && !it.isPlaying) {
                it.start()
                _isPlaying.value = true
                handler.post(updatePositionRunnable)
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            } else if (!shouldPlay && it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                handler.removeCallbacks(updatePositionRunnable)
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    fun playNext() {
        val currentAudio = _selectedAudio.value ?: return
        val upcomingList = _upcomingSongs.value
        val upcomingIndex = upcomingList.indexOfFirst { it.uri == currentAudio.uri }
        if (upcomingIndex != -1) {
            val nextIndex = (upcomingIndex + 1) % upcomingList.size
            setAudio(upcomingList[nextIndex], forcePlay = true)
        }
    }
    fun playPlaylist(playlist: Playlist) {
        if (playlist.songs.isNotEmpty()) {
            _upcomingSongs.value = playlist.songs
            setAudio(playlist.songs[0], forcePlay = true)
        }
    }
    fun selectAudioAndPlayFromHere(selectedAudio: AudioFile, fullList: List<AudioFile>) {
        val startIndex = fullList.indexOfFirst { it.uri == selectedAudio.uri }
        if (startIndex == -1) return

        val newQueue = fullList.drop(startIndex) + fullList.take(startIndex)

        updateUpcomingList(newQueue)
        setAudio(selectedAudio, forcePlay = true)
    }
    fun playPrevious() {
        val currentAudio = _selectedAudio.value ?: return
        val upcomingList = _upcomingSongs.value
        val upcomingIndex = upcomingList.indexOfFirst { it.uri == currentAudio.uri }

        if (upcomingIndex > 0) {
            val previousAudio = upcomingList[upcomingIndex - 1]
            setAudio(previousAudio, forcePlay = true)
        }
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    fun updateUpcomingList(newList: List<AudioFile>) {
        _upcomingSongs.value = newList
    }

    private fun releasePlayer() {
        handler.removeCallbacks(updatePositionRunnable)
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _totalDuration.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        mediaSession.release()
        releasePlayer()
    }


    fun toggleFavorite(audioFile: AudioFile) {
        val favIndex = _playlists.indexOfFirst { it.name == "Favoriler" }
        if (favIndex == -1) {
            addPlaylist("Favoriler", listOf(audioFile))
        } else {
            val favPlaylist = _playlists[favIndex]
            val isFav = favPlaylist.songs.any { it.uriString == audioFile.uriString }
            val newSongs = if (isFav) {
                favPlaylist.songs.filter { it.uriString != audioFile.uriString }
            } else {
                favPlaylist.songs + audioFile
            }
            _playlists[favIndex] = favPlaylist.copy(songs = newSongs)
            playlistStorage.savePlaylists(_playlists)
        }
    }
    fun addPlaylist(name: String, songs: List<AudioFile> = emptyList()) {
        if (_playlists.none { it.name == name }) {
            _playlists.add(Playlist(name, songs))
            playlistStorage.savePlaylists(_playlists)
        }
    }

    fun changePlaylistImage(playlist: Playlist, imageUri: Uri) {
        val index = _playlists.indexOfFirst { it.name == playlist.name }
        if (index != -1) {
            val updatedPlaylist = playlist.copy(imageUriString = imageUri.toString())
            _playlists[index] = updatedPlaylist
            playlistStorage.savePlaylists(_playlists)
        }
    }

    fun shuffleUpcomingSongs(currentPlaying: AudioFile?) {
        currentPlaying ?: return

        val current = currentPlaying
        val rest = _upcomingSongs.value.filter { it.uri != current.uri }.shuffled()
        _upcomingSongs.value = listOf(current) + rest
    }

    class PlaylistStorage(private val context: Context) {
        private val prefs = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
        private val gson = Gson()

        fun savePlaylists(playlists: List<Playlist>) {
            val playlistsForSave = playlists.map {
                it.copy(imageUriString = it.imageUri?.toString())
            }
            val json = gson.toJson(playlistsForSave)
            prefs.edit().putString("playlists", json).apply()
        }

        fun loadPlaylists(): List<Playlist> {
            val json = prefs.getString("playlists", null) ?: return emptyList()
            val type = object : TypeToken<List<Playlist>>() {}.type
            return gson.fromJson(json, type)
        }
    }


    companion object {
        fun getAllAudioFiles(context: Context): List<AudioFile> {
            val audioList = mutableListOf<AudioFile>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex)
                    val artist = it.getString(artistIndex) ?: "Bilinmiyor"
                    val album = it.getString(albumIndex) ?: "Bilinmiyor"
                    val duration = it.getLong(durationIndex).takeIf { d -> d > 0 } ?: 0L


                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    audioList.add(AudioFile(title, artist, contentUri.toString(), album, duration))
                }
            }
            return audioList
        }
    }


    fun addSongToPlaylist(playlistName: String, song: AudioFile) {
        val index = _playlists.indexOfFirst { it.name == playlistName }
        if (index != -1) {
            val playlist = _playlists[index]
            val newSongs = playlist.songs.toMutableList()
            if (!newSongs.any { it.uri == song.uri }) {
                newSongs.add(song)
                _playlists[index] = playlist.copy(songs = newSongs)
                playlistStorage.savePlaylists(_playlists)
            }
        }
    }


    fun addToQueue(song: AudioFile) {
        val currentQueue = _upcomingSongs.value.toMutableList()
        currentQueue.add(song) // duplicate serbest
        updateUpcomingList(currentQueue)
    }

    fun deleteSongFromDevice(audioFile: AudioFile): Boolean {
        return try {
            val rowsDeleted = context.contentResolver.delete(audioFile.uri, null, null)
            if (rowsDeleted > 0) {
                removeSongFromAllPlaylists(audioFile)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    private fun removeSongFromAllPlaylists(song: AudioFile) {
        val updatedPlaylists = _playlists.map { playlist ->
            val filteredSongs = playlist.songs.filter { it.uri != song.uri }
            playlist.copy(songs = filteredSongs)
        }
        _playlists.clear()
        _playlists.addAll(updatedPlaylists)
        playlistStorage.savePlaylists(_playlists)
    }

    fun renameSongFile(audioFile: AudioFile, newTitle: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, newTitle)
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$newTitle.mp3")
            }
            val rowsUpdated = context.contentResolver.update(audioFile.uri, values, null, null)
            if (rowsUpdated > 0) {
                renameSongInAllPlaylists(audioFile, newTitle)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun renameSongInAllPlaylists(oldSong: AudioFile, newTitle: String) {
        val updatedPlaylists = _playlists.map { playlist ->
            val newSongs = playlist.songs.map {
                if (it.uri == oldSong.uri) it.copy(title = newTitle) else it
            }
            playlist.copy(songs = newSongs)
        }
        _playlists.clear()
        _playlists.addAll(updatedPlaylists)
        playlistStorage.savePlaylists(_playlists)
    }

    fun deletePlaylist(playlist: Playlist) {
        _playlists.remove(playlist)
        playlistStorage.savePlaylists(_playlists)
    }

    fun renamePlaylist(playlist: Playlist, newName: String) {
        val index = _playlists.indexOfFirst { it.name == playlist.name }
        if (index != -1) {
            _playlists[index] = playlist.copy(name = newName)
            playlistStorage.savePlaylists(_playlists)
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _selectedAudio.value = null
        _isPlaying.value = false
        handler.removeCallbacks(updatePositionRunnable)
        _currentPosition.value = 0L
        _totalDuration.value = 0L
    }
}