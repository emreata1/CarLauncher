package com.example.myapplication.utils

import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myapplication.R

@Composable
fun CustomBottomBar(
    modifier: Modifier = Modifier,
    onDrawerToggle: () -> Unit,
    onOpenSongPicker: () -> Unit,
    isPlaying: Boolean = false,
    trackName: String = "Parça İsmi",
    artistName: String? = null,
    currentPosition: Long = 0L,
    totalDuration: Long = 100L,
    onPlayPauseToggle: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    fun volumeUp() = audioManager?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
    fun volumeDown() = audioManager?.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF212121))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🎵 Sol Bölüm (içerik + ikon)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1) Parça bilgisi + slider
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Üst %50
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = trackName,
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            artistName?.let {
                                Text(
                                    text = it,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // Alt %50
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        CustomCanvasSlider(
                            position = currentPosition.toFloat(),
                            onPositionChange = { onSeekTo(it.toLong()) },
                            maxDuration = totalDuration.toFloat(),
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatTime(totalDuration),
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                    }
                }

                // 2) Sağında ikon

            }
        }

        // 🎛️ Orta Bölüm
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                IconButton(
                    onClick = onOpenSongPicker,

                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent, // hiç arkaplan yok
                        contentColor = Color.White          // ripple vb. beyaz
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Şarkı Seç",
                        tint = Color.White                   // ikonu beyaz yap
                    )
                }

                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Önceki", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onPlayPauseToggle) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Durdur" else "Oynat",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Sonraki", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { volumeUp() }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Ses Aç", tint = Color.White)
                }
            }
        }

        // 📂 Sağ Bölüm
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Button(
                onClick = onDrawerToggle,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.size(70.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.appdrawer),
                    contentDescription = "App Drawer",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White // ← burası eklendi
                )
            }

        }
    }
}







@Composable
fun CompactBottomBar(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    trackName: String = "Parça İsmi",
    artistName: String? = null,
    onPlayPauseToggle: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    currentPosition: Long = 0L,
    totalDuration: Long = 100L,
    onSeekTo: (Long) -> Unit = {},
    onOpenSongPicker: () -> Unit = {},
    onShuffleNextSongs: () -> Unit = {}   // Yeni parametre
) {
    var albumArtWidth by remember { mutableStateOf(0) }
    Column(
        modifier = modifier
            .background(Color(0xFF303030))
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1) Parça bilgisi
        var columnWidth by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clickable { onOpenSongPicker() }
                .padding(horizontal = 6.dp)
                .onSizeChanged { columnWidth = it.width },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val density = LocalDensity.current
            val trackFontSize = with(density) { (columnWidth * 0.1f).toSp() }
            val artistFontSize = with(density) { (columnWidth * 0.085f).toSp() }

            Text(
                text = trackName,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = trackFontSize
            )
            artistName?.let {
                Text(
                    text = it,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = artistFontSize,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Spacer1 (Parça bilgisi ile slider arası)
        val spacer1Height = with(LocalDensity.current) { (columnWidth * 0.1f).toDp() }
        Spacer(modifier = Modifier.height(spacer1Height))

        // 2) Slider ve süreler
        var columnWidth1 by remember { mutableStateOf(0) }
        val density1 = LocalDensity.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .onSizeChanged { columnWidth1 = it.width }
        ) {
            val timeFontSize = with(density1) { (columnWidth1 * 0.035f).toSp() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    color = Color.LightGray,
                    fontSize = timeFontSize
                )
                Text(
                    text = formatDuration(totalDuration),
                    color = Color.LightGray,
                    fontSize = timeFontSize
                )
            }

            // Spacer2 (Süreler ile slider arası)
            val spacer2Height = with(density1) { (columnWidth1 * 0.02f).toDp() }
            Spacer(modifier = Modifier.height(spacer2Height))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CustomCanvasSlider(
                    position = currentPosition.toFloat(),
                    onPositionChange = { onSeekTo(it.toLong()) },
                    maxDuration = totalDuration.toFloat(),
                    modifier = Modifier.fillMaxWidth(1f)
                )
            }
        }

        // Spacer3 (Slider ile butonlar arası)
        val spacer3Height = with(LocalDensity.current) { (columnWidth1 * 0.08f).toDp() }
        Spacer(modifier = Modifier.height(spacer3Height))

        // 3) Butonlar
        var rowWidth by remember { mutableStateOf(0) }
        val density2 = LocalDensity.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowWidth = it.width },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = with(density2) { (rowWidth * 0.05f).toDp() },
                alignment = Alignment.CenterHorizontally
            )
        ) {
            val iconSize = with(density2) { (rowWidth * 0.12f).toDp() }
            IconButton(onClick = onShuffleNextSongs, modifier = Modifier.size(iconSize)) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Karıştır",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize-10.dp)
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(iconSize)) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Önceki",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }

            // Yeni Shuffle Butonu


            IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(iconSize)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Durdur" else "Oynat",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(iconSize)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Sonraki",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            IconButton(onClick = onShuffleNextSongs, modifier = Modifier.size(iconSize)) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Karıştır",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize-10.dp)
                )
            }
        }
    }
}







@Composable
fun CustomCanvasSlider(
    position: Float,
    onPositionChange: (Float) -> Unit,
    maxDuration: Float,
    height: Dp = 5.dp,
    thumbRadius: Dp = 6.dp,
    inactiveColor: Color = Color.Gray,
    activeColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    var sliderWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .zIndex(1f)
            .height((thumbRadius * 2).coerceAtLeast(height))
            .onSizeChanged { sliderWidth = it.width.toFloat() }
            .pointerInput(maxDuration, sliderWidth) {
                detectTapGestures { tapOffset ->
                    if (sliderWidth > 0f) {
                        val newX = tapOffset.x.coerceIn(0f, sliderWidth)
                        onPositionChange((newX / sliderWidth) * maxDuration)
                    }
                }
                detectDragGestures { change, _ ->
                    change.consume()
                    if (sliderWidth > 0f) {
                        val newX = change.position.x.coerceIn(0f, sliderWidth)
                        onPositionChange((newX / sliderWidth) * maxDuration)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = height.toPx()
            val centerY = size.height / 2
            val cornerRadius = strokeWidthPx / 2

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - strokeWidthPx / 2),
                size = androidx.compose.ui.geometry.Size(size.width, strokeWidthPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )

            val fraction = (position.coerceIn(0f, maxDuration)) / maxDuration
            val activeWidth = size.width * fraction

            if (activeWidth > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - strokeWidthPx / 2),
                    size = androidx.compose.ui.geometry.Size(activeWidth, strokeWidthPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            }

            drawCircle(
                color = Color(0xFFFF3B3B),
                radius = thumbRadius.toPx(),
                center = Offset(activeWidth, centerY)
            )
        }
    }
}


fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}


fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}