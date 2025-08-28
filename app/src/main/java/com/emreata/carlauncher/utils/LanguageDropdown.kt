package com.emreata.carlauncher.utils

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.emreata.carlauncher.setLocale
import androidx.core.content.edit


@Composable
fun LanguageDropdown(
    context: Context,
    selectedLang: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val supportedLanguages = mapOf(
        "en" to "English",
        "tr" to "Türkçe",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "ko" to "한국어",
        "ru" to "Русский",
        "pt" to "Português",
        "zh" to "中文",
        "ja" to "日本語",
        "ar" to "العربية",
        "pl" to "Polski",
        "nl" to "Nederlands",
        "it" to "Italiano"
    )

    Box {
        Text(
            text = supportedLanguages[selectedLang] ?: selectedLang,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            supportedLanguages.forEach { (langCode, langName) ->
                DropdownMenuItem(
                    text = { Text(langName) },
                    onClick = {
                        onLanguageSelected(langCode)

                        // SharedPreferences'a kaydet
                        prefs.edit { putString("selected_language", langCode) }

                        // Uygulama dilini değiştir
                        setLocale(context as Activity, langCode)

                        expanded = false
                    }
                )
            }
        }
    }
}


