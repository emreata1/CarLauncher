package com.example.myapplication.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myapplication.AlbumArtImage
import com.example.myapplication.MusicPlayerViewModel.AudioFile
import com.example.myapplication.getAlbumArt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.example.myapplication.MusicPlayerViewModel
import kotlin.math.roundToInt


@Composable
fun SongPickerPopup(
    audioFiles: List<AudioFile>,
    onSelect: (AudioFile) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    // Yükseklik ve genişlik üzerinden ölçek faktörü
    val baseHeightDp = 800f // referans cihaz yüksekliği
    val scaleFactor = (screenHeightDp / baseHeightDp)*1.5

    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredList = remember(searchQuery, audioFiles) {
        if (searchQuery.isBlank()) audioFiles
        else audioFiles.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        Modifier
            .fillMaxHeight()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.375f)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .padding(bottom = 56.dp),

                    shape = RoundedCornerShape((16 * scaleFactor).dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                Modifier
                    .background(Color(0xFF303030))
                    .padding(12.dp)
                    .clickable(enabled = false) {}
            ) {
                if (filteredList.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Şarkı bulunamadı",
                            color = Color.LightGray,
                            fontSize = (14 * scaleFactor).sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList) { audio ->
                            val bitmap = remember(audio.uri) {
                                getAlbumArt(context, audio.uri)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(audio) }
                                    .padding(
                                        horizontal = (4 * scaleFactor).dp,
                                        vertical = (4 * scaleFactor).dp
                                    )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                                    AlbumArtImage(
                                        bitmap = bitmap,
                                        modifier = Modifier.size((48 * scaleFactor).dp)
                                    )
                                    Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                                    Column {
                                        Text(
                                            text = audio.title,
                                            color = Color.White,
                                            fontSize = (16 * scaleFactor).sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = audio.artist ?: "Bilinmeyen Sanatçı",
                                            color = Color.LightGray,
                                            fontSize = (11 * scaleFactor).sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Divider(
                                    color = Color.DarkGray.copy(alpha = 0.6f),
                                    thickness = (1 * scaleFactor).dp,
                                    modifier = Modifier.padding(top = (4 * scaleFactor).dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


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
            .fillMaxHeight()
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
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            var albumArtWidth by remember { mutableStateOf(0) }

            Column(
                Modifier
                    .background(Color(0xFF303030))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .onSizeChanged { albumArtWidth = it.width }
                        .height(with(LocalDensity.current) { (albumArtWidth * 0.05f).toDp() })
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
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

                Spacer(Modifier.height(16.dp))

                CompactBottomBar(
                    modifier = Modifier.fillMaxWidth(0.9f),
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

enum class Direction {
    Horizontal, Vertical
}

private var isRemoving = false

@Composable
fun NextSongsPopup(
    upcoming: List<AudioFile>,
    currentPlaying: AudioFile?,
    onListChanged: (List<AudioFile>) -> Unit,
    musicPlayerVM: MusicPlayerViewModel,
    modifier: Modifier = Modifier,
    itemHeightFraction: Float = 0.05f,   // Toplam yüksekliğin %5’i gibi
    fontSizeFraction: Float = 0.03f,
    selectedAudio: AudioFile   // Toplam yüksekliğin %3’ü gibi (örnek)
) {
    val context = LocalContext.current

    // Burada remember parametre olarak upcoming verildi, böylece liste güncellenince state de yenilenir
    val list = remember(upcoming) { mutableStateListOf<AudioFile>().apply { addAll(upcoming) } }

    val swipeOffsetX = remember { mutableStateListOf<Float>().apply { repeat(list.size) { add(0f) } } }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var gestureDirection by remember { mutableStateOf<Direction?>(null) }
    var isRemoving by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val listState = rememberLazyListState()

    var parentHeightPx by remember { mutableStateOf(0) }

    // Eğer liste büyüklüğü değişirse swipeOffsetX da güncellensin
    LaunchedEffect(list.size) {
        if (swipeOffsetX.size != list.size) {
            swipeOffsetX.clear()
            swipeOffsetX.addAll(List(list.size) { 0f })
        }
    }

    // currentPlaying değiştiğinde, listede varsa o elemana scroll yap
    LaunchedEffect(currentPlaying, list) {
        val index = list.indexOfFirst { it.uri == currentPlaying?.uri }
        if (index != -1) {
            listState.animateScrollToItem(index)
        }
    }

    fun swapItems(from: Int, to: Int) {
        if (from == to) return
        list.apply {
            add(to, removeAt(from))
        }
        onListChanged(list.toList())
        draggingIndex = to
        dragOffsetY = 0f
    }

    fun removeItem(index: Int) {
        if (isRemoving) return
        if (index !in list.indices) return

        isRemoving = true

        val removedItem = list[index]
        val isRemovedCurrentlyPlaying = removedItem.uri == currentPlaying?.uri

        list.removeAt(index)
        onListChanged(list.toList())

        swipeOffsetX.clear()
        swipeOffsetX.addAll(List(list.size) { 0f })

        if (isRemovedCurrentlyPlaying) {
            val newIndex = index.coerceAtMost(list.lastIndex)
            val nextAudio = list.getOrNull(newIndex) ?: list.getOrNull(newIndex - 1)

            if (nextAudio != null) {
                musicPlayerVM.setAudio(nextAudio, forcePlay = true)
            } else {
                musicPlayerVM.stopAudio()
            }
        }

        isRemoving = false
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 56.dp)
            .onSizeChanged { parentHeightPx = it.height },  // Parent yüksekliği piksel olarak alınıyor
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        val itemHeightPx = parentHeightPx * itemHeightFraction
        val fontSizePx = parentHeightPx * fontSizeFraction
        val fontSizeSp = with(density) { fontSizePx.toSp() }

        val itemHeightDp = with(density) { itemHeightPx.toDp() }
        Column(
            Modifier
                .background(Color(0xFF303030))
                .padding(12.dp)
                .fillMaxHeight()
        ) {
            Text(
                "Sıradaki Şarkılar",
                color = Color.White,
                fontSize = fontSizeSp * 1.5,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(state = listState) {
                itemsIndexed(list, key = { _, audio -> audio.uri }) { index, audio ->
                    val isDragging = draggingIndex == index
                    val offsetY = if (isDragging) dragOffsetY.roundToInt() else 0
                    val swipeThreshold = with(density) { 50.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .offset { IntOffset(swipeOffsetX.getOrNull(index)?.roundToInt() ?: 0, offsetY) }
                            .background(if (isDragging) Color.DarkGray else Color.Transparent)
                            .pointerInput(index) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                        gestureDirection = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (gestureDirection == null) {
                                            gestureDirection = if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                                Direction.Horizontal
                                            } else {
                                                Direction.Vertical
                                            }
                                        }

                                        if (gestureDirection == Direction.Horizontal) {
                                            val newOffset = (swipeOffsetX.getOrNull(index) ?: 0f) + dragAmount.x
                                            swipeOffsetX[index] = newOffset.coerceAtLeast(0f)
                                        } else if (gestureDirection == Direction.Vertical) {
                                            dragOffsetY += dragAmount.y
                                            val newPosition = draggingIndex + (dragOffsetY / itemHeightPx).roundToInt()
                                            if (newPosition in list.indices && newPosition != draggingIndex) {
                                                swapItems(draggingIndex, newPosition)
                                            }
                                        }

                                        change.consume()
                                    },
                                    onDragEnd = {
                                        if (gestureDirection == Direction.Horizontal) {
                                            if ((swipeOffsetX.getOrNull(index) ?: 0f) > swipeThreshold) {
                                                removeItem(index)
                                            } else {
                                                swipeOffsetX[index] = 0f
                                            }
                                        }
                                        draggingIndex = -1
                                        dragOffsetY = 0f
                                        gestureDirection = null
                                    },
                                    onDragCancel = {
                                        swipeOffsetX[index] = 0f
                                        draggingIndex = -1
                                        dragOffsetY = 0f
                                        gestureDirection = null
                                    }
                                )
                            }
                    ) {
                        val swipeWidth = swipeOffsetX.getOrNull(index) ?: 0f

                        if (swipeWidth > 0f) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(with(density) { swipeWidth.toDp() })
                                    .background(Color.Red),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    tint = Color.White,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(swipeWidth.roundToInt(), offsetY) }
                                .background(if (isDragging) Color.DarkGray else Color.Transparent)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(audio.uri) {
                                getAlbumArt(context, audio.uri)
                            }
                            AlbumArtImage(
                                bitmap = bitmap,
                                modifier = Modifier.size(itemHeightDp * 0.9f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = audio.title,
                                color = if (audio.uri == currentPlaying?.uri) Color.Cyan else Color.LightGray,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = fontSizeSp
                            )
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Sürükle",
                                tint = Color.White,
                                modifier = Modifier.size(fontSizePx.dp)
                            )
                        }

                    }

                    if (index < list.lastIndex) {
                        Divider(
                            color = Color.Gray.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                    }
                }
            }

        }
    }
}








