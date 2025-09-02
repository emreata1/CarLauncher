package com.emreata.carlauncher.utils

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emreata.carlauncher.R
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel.AudioFile
import kotlinx.coroutines.launch
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NextSongsPopup(
    upcoming: List<AudioFile>,
    currentPlaying: AudioFile?,
    onListChanged: (List<AudioFile>) -> Unit,
    musicPlayerVM: MusicPlayerViewModel,
    modifier: Modifier = Modifier,
    itemHeightFraction: Float = 0.05f,
    fontSizeFraction: Float = 0.03f,
    selectedAudio: AudioFile
) {
    val context = LocalContext.current
    val swipeOffsetMap = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(upcoming) {
        val keys = upcoming.map { it.uri.toString() }.toSet()
        keys.forEach { if (it !in swipeOffsetMap) swipeOffsetMap[it] = 0f }
        swipeOffsetMap.keys.filter { it !in keys }.forEach { swipeOffsetMap.remove(it) }
    }

    var draggingItemUri by remember { mutableStateOf<Uri?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isRemoving by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    var parentHeightPx by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(currentPlaying, upcoming) {
        val idx = upcoming.indexOfFirst { it.uri == currentPlaying?.uri }
        if (idx != -1) listState.animateScrollToItem(idx)
    }

    fun moveItem(from: Int, to: Int) {
        if (from == to) return
        if (from !in upcoming.indices) return
        val newList = upcoming.toMutableList()
        val item = newList.removeAt(from)
        val insertIdx = to.coerceIn(0, newList.size)
        newList.add(insertIdx, item)
        onListChanged(newList)
    }

    fun removeItemByIndex(index: Int) {
        if (isRemoving) return
        if (index !in upcoming.indices) return
        isRemoving = true
        val removed = upcoming[index]
        val newList = upcoming.toMutableList().apply { removeAt(index) }
        swipeOffsetMap.remove(removed.uri.toString())
        onListChanged(newList)

        if (removed.uri == currentPlaying?.uri) {
            val newIndex = index.coerceAtMost(newList.lastIndex)
            val next = newList.getOrNull(newIndex) ?: newList.getOrNull(newIndex - 1)
            if (next != null) musicPlayerVM.setAudio(next, forcePlay = true) else musicPlayerVM.stopAudio()
        }

        if (draggingItemUri == removed.uri) {
            draggingItemUri = null
            dragOffsetY = 0f
        }
        isRemoving = false
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 56.dp)
            .onSizeChanged { parentHeightPx = it.height },
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
                text = stringResource(R.string.next_songs),
                color = Color.White,
                fontSize = fontSizeSp * 1.5,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(state = listState) {
                itemsIndexed(upcoming, key = { _, audio -> audio.uri.toString() }) { index, audio ->
                    val key = audio.uri.toString()
                    val currentOffset = swipeOffsetMap[key] ?: 0f
                    val isDragging = draggingItemUri == audio.uri
                    val offsetY = if (isDragging) dragOffsetY.roundToInt() else 0
                    val swipeThreshold = with(density) { 100.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .offset { IntOffset(currentOffset.roundToInt(), offsetY) }
                            .background(if (isDragging) Color.DarkGray else Color.Transparent)
                            .pointerInput(audio.uri) {
                                detectDragGestures(
                                    onDragStart = { draggingItemUri = audio.uri },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                            swipeOffsetMap[key] = (swipeOffsetMap[key] ?: 0f) + dragAmount.x
                                        } else {
                                            scope.launch { listState.scrollBy(-dragAmount.y) }                                        }
                                    },
                                    onDragEnd = {
                                        if ((swipeOffsetMap[key] ?: 0f) > swipeThreshold) {
                                            removeItemByIndex(index)
                                        } else {
                                            swipeOffsetMap[key] = 0f
                                        }
                                        draggingItemUri = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        swipeOffsetMap[key] = 0f
                                        draggingItemUri = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                    ) {
                        val swipeWidth = (swipeOffsetMap[key] ?: 0f)
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
                                .offset { IntOffset(swipeWidth.roundToInt(), 0) }
                                .background(if (isDragging) Color.DarkGray else Color.Transparent)
                                .padding(horizontal = 8.dp)
                                .clickable { musicPlayerVM.setAudio(audio, forcePlay = true) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(audio.uri) { getAlbumArt(context, audio.uri) }
                            AlbumArtImage(bitmap = bitmap, modifier = Modifier.size(itemHeightDp * 0.9f))

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = audio.title,
                                color = if (audio.uri == currentPlaying?.uri) Color.Cyan else Color.LightGray,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = fontSizeSp
                            )

                            Box(
                                modifier = Modifier.pointerInput(key) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingItemUri = audio.uri; dragOffsetY = 0f },
                                        onDrag = { change, dragAmount ->
                                            if (draggingItemUri == audio.uri) {
                                                dragOffsetY += dragAmount.y
                                                change.consume()
                                                val fromIndex = upcoming.indexOfFirst { it.uri == audio.uri }
                                                val offsetIndex = (dragOffsetY / itemHeightPx).toInt()
                                                val toIndex = (fromIndex + offsetIndex).coerceIn(0, upcoming.lastIndex)
                                                if (fromIndex != toIndex) {
                                                    moveItem(fromIndex, toIndex)
                                                    dragOffsetY -= offsetIndex * itemHeightPx
                                                    scope.launch {
                                                        if (toIndex == 0) listState.scrollToItem(0)
                                                        else if (toIndex == upcoming.lastIndex) listState.scrollToItem(upcoming.lastIndex)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = { draggingItemUri = null; dragOffsetY = 0f },
                                        onDragCancel = { draggingItemUri = null; dragOffsetY = 0f }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Sürükle",
                                    tint = Color.White,
                                    modifier = Modifier.size(fontSizePx.dp+8.dp)
                                )
                            }
                        }
                    }

                    if (index < upcoming.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}