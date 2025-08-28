package com.emreata.carlauncher.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon

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
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel.AudioFile
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import coil.compose.rememberAsyncImagePainter
import com.emreata.carlauncher.CompactBottomBar
import com.emreata.carlauncher.R
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.abs

@Composable
fun SongPickerPopup(
    audioFiles: List<AudioFile>,
    audioList: List<AudioFile>, // Tüm listeyi parametre olarak alıyoruz
    playlists: SnapshotStateList<MusicPlayerViewModel.Playlist>,
    onAddPlaylist: (String) -> Unit,
    onSelectAudio: (AudioFile, List<AudioFile>) -> Unit,

    onDismiss: () -> Unit,
    onChangePlaylistImage: (MusicPlayerViewModel.Playlist, Uri) -> Unit,
    onAddToQueue: (AudioFile) -> Unit,
    onAddToPlaylist: (AudioFile, MusicPlayerViewModel.Playlist) -> Unit,
    onRenameSong: (AudioFile, String) -> Unit,
    onDeleteSong: (AudioFile) -> Unit,
    onToggleFavorite: (AudioFile) -> Unit,
    onPlayPlaylist: (MusicPlayerViewModel.Playlist) -> Unit,
    onDeletePlaylist: (MusicPlayerViewModel.Playlist) -> Unit,
    onRenamePlaylist: (MusicPlayerViewModel.Playlist, String) -> Unit,

    modifier: Modifier = Modifier
) {
    val allSongsName = stringResource(R.string.all_songs) // Compose içinde çağrılır

    val allPlaylist = remember(audioFiles) {
        MusicPlayerViewModel.Playlist(
            name = allSongsName,
            songs = audioFiles
        )
    }

    val playlistList by remember {
        derivedStateOf { listOf(allPlaylist) + playlists }
    }

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val baseHeightDp = 800f
    val scaleFactor = (screenHeightDp / baseHeightDp) * 1.5f
    var showRenameDialogForPlaylist by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<MusicPlayerViewModel.Playlist?>(null) }
    var selectedPlaylist by remember { mutableStateOf<MusicPlayerViewModel.Playlist?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPlaylist?.let { playlist ->
                onChangePlaylistImage(playlist, uri)
            }
        }
    }
    var menuExpandedForPlaylist by remember { mutableStateOf<MusicPlayerViewModel.Playlist?>(null) }

    Box(
        Modifier
            .fillMaxHeight()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            }
    ) {
        Card(
            modifier = modifier
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
                    .fillMaxSize()
            ) {
                if (selectedPlaylist == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(playlistList) { playlist ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPlaylist = playlist }
                                        .padding(
                                            horizontal = (8 * scaleFactor).dp,
                                            vertical = (8 * scaleFactor).dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (playlist.imageUri != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(playlist.imageUri),
                                            contentDescription = "Playlist Image",
                                            modifier = Modifier
                                                .size((60 * scaleFactor).dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                            contentDescription = null,
                                            tint = Color.LightGray,
                                            modifier = Modifier.size((60 * scaleFactor).dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f) // Text alanını genişletiyoruz
                                    ) {
                                        Text(
                                            text = if (playlist.name == "Favoriler") {
                                                stringResource(R.string.favorites)
                                            } else {
                                                playlist.name
                                            },
                                            fontSize = (14 * scaleFactor).sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.playlist_songs_count,
                                                playlist.songs.size
                                            ),
                                            fontSize = (12 * scaleFactor).sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Oynatma butonu
                                    IconButton(
                                        onClick = {
                                            onPlayPlaylist(playlist)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Oynat",
                                            tint = Color.Cyan,
                                            modifier = Modifier.size((30 * scaleFactor).dp)
                                        )
                                    }

                                    // Menü (üç nokta) butonu
                                    Box {
                                        IconButton(onClick = {
                                            // Açılır menüyü toggle yapıyoruz
                                            if (menuExpandedForPlaylist == playlist) {
                                                menuExpandedForPlaylist = null
                                            } else {
                                                menuExpandedForPlaylist = playlist
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Menü",
                                                tint = Color.White,
                                                modifier = Modifier.size((30 * scaleFactor).dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpandedForPlaylist == playlist,
                                            onDismissRequest = { menuExpandedForPlaylist = null }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.delete_playlist)) },
                                                onClick = {
                                                    menuExpandedForPlaylist = null
                                                    onDeletePlaylist(playlist)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.rename_playlist)) },
                                                onClick = {
                                                    menuExpandedForPlaylist = null
                                                    newPlaylistName = playlist.name
                                                    selectedPlaylist = null
                                                    showRenameDialogForPlaylist = true
                                                    playlistToRename = playlist
                                                }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = (8 * scaleFactor).dp),
                                    thickness = (1 * scaleFactor).dp,
                                    color = Color.DarkGray.copy(alpha = 0.6f)
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Yeni Oynatma Listesi")
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.back),
                                color = Color.Cyan,
                                fontSize = (16 * scaleFactor).sp,
                                modifier = Modifier
                                    .clickable { selectedPlaylist = null }
                                    .padding(8.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = selectedPlaylist!!.name,
                                color = Color.White,
                                fontSize = (18 * scaleFactor).sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Box {
                                IconButton(onClick = { menuExpandedForPlaylist = selectedPlaylist }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpandedForPlaylist == selectedPlaylist,
                                    onDismissRequest = { menuExpandedForPlaylist = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.change_image)) },
                                        onClick = {
                                            menuExpandedForPlaylist = null
                                            imagePickerLauncher.launch("image/*")
                                        }
                                    )
                                }
                            }
                        }

                        val songs = selectedPlaylist!!.songs
                        if (songs.isEmpty()) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_songs_in_playlist),
                                    color = Color.LightGray,
                                    fontSize = (14 * scaleFactor).sp
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(songs) { audio ->
                                    AudioFileItem(
                                        audioFile = audio,
                                        audioList = songs,                // <<< burada sadece o playlist'in şarkı listesi
                                        playlists = playlists,
                                        onAddToQueue = onAddToQueue,
                                        onAddToPlaylist = onAddToPlaylist,
                                        onRename = onRenameSong,
                                        onDelete = onDeleteSong,
                                        onSelectAudio = onSelectAudio,    // aynı 2-parametreli fonksiyonu geçir
                                        onToggleFavorite = onToggleFavorite,
                                        modifier = Modifier
                                            .padding(vertical = (4 * scaleFactor).dp, horizontal = (4 * scaleFactor).dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                onAddPlaylist(it)
                showAddDialog = false
            }
        )
    }

    if (showRenameDialogForPlaylist && playlistToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialogForPlaylist = false },
            title = { Text("Oynatma Listesini Yeniden Adlandır") },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Yeni İsim") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        onRenamePlaylist(playlistToRename!!, newPlaylistName)
                    }
                    showRenameDialogForPlaylist = false
                    playlistToRename = null
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialogForPlaylist = false
                    playlistToRename = null
                }) {
                    Text("İptal")
                }
            }
        )
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

    // 🔹 Artık kendi "list" state'ini tutmuyoruz, direkt upcoming ile çalışıyoruz
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
                            .pointerInput(key) {
                                detectDragGestures(
                                    onDragStart = { draggingItemUri = audio.uri },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                            swipeOffsetMap[key] = (swipeOffsetMap[key] ?: 0f) + dragAmount.x
                                        } else {
                                            scope.launch { listState.scrollBy(-dragAmount.y) }
                                        }
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
                                    modifier = Modifier.size(fontSizePx.dp)
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





@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistName by remember { mutableStateOf("") }

    Box(
        modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color(0xFF303030)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.create_new_playlist),
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { stringResource(R.string.playlist_name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(stringResource(R.string.cancel), color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                onConfirm(playlistName.trim())
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add), color = Color.Cyan)
                    }
                }
            }
        }
    }
}


