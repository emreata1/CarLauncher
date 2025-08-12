package com.astechsoft.carlauncher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import android.view.WindowInsets
import androidx.activity.compose.rememberLauncherForActivityResult
import com.astechsoft.carlauncher.viewmodels.MusicPlayerViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var musicPlayerViewModel: MusicPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        musicPlayerViewModel = MusicPlayerViewModel(application)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
            controller?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }

        setContent {
            var drawerOpen by remember { mutableStateOf(false) }

            var hasLocationPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            var hasStoragePermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            var isLocationProviderAvailable by remember {
                mutableStateOf(isAnyLocationProviderEnabled())
            }

            var showRationaleDialog by remember { mutableStateOf(false) }

            val multiplePermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                hasStoragePermission = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
                if (!hasLocationPermission || !hasStoragePermission) {
                    showRationaleDialog = true
                }
            }

            // İlk açılışta izinleri iste
            LaunchedEffect(Unit) {
                if (!hasLocationPermission || !hasStoragePermission) {
                    multiplePermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    )
                }
            }

            // Konum servisi açık mı kontrol et
            LaunchedEffect(Unit) {
                while (true) {
                    val providerAvailable = isAnyLocationProviderEnabled()
                    if (providerAvailable != isLocationProviderAvailable) {
                        isLocationProviderAvailable = providerAvailable
                    }
                    delay(2000)
                }
            }

            // Rasyonel dialog
            if (showRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showRationaleDialog = false },
                    title = { Text("İzin Gerekli") },
                    text = { Text("Uygulamanın düzgün çalışması için konum ve depolama izinlerine ihtiyaç vardır.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showRationaleDialog = false
                            multiplePermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                )
                            )
                        }) {
                            Text("İzin Ver")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRationaleDialog = false }) {
                            Text("İptal")
                        }
                    }
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                CustomTopBar(drawerOpen = drawerOpen, onCloseDrawer = { drawerOpen = false })

                when {
                    !hasLocationPermission -> {
                        PermissionScreen(
                            text = "Konum izni verilmedi.",
                            buttonText = "İzinleri İste",
                            onClick = {
                                multiplePermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        )
                    }

                    !hasStoragePermission -> {
                        PermissionScreen(
                            text = "Depolama izni verilmedi.",
                            buttonText = "İzinleri İste",
                            onClick = {
                                multiplePermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        )
                    }

                    !isLocationProviderAvailable -> {
                        PermissionScreen(
                            text = "Konum servisleri kapalı. Lütfen GPS veya ağ tabanlı konumu etkinleştirin.",
                            buttonText = "Konum Ayarlarını Aç",
                            onClick = {
                                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        )
                    }

                    else -> {
                        MusicLauncherScreen(
                            drawerOpen = drawerOpen,
                            onToggleDrawer = { drawerOpen = !drawerOpen },
                            onCloseDrawer = { drawerOpen = false }
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                musicPlayerViewModel.playNext()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                musicPlayerViewModel.playPrevious()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                musicPlayerViewModel.playPause()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isAnyLocationProviderEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

@Composable
fun PermissionScreen(text: String, buttonText: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClick) {
            Text(text = buttonText)
        }
    }
}
