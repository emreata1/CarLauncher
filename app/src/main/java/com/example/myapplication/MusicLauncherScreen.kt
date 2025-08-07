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

    // Buradaki vm değişkeni kaldırıldı çünkü musicPlayerVM zaten parametre olarak var

    // upcoming listesi mutableStateList olmadığı için LaunchedEffect ile takip etmek zor
    // İsterseniz bunu ViewModel'den direkt collect edebilirsiniz.
    val upcoming by remember { mutableStateOf(musicPlayerVM.upcomingSongs) }

    val selectedAudio by musicPlayerVM.selectedAudio.collectAsState()
    val isPlaying by musicPlayerVM.isPlaying.collectAsState()
    val currentPosition by musicPlayerVM.currentPosition.collectAsState()
    val totalDuration by musicPlayerVM.totalDuration.collectAsState()

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
                .size(180.dp)
                .align(Alignment.Center)
        )

        AnimatedCurvedLines()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp, start = 60.dp, end = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.58f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                AnalogClockWithNumbers(heightFraction = 0.57f)
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.58f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                SpeedometerScreen()
            }
        }

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
                        audioFiles = audioFiles,
                        onSelect = {
                            musicPlayerVM.setAudio(it)
                            showSongPicker = false
                        },
                        onDismiss = { showSongPicker = false },
                        onRefresh = {
                            audioList = getAllAudioFiles(context)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    // Detaylı Şarkı Bilgisi Popup
                    ExpandedSongInfoPopup(
                        selectedAudio = selectedAudio!!,
                        onDismiss = { showSongPicker = false },
                        onOpenSongPicker = { /* ... */ },
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        totalDuration = totalDuration,
                        onPlayPauseToggle = { musicPlayerVM.playPause() },
                        onSeekTo = { musicPlayerVM.seekTo(it) },
                        onNext = { musicPlayerVM.playNext() },
                        onPrevious = { musicPlayerVM.playPrevious() },
                        modifier = Modifier
                    )

                    // Sonraki Şarkılar Popup
                    NextSongsPopup(
                        upcoming = musicPlayerVM.upcomingSongs,
                        currentPlaying = currentPlaying,
                        onListChanged = { newList -> musicPlayerVM.updateUpcomingList(newList) },
                        musicPlayerVM = musicPlayerVM,
                        itemHeightFraction = 0.08f,
                        fontSizeFraction = 0.0350f
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
