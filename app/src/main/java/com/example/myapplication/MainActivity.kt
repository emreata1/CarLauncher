package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

import android.view.WindowInsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            var isLocationProviderAvailable by remember {
                mutableStateOf(isAnyLocationProviderEnabled())
            }

            // Rasyonel dialog gösterimi için state
            var showRationaleDialog by remember { mutableStateOf(false) }

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasLocationPermission = granted
                if (!granted) {
                    showRationaleDialog = true
                }
            }

            // Eğer izin yoksa ilk açılışta iste
            LaunchedEffect(Unit) {
                if (!hasLocationPermission) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            // Konum servisi kapalıysa kontrol et
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
                    text = { Text("Bu uygulama düzgün çalışması için konum iznine ihtiyaç duyar. Lütfen izin verin.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showRationaleDialog = false
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                            buttonText = "Konum İzni İste",
                            onClick = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
