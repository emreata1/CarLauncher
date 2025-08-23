package com.astechsoft.carlauncher.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class TopBarViewModel(app: Application) : AndroidViewModel(app) {

    @SuppressLint("StaticFieldLeak")
    private val context = app.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _locationName = MutableStateFlow("Konum Yükleniyor...")
    val locationName: StateFlow<String> = _locationName


    fun fetchLocationAndWeather() {
        viewModelScope.launch {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (permission == PackageManager.PERMISSION_GRANTED) {
                try {
                    val location = fusedLocationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .await()

                    if (location != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION") val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.getOrNull(0)
                        val city = address?.adminArea ?: "Bilinmeyen Şehir"
                        val district = address?.subAdminArea ?: "Bilinmeyen İlçe"
                        _locationName.value = "$district / $city"


                    } else {
                        _locationName.value = "Konum Alınamadı"
                    }
                } catch (_: SecurityException) {
                    _locationName.value = "Konum izni yok"
                } catch (_: Exception) {
                    _locationName.value = "Konum Hatası"
                }
            } else {
                _locationName.value = "Konum izni verilmedi"
            }
        }
    }
}