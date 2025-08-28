@file:Suppress("DEPRECATION")
package com.emreata.carlauncher.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.core.content.edit

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

    private val _upcomingSongs = mutableStateListOf<AudioFile>()
    val upcomingSongs: SnapshotStateList<AudioFile> get() = _upcomingSongs

    private val _playlists = mutableStateListOf<Playlist>()
    val playlists: SnapshotStateList<Playlist> get() = _playlists

    private var positionJob: Job? = null

    private val sharedPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
    private val gsonForPrefs = Gson()
    private var isRestoringState = false
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
            get() = uriString.toUri()
    }

    data class Playlist(
        val name: String,
        val songs: List<AudioFile>,
        val imageUriString: String? = null
    ) {
        val imageUri: Uri?
            get() = imageUriString?.toUri()
    }

    fun setAudioFiles(files: List<AudioFile>) {
        _audioFiles.clear()
        _audioFiles.addAll(files)

        _upcomingSongs.clear()
        _upcomingSongs.addAll(files)

        loadLastPlaybackState()
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val autoPlayOnLaunch = prefs.getBoolean("auto_play_on_launch", false)
        if (_selectedAudio.value == null && _audioFiles.isNotEmpty()) {
            currentIndex = 0
            setAudio(_audioFiles[currentIndex], forcePlay = autoPlayOnLaunch)
        } else {
            _selectedAudio.value?.let { setAudio(it, forcePlay = autoPlayOnLaunch) }
        }
    }


    fun setAudio(audio: AudioFile, forcePlay: Boolean = false) {
        if (!forcePlay && _selectedAudio.value?.uri == audio.uri && mediaPlayer != null) return

        _selectedAudio.value = audio
        _currentPosition.value = 0L
        _isPlaying.value = false
        _totalDuration.value = 0L
        savePlaybackState()
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(context, audio.uri)

            mediaPlayer?.setOnPreparedListener { player ->
                _totalDuration.value = player.duration.toLong()
                if (forcePlay) {
                    player.start()
                    _isPlaying.value = true
                    startUpdatingPosition()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    _isPlaying.value = false
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }
            }

            mediaPlayer?.setOnCompletionListener {
                _isPlaying.value = false
                stopUpdatingPosition()
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                playNext()
            }

            mediaPlayer?.prepareAsync()
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
                startUpdatingPosition()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            } else if (!shouldPlay && it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopUpdatingPosition()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
            savePlaybackState()
        }
    }
    fun playNext() {
        val currentAudio = _selectedAudio.value ?: return
        val upcomingList = _upcomingSongs.toList() // immutable kopya alıyoruz
        val upcomingIndex = upcomingList.indexOfFirst { it.uri == currentAudio.uri }
        if (upcomingIndex != -1) {
            val nextIndex = (upcomingIndex + 1) % upcomingList.size
            setAudio(upcomingList[nextIndex], forcePlay = true)
        }
        savePlaybackState()
    }
    fun playPlaylist(playlist: Playlist) {
        if (playlist.songs.isNotEmpty()) {
            _upcomingSongs.clear()
            _upcomingSongs.addAll(playlist.songs)
            setAudio(playlist.songs[0], forcePlay = true)
        }
        savePlaybackState()
    }
    fun selectAudioAndPlayFromHere(selectedAudio: AudioFile, fullList: List<AudioFile>) {
        val startIndex = fullList.indexOfFirst { it.uri == selectedAudio.uri }
        if (startIndex == -1) return

        val newQueue = fullList.drop(startIndex) + fullList.take(startIndex)

        updateUpcomingList(newQueue)
        setAudio(selectedAudio, forcePlay = true)
        savePlaybackState()

    }
    fun playPrevious() {
        val currentAudio = _selectedAudio.value ?: return
        val upcomingList = _upcomingSongs.toList()
        val upcomingIndex = upcomingList.indexOfFirst { it.uri == currentAudio.uri }

        if (upcomingIndex > 0) {
            val previousAudio = upcomingList[upcomingIndex - 1]
            setAudio(previousAudio, forcePlay = true)
        }
        savePlaybackState()
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    fun updateUpcomingList(newList: List<AudioFile>) {
        _upcomingSongs.clear()
        _upcomingSongs.addAll(newList)
        savePlaybackState()

    }

    private fun releasePlayer() {
        stopUpdatingPosition()
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
        savePlaybackState()

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
        val current = currentPlaying ?: return
        val rest = _upcomingSongs.filter { it.uri != current.uri }.shuffled()

        val newQueue = listOf(current) + rest
        _upcomingSongs.clear()
        _upcomingSongs.addAll(newQueue)

        if (_isPlaying.value) {
            setAudio(current, forcePlay = true)
        }
        savePlaybackState()

    }

    class PlaylistStorage(context: Context) {
        private val prefs = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
        private val gson = Gson()

        fun savePlaylists(playlists: List<Playlist>) {
            val playlistsForSave = playlists.map {
                it.copy(imageUriString = it.imageUri?.toString())
            }
            val json = gson.toJson(playlistsForSave)
            prefs.edit { putString("playlists", json) }
        }

        fun loadPlaylists(): List<Playlist> {
            val json = prefs.getString("playlists", null) ?: return emptyList()
            val type = object : TypeToken<List<Playlist>>() {}.type
            return gson.fromJson(json, type)
        }
    }

    companion object {
        suspend fun getAllAudioFiles(context: Context): List<AudioFile> = withContext(
            Dispatchers.IO) {
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
            audioList
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

    private fun startUpdatingPosition() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (_isPlaying.value) {
                _currentPosition.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(1000L)
            }
        }
    }

    private fun stopUpdatingPosition() {
        positionJob?.cancel()
    }

    fun addToQueue(song: AudioFile) {
        _upcomingSongs.add(song)
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

    fun savePlaybackState() {
        if (isRestoringState) {
            return
        }
        val current = _selectedAudio.value ?: run {
            return
        }
        val index = _upcomingSongs.indexOfFirst { it.uriString == current.uriString }
        if (index == -1) {
            return
        }
        sharedPrefs.edit {
            putString("last_audio_uri", current.uriString)
            putInt("last_audio_index", index)
            val upcomingUriList = _upcomingSongs.map { it.uriString }
            val upcomingJson = gsonForPrefs.toJson(upcomingUriList)
            putString("last_upcoming_json", upcomingJson)

        }
    }

    fun loadLastPlaybackState() {
        isRestoringState = true
        val lastAudioUri = sharedPrefs.getString("last_audio_uri", null)
        val upcomingJson = sharedPrefs.getString("last_upcoming_json", null)
        if (!upcomingJson.isNullOrEmpty()) {
            try {
                val uriListType = object : TypeToken<List<String>>() {}.type
                val savedUriList: List<String> = gsonForPrefs.fromJson(upcomingJson, uriListType)
                val restoredUpcoming = savedUriList.mapNotNull { uriStr ->
                    _audioFiles.find { it.uriString == uriStr }
                }
                if (restoredUpcoming.isNotEmpty()) {
                    _upcomingSongs.clear()
                    _upcomingSongs.addAll(restoredUpcoming)
                }
            } catch (e: Exception) {
            }
        }

        if (lastAudioUri != null) {
            val audioInUpcoming = _upcomingSongs.find { it.uriString == lastAudioUri }
            if (audioInUpcoming != null) {
                _selectedAudio.value = audioInUpcoming
                currentIndex = _upcomingSongs.indexOf(audioInUpcoming)
            } else {
                val audioGlobal = _audioFiles.find { it.uriString == lastAudioUri }
                if (audioGlobal != null) {
                    _selectedAudio.value = audioGlobal
                    currentIndex = _audioFiles.indexOf(audioGlobal)
                    if (_upcomingSongs.isEmpty() && _audioFiles.isNotEmpty()) {
                        _upcomingSongs.clear()
                        _upcomingSongs.addAll(_audioFiles)
                        currentIndex = _audioFiles.indexOf(audioGlobal)
                    }
                }
            }
        }
        if (_upcomingSongs.isNotEmpty()) {
            if (currentIndex < 0 || currentIndex >= _upcomingSongs.size) currentIndex = 0
            _selectedAudio.value = _upcomingSongs.getOrNull(currentIndex) ?: _selectedAudio.value
        }

        isRestoringState = false
    }

    fun stopAudio() {
        stopUpdatingPosition()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _selectedAudio.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _totalDuration.value = 0L
    }

}
class MusicPlayerService : Service() {
    private lateinit var mediaSession: MediaSessionCompat
    lateinit var musicPlayerViewModel: MusicPlayerViewModel
    private lateinit var audioManager: AudioManager
    private var maxVolume = 0

    override fun onCreate() {
        super.onCreate()
        musicPlayerViewModel = MusicPlayerViewModel(application)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        initMediaSession()
        requestAudioFocus()
    }
    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "CarLauncherSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    requestAudioFocus()
                    musicPlayerViewModel.playPause(true)
                }
                override fun onPause() {
                    musicPlayerViewModel.playPause(false)
                }
                override fun onSkipToNext() {
                    musicPlayerViewModel.playNext()
                }
                override fun onSkipToPrevious() {
                    musicPlayerViewModel.playPrevious()
                }
                override fun onSeekTo(pos: Long) {
                    musicPlayerViewModel.seekTo(pos)
                }
                override fun onStop() {
                    musicPlayerViewModel.stopAudio()
                }
                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        "VOLUME_UP" -> adjustVolume(AudioManager.ADJUST_RAISE)
                        "VOLUME_DOWN" -> adjustVolume(AudioManager.ADJUST_LOWER)
                        "VOLUME_MUTE" -> mute(true)
                        "VOLUME_UNMUTE" -> mute(false)
                        "SET_VOLUME" -> { extras?.getInt("volume")?.let { setVolume(it) } }
                    }
                }
                override fun onMediaButtonEvent(mediaButtonIntent: Intent?): Boolean {
                    val keyEvent = mediaButtonIntent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    keyEvent?.let {
                        if (it.action == KeyEvent.ACTION_DOWN) {
                            when (it.keyCode) {
                                KeyEvent.KEYCODE_VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
                                KeyEvent.KEYCODE_VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })

            isActive = true
            val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
            mediaButtonIntent.setClass(this@MusicPlayerService, MusicPlayerService::class.java)
            val pendingIntent = PendingIntent.getService(
                this@MusicPlayerService,
                0,
                mediaButtonIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            setMediaButtonReceiver(pendingIntent)
        }
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }
    private fun mute(mute: Boolean) {
        if (mute) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI
            )
        } else {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }
    private fun setVolume(volume: Int) {
        val newVolume = volume.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI)
    }
    private fun requestAudioFocus() {
        audioManager.requestAudioFocus(
            { },
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}