package com.example.myapplication

import androidx.compose.runtime.*
import com.example.myapplication.utils.CompactBottomBar
import com.example.myapplication.utils.CustomBottomBar
import com.example.myapplication.MusicPlayerViewModel

@Composable
fun PlayerUI(
    audio: MusicPlayerViewModel.AudioFile,  // Bu şekilde kullan
    isCompact: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDrawerToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSongPicker: (() -> Unit)? = null,
) {
    if (isCompact) {
        CompactBottomBar(
            isPlaying = isPlaying,
            trackName = audio.title,
            artistName = audio.artist,
            onPlayPauseToggle = onPlayPauseToggle,
            onNext = onNext,
            onPrevious = onPrevious,
            currentPosition = currentPosition,
            totalDuration = totalDuration,
            onOpenSongPicker = onOpenSongPicker ?: {}
        )
    } else {
        CustomBottomBar(
            onDrawerToggle = onDrawerToggle,
            onOpenSongPicker = onOpenSongPicker ?: {},
            isPlaying = isPlaying,
            trackName = audio.title,
            artistName = audio.artist,
            currentPosition = currentPosition,
            totalDuration = totalDuration,
            onPlayPauseToggle = onPlayPauseToggle,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeekTo = onSeekTo,
        )
    }
}
