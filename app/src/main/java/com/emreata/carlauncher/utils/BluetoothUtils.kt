package com.emreata.carlauncher.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import androidx.annotation.RequiresPermission

@Suppress("DEPRECATION")
fun isBluetoothEnabled(): Boolean {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    return adapter != null && adapter.isEnabled
}
@Suppress("DEPRECATION")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun checkBluetoothConnected(): Boolean {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
    if (!adapter.isEnabled) return false

    val headsetState = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
    val a2dpState = adapter.getProfileConnectionState(BluetoothProfile.A2DP)
    val healthState = adapter.getProfileConnectionState(BluetoothProfile.HEALTH)

    return headsetState == BluetoothProfile.STATE_CONNECTED ||
            a2dpState == BluetoothProfile.STATE_CONNECTED ||
            healthState == BluetoothProfile.STATE_CONNECTED
}
@Suppress("MissingPermission", "DEPRECATION")
fun getConnectedBluetoothDeviceName(): String? {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
    if (!adapter.isEnabled) return null

    val pairedDevices = adapter.bondedDevices
    val deviceName = pairedDevices.firstOrNull()?.name
    return deviceName?.take(7)
}