@Composable
fun AudioFileItem(
    modifier: Modifier = Modifier,
    audioFile: AudioFile,
    audioList: List<AudioFile>,
    playlists: List<MusicPlayerViewModel.Playlist>,
    onAddToQueue: (AudioFile) -> Unit,
    onAddToPlaylist: (AudioFile, MusicPlayerViewModel.Playlist) -> Unit,
    onRename: (AudioFile, String) -> Unit,
    onDelete: (AudioFile) -> Unit,
    onSelectAudio: (selectedAudio: AudioFile, fullList: List<AudioFile>) -> Unit,
    onToggleFavorite: (AudioFile) -> Unit,
    scaleFactor: Float = 1f
) {
    val context = LocalContext.current
    val bitmap = remember(audioFile.uri) { getAlbumArt(context, audioFile.uri) }

    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(audioFile.title) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val isFavorite = playlists.find { it.name == "Favoriler" }
        ?.songs?.any { it.uri == audioFile.uri } == true

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelectAudio(audioFile, audioList)
                }
                .padding(horizontal = (4 * scaleFactor).dp, vertical = (4 * scaleFactor).dp)
        ) {
            AlbumArtImage(
                bitmap = bitmap,
                modifier = Modifier.size((66 * scaleFactor).dp)
            )

            Spacer(modifier = Modifier.width((8 * scaleFactor).dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = audioFile.title,
                    color = Color.White,
                    fontSize = (20 * scaleFactor).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = audioFile.artist ?: stringResource(R.string.unknown_artist),
                    color = Color.LightGray,
                    fontSize = (11 * scaleFactor).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onToggleFavorite(audioFile) }) {
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = stringResource(R.string.remove_from_favorites),
                        tint = Color.Red
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.add_to_favorites),
                        tint = Color.White
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.song_menu),
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_queue)) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue(audioFile)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_playlist)) },
                        onClick = {
                            menuExpanded = false
                            showPlaylistDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename_song)) },
                        onClick = {
                            menuExpanded = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_song)) },
                        onClick = {
                            menuExpanded = false
                            onDelete(audioFile)
                        }
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = (4 * scaleFactor).dp),
            thickness = (1 * scaleFactor).dp,
            color = Color.DarkGray.copy(alpha = 0.6f)
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { stringResource(R.string.rename_song) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Yeni İsim") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && newName != audioFile.title) {
                        onRename(audioFile, newName)
                    }
                    showRenameDialog = false
                }) {
                    stringResource(R.string.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    stringResource(R.string.cancel)
                }
            }
        )
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { stringResource(R.string.add_to_playlist) },
            text = {
                LazyColumn {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddToPlaylist(audioFile, playlist)
                                    showPlaylistDialog = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    stringResource(R.string.cancel)
                }
            }
        )
    }
}
