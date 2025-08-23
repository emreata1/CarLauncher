package com.astechsoft.carlauncher

import com.astechsoft.carlauncher.R  // carview.png resminizin bulunduğu package
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Button
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onCarViewChange: (Uri) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // Başlangıçta kaydedilmiş URI'yi oku
    var selectedCarView by remember {
        mutableStateOf(
            prefs.getString("car_view_uri", null)?.let { Uri.parse(it) }
        )
    }
    var autoPlay by remember {
        mutableStateOf(prefs.getBoolean("auto_play_on_launch", false))
    }

    // Dosya seçici (sadece resimler için)
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedCarView = it
            onCarViewChange(it)
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Kaydet
            prefs.edit().putString("car_view_uri", it.toString()).apply()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF212121))
            .fillMaxWidth(0.3f)
    ) {
        // Başlık + Geri butonu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {

            Text(
                text = "Ayarlar",
                color = Color.White,
                fontSize = 30.sp
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = selectedCarView?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(R.drawable.carview),
                    contentDescription = "Seçilen Araba",
                    modifier = Modifier
                        .height(100.dp)
                        .width(100.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "1:1 Oranında görsel kullanınız (Önerilen:1024x1024)",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(0.4f) // alanın %40'ını kaplasın
                )

                Button(
                    onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(0.1f) // alanın %20'sini kaplasın
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Değiştir"
                    )
                }
            }

            Divider(
                color = Color.LightGray, // Çizgi rengi
                thickness = 1.dp,   // Çizgi kalınlığı

            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Uygulama açılınca otomatik çal", color = Color.White,
                    fontSize = 18.sp,)
                Switch(

                    checked = autoPlay,
                    onCheckedChange = {
                        autoPlay = it
                        prefs.edit().putBoolean("auto_play_on_launch", it).apply()
                    }
                )
            }
        }
    }
}

