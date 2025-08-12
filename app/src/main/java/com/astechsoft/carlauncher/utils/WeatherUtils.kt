package com.astechsoft.carlauncher.utils

import com.astechsoft.carlauncher.API_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

suspend fun fetchWeather(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&lang=tr&appid=${API_KEY}"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val jsonData = response.body?.string() ?: return@withContext "Hava durumu alınamadı"
        val jsonObject = JSONObject(jsonData)
        val main = jsonObject.getJSONObject("main")
        val weatherArray = jsonObject.getJSONArray("weather")
        val weatherDescription = weatherArray.getJSONObject(0).getString("description")
        val temp = main.getDouble("temp")
        val capitalizedDescription = weatherDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        "${temp.toInt()}°C, $capitalizedDescription"
    } catch (e: Exception) {
        e.printStackTrace()
        "Hava durumu alınamadı"
    }
}