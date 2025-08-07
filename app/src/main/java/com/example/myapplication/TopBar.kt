@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.utils.checkBluetoothConnected
import com.example.myapplication.utils.checkWifiConnected
import com.example.myapplication.utils.getConnectedBluetoothDeviceName
import com.example.myapplication.utils.getConnectedWifiName
import com.example.myapplication.utils.getCurrentTime
import com.example.myapplication.utils.isBluetoothEnabled
import kotlin.Unit

const val API_KEY = "ecd4bcb60b878b71d15d823d98fd955b" // OpenWeatherMap API anahtarın
@SuppressLint("MissingPermission")
@Composable
fun CustomTopBar(drawerOpen: Boolean, onCloseDrawer: () -> Unit, vm: TopBarViewModel = viewModel()) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val locationName by vm.locationName.collectAsState()
    val weatherInfo by vm.weatherInfo.collectAsState()
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var isWifiConnected by remember { mutableStateOf(checkWifiConnected(context)) }
    var isBluetoothOn by remember { mutableStateOf(isBluetoothEnabled()) }
    var isBluetoothConnected by remember { mutableStateOf(false) }

    LaunchedEffect(permissionState.status) {
        if (permissionState.status == PermissionStatus.Granted) {
            vm.fetchLocationAndWeather()
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
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
        title = {}, // boş bırakıyoruz

        actions = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol ikonlar - 1/3 genişlik
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {if (drawerOpen) {
                            onCloseDrawer()  // drawer açıksa kapat
                        } else {
                            // İstersen başka işlem yapabilirsin (örneğin activity.finish() gibi)
                        } }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(homeIntent)
                        }) {
                            Icon(Icons.Filled.Home, contentDescription = "Ana Ekran", tint = Color.White)
                        }
                    }
                }

                // Ortadaki metinler - 1/3 genişlik, ortalanmış
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
                        Text(
                            text = weatherInfo,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Sağ ikonlar + saat - 1/3 genişlik, sağa yaslanmış
                // Sağ ikonlar + saat - 1/3 genişlik, sağa yaslanmış
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                },
                                modifier = Modifier.size(32.dp)  // Varsayılan 48dp yerine 32dp
                            ) {
                                Icon(
                                    imageVector = if (isWifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                    contentDescription = "Wi-Fi Durumu",
                                    tint = Color.White,
                                )
                            }
                            Text(
                                text = getConnectedWifiName(context) ?: "Wi-Fi Yok",
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
                            },modifier = Modifier.size(32.dp)
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
                                text = getConnectedBluetoothDeviceName() ?: "Bluetooth Yok",
                                color = Color.White,
                                fontSize = 9.sp,
                                maxLines = 1 ,
                                overflow = TextOverflow.Ellipsis
                            )
                        }


                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }


            }
        }
    )


}













