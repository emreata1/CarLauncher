package com.astechsoft.carlauncher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.createBitmap

fun getAllLaunchableApps(context: Context): List<ResolveInfo> {
    val pm = context.packageManager
    return pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        PackageManager.MATCH_ALL
    ).sortedBy { it.loadLabel(pm).toString() }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        drawable.bitmap?.let { return it }
    }

    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        drawable is AdaptiveIconDrawable
    ) {
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
    } else {
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
    }
    return bitmap
}
