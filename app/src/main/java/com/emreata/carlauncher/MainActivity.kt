package com.emreata.carlauncher

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.emreata.carlauncher.utils.PermissionsRow
import com.emreata.carlauncher.viewmodels.MusicPlayerViewModel
import java.util.Locale
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var musicPlayerViewModel: MusicPlayerViewModel

    private var hasLocationPermission by mutableStateOf(false)
    private var hasStoragePermission by mutableStateOf(false)
    private var hasNearbyPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedLang = prefs.getString("selected_language", "tr") ?: "tr"
        val locale = Locale(savedLang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        musicPlayerViewModel = MusicPlayerViewModel(application)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUI()
        requestAllPermissions()


        setContent {
            var drawerOpen by remember { mutableStateOf(false) }
            var settingsOpen by remember { mutableStateOf(false)}
            val isDefaultLauncher = isThisAppDefaultLauncher()
            var showLauncherDialog by remember { mutableStateOf(!isDefaultLauncher) }
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTopBar(drawerOpen = drawerOpen, onCloseDrawer = { drawerOpen = false })

                if (!hasLocationPermission || !hasStoragePermission || !hasNearbyPermission ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PermissionsRow(
                            hasLocationPermission = hasLocationPermission,
                            hasStoragePermission = hasStoragePermission,
                            hasNearbyPermission = hasNearbyPermission,
                            onRequestPermission = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", packageName, null)
                                startActivity(intent)
                            }
                        )
                    }
                } else {
                    MusicLauncherScreen(
                        drawerOpen = drawerOpen,
                        settingsOpen = settingsOpen,
                        onToggleDrawer = { drawerOpen = !drawerOpen },
                        onCloseDrawer = { drawerOpen = false },
                        onOpenSettings = { settingsOpen = !settingsOpen}
                    )
                }

                if (showLauncherDialog) {
                    SetDefaultLauncherDialog(
                        onDismiss = { showLauncherDialog = false }
                    )
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun requestAllPermissions() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, storagePermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun checkPermissions() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            storagePermission
        ) == PackageManager.PERMISSION_GRANTED

        hasNearbyPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    }





    private fun hideSystemUI() {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = decorView?.windowInsetsController
            controller?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }



    @Composable
    fun SetDefaultLauncherDialog(
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Varsayılan Launcher") },
            text = { Text("Bu uygulamayı varsayılan launcher yapmak ister misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    context.startActivity(intent)
                    onDismiss()
                }) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Hayır")
                }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        )
    }
    private fun isThisAppDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val defaultLauncher = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return defaultLauncher?.activityInfo?.packageName == packageName
    }
}

fun setLocale(activity: Activity, lang: String) {
    val locale = Locale(lang)
    Locale.setDefault(locale)

    val config = Configuration()
    config.setLocale(locale)

    activity.baseContext.resources.updateConfiguration(
        config,
        activity.baseContext.resources.displayMetrics
    )

    // Seçilen dili kaydet
    val prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit { putString("selected_language", lang) }

    // Activity'yi yeniden başlat
    val refresh = Intent(activity, activity::class.java)
    refresh.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    activity.startActivity(refresh)
    activity.finish()
}
