package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
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

    private val _upcomingSongs = MutableStateFlow<List<AudioFile>>(emptyList())
    val upcomingSongs: StateFlow<List<AudioFile>> = _upcomingSongs

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
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    handler.removeCallbacks(updatePositionRunnable)
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

    fun playPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                handler.removeCallbacks(updatePositionRunnable)
            } else {
                it.start()
                _isPlaying.value = true
                handler.post(updatePositionRunnable)
            }
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

    fun playNext() {
        val currentAudio = _selectedAudio.value ?: return
        val upcomingList = _upcomingSongs.value
        val upcomingIndex = upcomingList.indexOfFirst { it.uri == currentAudio.uri }
        if (upcomingIndex != -1) {
            val nextIndex = (upcomingIndex + 1) % upcomingList.size
            setAudio(upcomingList[nextIndex], forcePlay = true)
        }
    }

    fun playPrevious() {
        val currentAudio = _selectedAudio.value ?: return
        val upcomingList = _upcomingSongs.value
        val upcomingIndex = upcomingList.indexOfFirst { it.uri == currentAudio.uri }

        if (upcomingIndex > 0) {
            val previousAudio = upcomingList[upcomingIndex - 1]
            setAudio(previousAudio, forcePlay = true)
        } else {
            stopAudio()
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
        releasePlayer()
    }

    data class AudioFile(
        val title: String,
        val artist: String?,
        val uri: Uri,
        val album: String? = null,
        val duration: Long? = null // milisaniye cinsinden süre
    )

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
                    val artist = it.getString(artistIndex)
                    val album = it.getString(albumIndex)
                    val duration = it.getLong(durationIndex)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    audioList.add(AudioFile(title, artist, contentUri, album, duration))
                }
            }
            return audioList
        }
    }
    fun shuffleUpcomingSongs(currentPlaying: AudioFile?) {
        currentPlaying ?: return

        val current = currentPlaying
        val rest = _upcomingSongs.value.filter { it.uri != current.uri }.shuffled()
        _upcomingSongs.value = listOf(current) + rest
    }

}
