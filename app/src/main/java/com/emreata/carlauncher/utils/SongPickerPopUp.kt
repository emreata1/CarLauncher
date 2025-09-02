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
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel.AudioFile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import com.emreata.carlauncher.R
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel

@Composable
fun SongPickerPopup(
    audioFiles: List<AudioFile>,
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
    modifier: Modifier = Modifier,
    audioList: List<AudioFile>
) {
    val allSongsName = stringResource(R.string.all_songs)
    val allPlaylist = remember(audioFiles) {
        MusicPlayerViewModel.Playlist(name = allSongsName, songs = audioFiles)
    }
    val playlistList = remember(audioFiles, playlists) {
        listOf(allPlaylist) + playlists
    }

    val configuration = LocalConfiguration.current
    val scaleFactor = (configuration.screenHeightDp / 800f) * 1.5f

    var selectedPlaylist by remember { mutableStateOf<MusicPlayerViewModel.Playlist?>(null) }
    var menuExpandedForPlaylist by remember { mutableStateOf<MusicPlayerViewModel.Playlist?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<MusicPlayerViewModel.Playlist?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedPlaylist?.let { pl -> onChangePlaylistImage(pl, uri) } }
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
                    // Playlist listesi
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(playlistList) { _, playlist ->
                                PlaylistRow(
                                    playlist = playlist,
                                    scaleFactor = scaleFactor,
                                    onSelect = { selectedPlaylist = playlist },
                                    onPlay = onPlayPlaylist,
                                    onDelete = onDeletePlaylist,
                                    onRename = { p ->
                                        playlistToRename = p
                                        newPlaylistName = p.name
                                        showRenameDialog = true
                                    },
                                    onMenuClick = { menuExpandedForPlaylist = it },
                                    menuExpanded = menuExpandedForPlaylist == playlist
                                )
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
                    // Playlist detay
                    PlaylistDetailView(
                        playlist = selectedPlaylist!!,
                        scaleFactor = scaleFactor,
                        playlists = playlists,
                        onBack = { selectedPlaylist = null },
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onRenameSong = onRenameSong,
                        onDeleteSong = onDeleteSong,
                        onSelectAudio = onSelectAudio,
                        onToggleFavorite = onToggleFavorite,
                        onChangePlaylistImage = { imagePickerLauncher.launch("image/*") },
                        onMenuToggle = { menuExpandedForPlaylist = selectedPlaylist }
                    )
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

    if (showRenameDialog && playlistToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
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
                    showRenameDialog = false
                    playlistToRename = null
                    newPlaylistName = ""
                }) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    playlistToRename = null
                    newPlaylistName = ""
                }) { Text("İptal") }
            }
        )
    }
}

@Composable
fun PlaylistRow(
    playlist: MusicPlayerViewModel.Playlist,
    scaleFactor: Float,
    onSelect: (MusicPlayerViewModel.Playlist) -> Unit,
    onPlay: (MusicPlayerViewModel.Playlist) -> Unit,
    onDelete: (MusicPlayerViewModel.Playlist) -> Unit,
    onRename: (MusicPlayerViewModel.Playlist) -> Unit,
    onMenuClick: (MusicPlayerViewModel.Playlist) -> Unit,
    menuExpanded: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(playlist) }
            .padding(horizontal = (8 * scaleFactor).dp, vertical = (8 * scaleFactor).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val painter = playlist.imageUri?.let { rememberAsyncImagePainter(it) }
        if (painter != null) {
            Image(
                painter = painter,
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (playlist.name == "Favoriler") stringResource(R.string.favorites) else playlist.name,
                fontSize = (14 * scaleFactor).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.playlist_songs_count, playlist.songs.size),
                fontSize = (12 * scaleFactor).sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { onPlay(playlist) }) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Oynat",
                tint = Color.Cyan,
                modifier = Modifier.size((30 * scaleFactor).dp)
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menü",
                    tint = Color.White,
                    modifier = Modifier.size((30 * scaleFactor).dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(text = { Text(stringResource(R.string.delete_playlist)) }, onClick = { onDelete(playlist) })
                DropdownMenuItem(text = { Text(stringResource(R.string.rename_playlist)) }, onClick = { onRename(playlist) })
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: MusicPlayerViewModel.Playlist,
    scaleFactor: Float,
    playlists: SnapshotStateList<MusicPlayerViewModel.Playlist>,
    onBack: () -> Unit,
    onAddToQueue: (AudioFile) -> Unit,
    onAddToPlaylist: (AudioFile, MusicPlayerViewModel.Playlist) -> Unit,
    onRenameSong: (AudioFile, String) -> Unit,
    onDeleteSong: (AudioFile) -> Unit,
    onSelectAudio: (AudioFile, List<AudioFile>) -> Unit,
    onToggleFavorite: (AudioFile) -> Unit,
    onChangePlaylistImage: () -> Unit,
    onMenuToggle: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = playlist.name,
                color = Color.White,
                fontSize = (18 * scaleFactor).sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                )  {
                    DropdownMenuItem(text = { Text(stringResource(R.string.change_image)) }, onClick = { onChangePlaylistImage() })
                }
            }
        }

        val songs = playlist.songs
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(songs) { _, audio ->
                    AudioFileItem(
                        audioFile = audio,
                        audioList = songs,
                        playlists = playlists,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onRename = onRenameSong,
                        onDelete = onDeleteSong,
                        onSelectAudio = onSelectAudio,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.padding(vertical = (4 * scaleFactor).dp, horizontal = (4 * scaleFactor).dp)
                    )
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
