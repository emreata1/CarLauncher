package com.emreata.carlauncher.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emreata.carlauncher.CompactBottomBar
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel.AudioFile

@Composable
fun ExpandedSongInfoPopup(
    selectedAudio: AudioFile,
    onDismiss: () -> Unit,
    onOpenSongPicker: () -> Unit,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleNextSongs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier
            .fillMaxWidth(0.4f)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        Card(
            modifier = modifier
                .fillMaxHeight()
                .align(Alignment.Center)
                .padding(bottom = 56.dp)
                .zIndex(1f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 12.dp,
                pressedElevation = 16.dp,
                focusedElevation = 14.dp,
                hoveredElevation = 14.dp
            )
        ) {

            Column(
                modifier = Modifier
                    .background(Color(0xFF303030))
                    .padding(16.dp)
                    .fillMaxHeight(), // Column'un yüksekliği ekranı kapsasın
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Dikeyde ortala
            ) {
                Spacer(modifier = modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(selectedAudio.uri) {
                        getAlbumArt(context, selectedAudio.uri)
                    }
                    AlbumArtImage(
                        bitmap = bitmap,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .weight(0.4f)
                ) {
                    CompactBottomBar(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(horizontal = 8.dp),
                        isPlaying = isPlaying,
                        trackName = selectedAudio.title,
                        artistName = selectedAudio.artist,
                        currentPosition = currentPosition,
                        totalDuration = totalDuration,
                        onPlayPauseToggle = onPlayPauseToggle,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeekTo = onSeekTo,
                        onOpenSongPicker = onOpenSongPicker,
                        onShuffleNextSongs = onShuffleNextSongs
                    )
                }
            }
        }
    }
}