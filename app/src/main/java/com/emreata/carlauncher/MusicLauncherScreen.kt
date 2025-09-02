package com.emreata.carlauncher

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel.AudioFile
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel.Companion.getAllAudioFiles
import com.emreata.carlauncher.utils.ExpandedSongInfoPopup
import com.emreata.carlauncher.utils.NextSongsPopup
import com.emreata.carlauncher.utils.SongPickerPopup
import com.emreata.carlauncher.utils.getAllLaunchableApps
import kotlin.math.sqrt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import com.emreata.carlauncher.utils.AnimatedCurvedLines
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel
import com.emreata.carlauncher.viewmodels.SpeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.core.net.toUri

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MusicLauncherScreen(
    drawerOpen: Boolean,
    settingsOpen: Boolean,
    onToggleDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    speedVm: SpeedViewModel = viewModel(),
    musicPlayerVM: MusicPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val apps = remember { getAllLaunchableApps(context) }
    var audioFiles by remember { mutableStateOf(emptyList<AudioFile>()) }
    val selectedAudio by musicPlayerVM.selectedAudio.collectAsState()
    val isPlaying by musicPlayerVM.isPlaying.collectAsState()
    val currentPosition by musicPlayerVM.currentPosition.collectAsState()
    val totalDuration by musicPlayerVM.totalDuration.collectAsState()
    var audioList by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var showSongPicker by remember { mutableStateOf(false) }
    val currentPlaying by musicPlayerVM.selectedAudio.collectAsState()
    val upcomingSongs = musicPlayerVM.upcomingSongs
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var selectedCarView by remember {
        mutableStateOf(prefs.getString("car_view_uri", null)?.toUri())
    }
     // default drawable
    LaunchedEffect(Unit) {
        audioFiles = getAllAudioFiles(context)
        musicPlayerVM.setAudioFiles(audioFiles)
        audioList = withContext(Dispatchers.IO) {
            getAllAudioFiles(context) // burası artık arka planda çalışıyor
        }
        if (selectedAudio == null && audioFiles.isNotEmpty()) {
            musicPlayerVM.setAudio(audioFiles.first())
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {


        ResponsiveRow()

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
        ) {

                PlayerUI(
                    audio = selectedAudio,
                    onOpenSongPicker = { showSongPicker = true },
                    isCompact = false,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    totalDuration = totalDuration,
                    onPlayPauseToggle = { musicPlayerVM.playPause() },
                    onSeekTo = { musicPlayerVM.seekTo(it) },
                    onDrawerToggle = onToggleDrawer,
                    onNext = { musicPlayerVM.playNext() },
                    onPrevious = { musicPlayerVM.playPrevious() },
                    onOpenSettings = onOpenSettings,
                    onShuffleNextSongs = { musicPlayerVM.shuffleUpcomingSongs(currentPlaying) },
                )

        }

        if (showSongPicker) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showSongPicker = false
                    }
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    SongPickerPopup(
                        playlists = musicPlayerVM.playlists,
                        audioFiles = audioFiles,
                        audioList = audioList,
                        onAddPlaylist = { name -> musicPlayerVM.addPlaylist(name) },
                        onSelectAudio = { audio, fullList ->
                            musicPlayerVM.selectAudioAndPlayFromHere(audio, fullList)
                        },
                        onDismiss = { showSongPicker = false },
                        onChangePlaylistImage = { playlist, uri -> musicPlayerVM.changePlaylistImage(playlist, uri) },
                        onAddToQueue = { audio -> musicPlayerVM.addToQueue(audio) },
                        onAddToPlaylist = { audio, playlist -> musicPlayerVM.addSongToPlaylist(playlist.name, audio) },
                        onRenameSong = { audio, newName -> musicPlayerVM.renameSongFile(audio, newName) },
                        onDeleteSong = { audio -> musicPlayerVM.deleteSongFromDevice(audio) },
                        onToggleFavorite = { audio -> musicPlayerVM.toggleFavorite(audio) },
                        onPlayPlaylist = { playlist -> musicPlayerVM.playPlaylist(playlist) },
                        onDeletePlaylist = { playlist -> musicPlayerVM.deletePlaylist(playlist) },
                        onRenamePlaylist = { playlist, newName -> musicPlayerVM.renamePlaylist(playlist, newName) },
                        modifier = Modifier
                    )

                    ExpandedSongInfoPopup(
                        selectedAudio = selectedAudio ?: MusicPlayerViewModel.AudioFile(
                            title = stringResource(id = R.string.unknown_track),
                            artist = stringResource(id = R.string.unknown_artist),
                            uriString = "",
                            album = stringResource(id = R.string.unknown_album),
                            duration = 0L
                        ),
                        onDismiss = { showSongPicker = false },
                        onOpenSongPicker = { /* opsiyonel işlem */ },
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        totalDuration = totalDuration,
                        onPlayPauseToggle = { musicPlayerVM.playPause() },
                        onSeekTo = { musicPlayerVM.seekTo(it) },
                        onNext = { musicPlayerVM.playNext() },
                        onPrevious = { musicPlayerVM.playPrevious() },
                        onShuffleNextSongs = { musicPlayerVM.shuffleUpcomingSongs(selectedAudio) },
                        modifier = Modifier
                    )

                    NextSongsPopup(
                        upcoming = upcomingSongs,
                        currentPlaying = currentPlaying,
                        onListChanged = { newList ->
                            musicPlayerVM.updateUpcomingList(newList) },
                        musicPlayerVM = musicPlayerVM,
                        itemHeightFraction = 0.1f,
                        fontSizeFraction = 0.035f,
                        selectedAudio = selectedAudio ?: MusicPlayerViewModel.AudioFile(
                            title = stringResource(id = R.string.unknown_track),
                            artist = stringResource(id = R.string.unknown_artist),
                            uriString = "",
                            album = stringResource(id = R.string.unknown_album),
                            duration = 0L
                        )
                    )
                }
            }
        }

        if (settingsOpen) {
            SettingsScreen(
                onClose = { onCloseDrawer() },
                onCarViewChange = { newUri ->
                    selectedCarView = newUri
                    prefs.edit { putString("car_view_uri", newUri.toString()) }
                }
            )
        }


        if (drawerOpen) {
            CustomDrawer(
                initialApps = apps,
                onAppClick = { app ->
                    val intent =
                        context.packageManager.getLaunchIntentForPackage(app.activityInfo.packageName)
                    intent?.let { context.startActivity(it) }
                }
            )
        }
    }
}
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ResponsiveRow() {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenWidth = configuration.screenWidthDp.toFloat()
    val diag = sqrt(screenWidth * screenWidth + screenHeight * screenHeight)
    val normDiag = ((diag - 360f) / (2000f - 360f)).coerceIn(0f, 1f)
    val aspectRatio = screenWidth / screenHeight
    val aspectWeight = when {
        aspectRatio > 2f -> 0.8f
        aspectRatio in 1.5f..2f -> 1.1f
        aspectRatio in 1.0f..1.5f -> 1.0f
        else -> 0.9f
    }
    val baseFraction = 0.20f
    val widthRange = 0.15f
    val adjustedWidthFraction = (baseFraction + widthRange * sqrt(normDiag)) * aspectWeight
    val finalWidthFraction = adjustedWidthFraction.coerceIn(0.16f, 0.5f)
    val horizontalPaddingDp = (screenWidth * 0.03f).dp
    val context = LocalContext.current

    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var selectedCarView by remember {
        mutableStateOf(prefs.getString("car_view_uri", null)?.toUri())
    }
    val painter = selectedCarView?.let { uri ->
        rememberAsyncImagePainter(uri)
    } ?: painterResource(id = R.drawable.carview)

    val widthDp = with(LocalDensity.current) { (screenWidth * 0.16f).toDp() }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp, start = horizontalPaddingDp, end = horizontalPaddingDp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.CenterStart
        ) {
            AnalogClockWithNumbers(widthFraction = finalWidthFraction)
        }

        Box(
            modifier = Modifier
                .weight(1f) // kalan tüm alanı kaplasın
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedCurvedLines(
                painter = painter,
                widthDp = widthDp,
                lineLengthFraction = 0.6f,
                remainingwidth = 1f,
            )
        }

        Box(
            contentAlignment = Alignment.CenterEnd
        ) {
            SpeedometerScreen(widthFraction = finalWidthFraction)
        }
    }

}