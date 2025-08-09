package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.MusicPlayerViewModel.AudioFile
import com.example.myapplication.MusicPlayerViewModel.Companion.getAllAudioFiles

import com.example.myapplication.utils.ExpandedSongInfoPopup
import com.example.myapplication.utils.NextSongsPopup
import com.example.myapplication.utils.SongPickerPopup
import com.example.myapplication.utils.getAllLaunchableApps
import kotlin.math.cbrt
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun MusicLauncherScreen(
    drawerOpen: Boolean,
    onToggleDrawer: () -> Unit,
    onCloseDrawer: () -> Unit,
    speedVm: SpeedViewModel = viewModel(),
    musicPlayerVM: MusicPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val apps = remember { getAllLaunchableApps(context) }

    // audioFiles başlangıçta boş, sonra güncellenecek
    var audioFiles by remember { mutableStateOf(emptyList<AudioFile>()) }

    val speed by speedVm.speedKmh.collectAsState(initial = 0f)
    val onUpdateUpcomingList: (List<AudioFile>) -> Unit = { newList ->
        musicPlayerVM.updateUpcomingList(newList)
    }
    // Buradaki vm değişkeni kaldırıldı çünkü musicPlayerVM zaten parametre olarak var

    // upcoming listesi mutableStateList olmadığı için LaunchedEffect ile takip etmek zor
    // İsterseniz bunu ViewModel'den direkt collect edebilirsiniz.
    val upcomingSongs by musicPlayerVM.upcomingSongs.collectAsState()

    val selectedAudio by musicPlayerVM.selectedAudio.collectAsState()
    val isPlaying by musicPlayerVM.isPlaying.collectAsState()
    val currentPosition by musicPlayerVM.currentPosition.collectAsState()
    val totalDuration by musicPlayerVM.totalDuration.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    // Bu listeyi de state olarak tutuyoruz, getAllAudioFiles fonksiyonundan geliyor
    var audioList by remember { mutableStateOf(getAllAudioFiles(context)) }

    var showSongPicker by remember { mutableStateOf(false) }

    val currentPlaying by musicPlayerVM.selectedAudio.collectAsState()

    LaunchedEffect(Unit) {
        audioFiles = getAllAudioFiles(context)
        musicPlayerVM.setAudioFiles(audioFiles)

        if (selectedAudio == null && audioFiles.isNotEmpty()) {
            musicPlayerVM.setAudio(audioFiles.first())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = R.drawable.carview),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(screenWidth * 0.20f)   // Genişliği ekran genişliğinin %30'u yap
                .align(Alignment.Center)
        )

        AnimatedCurvedLines(lineLengthFraction = 0.6f)

        ResponsiveRow()


        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
        ) {
            selectedAudio?.takeIf { it.title != null && it.uri != null }?.let {
                PlayerUI(
                    audio = it,
                    onOpenSongPicker = { showSongPicker = true },
                    isCompact = false,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    totalDuration = totalDuration,
                    onPlayPauseToggle = { musicPlayerVM.playPause() },
                    onSeekTo = { musicPlayerVM.seekTo(it) },
                    onDrawerToggle = onToggleDrawer,
                    onNext = { musicPlayerVM.playNext() },
                    onPrevious = { musicPlayerVM.playPrevious() }
                )
            }
        }

        if (showSongPicker) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)) // Arka plan karartması
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
                    // Şarkı Seçici Popup
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


                    // Detaylı Şarkı Bilgisi Popup
                    ExpandedSongInfoPopup(
                        selectedAudio = selectedAudio!!,
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

                    val upcomingSongs by musicPlayerVM.upcomingSongs.collectAsState()

                    // Sonraki Şarkılar Popup
                    NextSongsPopup(
                        upcoming = upcomingSongs,
                        currentPlaying = currentPlaying,
                        onListChanged = { newList ->
                            musicPlayerVM.updateUpcomingList(newList)
                        },
                        musicPlayerVM = musicPlayerVM,
                        itemHeightFraction = 0.1f,
                        fontSizeFraction = 0.035f,
                        selectedAudio = selectedAudio!!
                    )
                }
            }
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

    // Ekran genişliğinin %3'ü kadar padding (istediğin gibi ayarlayabilirsin)
    val horizontalPaddingDp = (screenWidth * 0.03f).dp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 56.dp, start = horizontalPaddingDp, end = horizontalPaddingDp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnalogClockWithNumbers(widthFraction = finalWidthFraction)
        }
        Box(contentAlignment = Alignment.Center) {
            SpeedometerScreen(widthFraction = (finalWidthFraction + 0.01f).coerceAtMost(0.5f))
        }
    }
}


