package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

fun getAlbumArt(context: Context, uri: Uri): Bitmap? {
    val mmr = MediaMetadataRetriever()
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            mmr.setDataSource(pfd.fileDescriptor)
            val artBytes = mmr.embeddedPicture
            artBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    } catch (_: Exception) {
        null
    } finally {
        mmr.release()
    }
}

@Composable
fun AlbumArtImage(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier.size(36.dp)
) {
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Album Art",
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color.Gray, RoundedCornerShape(4.dp))
        )
    }
}

