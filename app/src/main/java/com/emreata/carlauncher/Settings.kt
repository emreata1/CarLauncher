package com.emreata.carlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import com.emreata.carlauncher.utils.LanguageDropdown
import androidx.core.net.toUri
import androidx.core.content.edit

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    onCarViewChange: (Uri) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var selectedLang by remember { mutableStateOf("tr") }
    var selectedCarView by remember {
        mutableStateOf(
            prefs.getString("car_view_uri", null)?.toUri()
        )
    }
    var autoPlay by remember {
        mutableStateOf(prefs.getBoolean("auto_play_on_launch", false))
    }

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
            prefs.edit { putString("car_view_uri", it.toString()) }
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
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 30.sp
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.15f),
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
                    stringResource(R.string.car_image_hint),
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(0.4f)
                )

                Button(
                    onClick = { pickImageLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.change)
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,   // Çizgi kalınlığı
                color = Color.LightGray // Çizgi rengi
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.15f),
            ) {
                Text(
                    stringResource(R.string.auto_play),
                    color = Color.White,
                    fontSize = 18.sp
                )
                Switch(
                    checked = autoPlay,
                    onCheckedChange = {
                        autoPlay = it
                        prefs.edit { putBoolean("auto_play_on_launch", it) }
                    }
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = Color.LightGray
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.15f),
                ) {
                    Text(
                        text = stringResource(R.string.change_language), // strings.xml'de <string name="change_language">Dil değiştir</string>
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    LanguageDropdown(
                        context = context,
                        selectedLang = selectedLang
                    ) { langCode ->
                        selectedLang = langCode
                        setLocale(context as Activity, langCode)
                    }
                }

            }
        }
    }



