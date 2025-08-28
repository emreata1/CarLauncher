@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
package com.emreata.carlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import android.provider.Settings
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emreata.carlauncher.utils.checkBluetoothConnected
import com.emreata.carlauncher.utils.checkWifiConnected
import com.emreata.carlauncher.utils.getConnectedBluetoothDeviceName
import com.emreata.carlauncher.utils.getConnectedWifiName
import com.emreata.carlauncher.utils.getCurrentTime
import com.emreata.carlauncher.utils.isBluetoothEnabled
import kotlin.Unit
import android.media.AudioManager
import androidx.compose.material3.Slider
import android.database.ContentObserver
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.emreata.carlauncher.utils.isLocationEnabled
import com.emreata.carlauncher.viewmodels.TopBarViewModel

@SuppressLint("MissingPermission")
@Composable
fun CustomTopBar(
    drawerOpen: Boolean,
    onCloseDrawer: () -> Unit,
    vm: TopBarViewModel = viewModel())
{
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val locationName by vm.locationName.collectAsState()
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var isWifiConnected by remember { mutableStateOf(checkWifiConnected(context)) }
    var isBluetoothOn by remember { mutableStateOf(isBluetoothEnabled()) }
    var isBluetoothConnected by remember { mutableStateOf(false) }
    var isLocationEnabled by remember { mutableStateOf(isLocationEnabled(context))}
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    isLocationEnabled = isLocationEnabled(context)
                }
            }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

// Permission kontrol ve location+weather fetch
    LaunchedEffect(permissionState.status) {
        if (permissionState.status == PermissionStatus.Granted) {
            vm.fetchLocationAndWeather()
        } else {
            permissionState.launchPermissionRequest()
        }
    }

// Diğer sürekli güncellemeler
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            isWifiConnected = checkWifiConnected(context)
            isBluetoothOn = isBluetoothEnabled()
            isBluetoothConnected = try {
                checkBluetoothConnected()
            } catch (_: SecurityException) {
                false
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF212121)),
        title = {},
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {




                            IconButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                },
                                modifier = Modifier.width(56.dp).height(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                    contentDescription = "Wi-Fi Durumu",
                                    tint = Color.White,
                                )
                            }
                            Text(
                                text = getConnectedWifiName(context) ?: stringResource(R.string.wifi_status_none),
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }, modifier = Modifier.width(56.dp).height(38.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        !isBluetoothOn -> Icons.Filled.BluetoothDisabled
                                        isBluetoothConnected -> Icons.Filled.BluetoothAudio
                                        else -> Icons.Filled.Bluetooth
                                    },
                                    contentDescription = "Bluetooth Durumu",
                                    tint = Color.White,
                                )
                            }
                            Text(
                                text = getConnectedBluetoothDeviceName() ?: stringResource(R.string.bluetooth_status_none),
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                },
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLocationEnabled
                                        ) {
                                        Icons.Filled.LocationOn
                                    } else {
                                        Icons.Filled.LocationOff
                                    },
                                    contentDescription = "Konum Durumu",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = if (isLocationEnabled)
                                    stringResource(R.string.location_on)
                                else
                                    stringResource(R.string.location_off),
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }




                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = locationName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    VolumeSlider(
                        modifier = Modifier.height(80.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun VolumeSlider(
    modifier: Modifier = Modifier,
    trackHeight: Dp = 20.dp,
    thumbSize: Dp = 40.dp
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    val contentResolver = context.contentResolver
    val volumeObserver = remember {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            }
        }
    }

    DisposableEffect(Unit) {
        contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
        onDispose {
            contentResolver.unregisterContentObserver(volumeObserver)
        }
    }

    Slider(
        value = currentVolume,
        onValueChange = { value ->
            currentVolume = value
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value.toInt(), 0)
        },
        valueRange = 0f..maxVolume,
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(max(thumbSize, trackHeight))
            .padding(horizontal = (thumbSize / 2)),
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .border(width = 2.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentVolume <= 0f)
                        Icons.AutoMirrored.Filled.VolumeOff
                    else
                        Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.volume),
                    tint = Color.White,
                    modifier = Modifier.size((thumbSize.value * 0.6f).dp)
                )

            }
        }
        ,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(trackHeight),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Black,
                    inactiveTrackColor = Color.Gray
                )
            )
        }
    )



}



