package com.astechsoft.carlauncher.utils

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
import com.astechsoft.carlauncher.viewmodels.MusicPlayerViewModel.AudioFile
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import coil.compose.rememberAsyncImagePainter
import com.astechsoft.carlauncher.CompactBottomBar
import com.astechsoft.carlauncher.viewmodels.MusicPlayerViewModel
import kotlin.math.roundToInt

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
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val baseHeightDp = 800f
    val scaleFactor = (screenHeightDp / baseHeightDp) * 1.5f
    val context = LocalContext.current
    val allPlaylist = remember(audioFiles) { MusicPlayerViewModel.Playlist("Tüm Şarkılar", audioFiles) }
    val playlistList by remember {
        derivedStateOf { listOf(allPlaylist) + playlists }
    }
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
                                        .padding(horizontal = (8 * scaleFactor).dp, vertical = (8 * scaleFactor).dp),
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
                                            imageVector = Icons.Default.PlaylistPlay,
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
                                            text = playlist.name,
                                            color = Color.White,
                                            fontSize = (18 * scaleFactor).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Oynatma Listesi: ${playlist.songs.size} parça",
                                            color = Color.Gray,
                                            fontSize = (12 * scaleFactor).sp,
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
                                                text = { Text("Oynatma Listesini Sil") },
                                                onClick = {
                                                    menuExpandedForPlaylist = null
                                                    onDeletePlaylist(playlist)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Yeniden Adlandır") },
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

                                Divider(
                                    color = Color.DarkGray.copy(alpha = 0.6f),
                                    thickness = (1 * scaleFactor).dp,
                                    modifier = Modifier.padding(horizontal = (8 * scaleFactor).dp)
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
                                text = "< Geri",
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
                                        text = { Text("Resmi Değiştir") },
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
                                    "Bu oynatma listesinde şarkı yok.",
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
            var albumArtWidth by remember { mutableIntStateOf(0) }

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
    itemHeightFraction: Float = 0.05f,
    fontSizeFraction: Float = 0.03f,
    selectedAudio: AudioFile
) {
    val context = LocalContext.current
    val list = remember(upcoming) { mutableStateListOf<AudioFile>().apply { addAll(upcoming) } }
    val swipeOffsetX = remember { mutableStateListOf<Float>().apply { repeat(list.size) { add(0f) } } }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var gestureDirection by remember { mutableStateOf<Direction?>(null) }
    var isRemoving by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    var parentHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(list.size) {
        if (swipeOffsetX.size != list.size) {
            swipeOffsetX.clear()
            swipeOffsetX.addAll(List(list.size) { 0f })
        }
    }

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
                                .padding(horizontal = 8.dp)
                            .clickable {
                            musicPlayerVM.setAudio(audio, forcePlay = true)
                        },
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
                    text = "Yeni Oynatma Listesi Oluştur",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Liste Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("İptal", color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                onConfirm(playlistName.trim())
                            }
                        }
                    ) {
                        Text("Ekle", color = Color.Cyan)
                    }
                }
            }
        }
    }
}


@Composable
fun AudioFileItem(
    audioFile: AudioFile,
    audioList: List<AudioFile>,
    playlists: List<MusicPlayerViewModel.Playlist>,
    onAddToQueue: (AudioFile) -> Unit,
    onAddToPlaylist: (AudioFile, MusicPlayerViewModel.Playlist) -> Unit,
    onRename: (AudioFile, String) -> Unit,
    onDelete: (AudioFile) -> Unit,
    onSelectAudio: (selectedAudio: AudioFile, fullList: List<AudioFile>) -> Unit,
    onToggleFavorite: (AudioFile) -> Unit,
    scaleFactor: Float = 1f,
    modifier: Modifier = Modifier
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
                    text = audioFile.artist ?: "Bilinmeyen Sanatçı",
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
                        contentDescription = "Favorilerden Kaldır",
                        tint = Color.Red
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorilere Ekle",
                        tint = Color.White
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Şarkı Menüsü",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sıraya Ekle") },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue(audioFile)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Oynatma Listesine Ekle") },
                        onClick = {
                            menuExpanded = false
                            showPlaylistDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Yeniden İsimlendir") },
                        onClick = {
                            menuExpanded = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cihazdan Sil") },
                        onClick = {
                            menuExpanded = false
                            onDelete(audioFile)
                        }
                    )
                }
            }
        }

        Divider(
            color = Color.DarkGray.copy(alpha = 0.6f),
            thickness = (1 * scaleFactor).dp,
            modifier = Modifier.padding(top = (4 * scaleFactor).dp)
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Şarkı Adını Değiştir") },
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
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Oynatma Listesine Ekle") },
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
                    Text("İptal")
                }
            }
        )
    }
}





