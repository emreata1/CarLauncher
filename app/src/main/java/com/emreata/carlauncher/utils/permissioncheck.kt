package com.emreata.carlauncher.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PermissionsRow(
    hasLocationPermission: Boolean,
    hasStoragePermission: Boolean,
    hasNearbyPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val horizontalPadding = 40.dp
    val spacing = 40.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f) // dikeyde %60
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center // dikeyde ortala
    ) {
        val totalSpacing = spacing * 2
        val itemWidth: Dp = (maxWidth - totalSpacing) / 3f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PermissionCard(
                modifier = Modifier.width(itemWidth),
                title = "Konum",
                granted = hasLocationPermission,
                onClick = onRequestPermission
            )
            PermissionCard(
                modifier = Modifier.width(itemWidth),
                title = "Depolama",
                granted = hasStoragePermission,
                onClick = onRequestPermission
            )
            PermissionCard(
                modifier = Modifier.width(itemWidth),
                title = "Yakındaki\nCihazlar",
                granted = hasNearbyPermission,
                onClick = onRequestPermission
            )
        }
    }
}


@Composable
private fun PermissionCard(
    modifier: Modifier = Modifier,
    title: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(400.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF212121),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(top = 30.dp, bottom = 20.dp )
        ) {
            Icon(
                imageVector = when(title) {
                    "Konum" -> Icons.Default.Place // Konum için örnek ikon
                    "Depolama" -> Icons.Default.Folder // Depolama için örnek ikon
                    "Yakındaki\nCihazlar" -> Icons.Default.Devices // Yakındaki cihazlar
                    else -> Icons.Default.Info
                },
                contentDescription = "$title ikonu",
                tint = Color.White,
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopCenter)
            )

            // Başlık ortada
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            // Altta durum ikonu
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (granted) "İzin verildi" else "İzin yok",
                tint = if (granted) Color.Green else Color.Red, // burası önemli
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.BottomCenter)
            )

        }


    }
}

