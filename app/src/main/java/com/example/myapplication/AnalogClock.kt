package com.example.myapplication

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.core.graphics.withSave

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AnalogClockWithNumbers(
    heightFraction: Float = 0.3f,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val sizeDp = (screenHeightDp * heightFraction).dp
    val radiusPx = with(LocalDensity.current) { sizeDp.toPx() / 2f }
    val animatedSecond = remember { Animatable(35f) }

    LaunchedEffect(Unit) {
        delay(1000)

        animatedSecond.animateTo(
            targetValue = 70f,
            animationSpec = tween(durationMillis = 1000)
        )
        animatedSecond.snapTo(animatedSecond.value % 60)

        // 2. 15'ten gerçek saniyeye geçiş
        val currentSecond = Calendar.getInstance().get(Calendar.SECOND).toFloat()
        val targetSecond = if (currentSecond < 15f) currentSecond + 60f else currentSecond
        animatedSecond.animateTo(
            targetValue = targetSecond,
            animationSpec = tween(durationMillis = 900)
        )
        animatedSecond.snapTo(animatedSecond.value % 60)

        while (true) {
            val now = Calendar.getInstance()
            val nextSecond = now.get(Calendar.SECOND).toFloat()
            val currentValue = animatedSecond.value

            val animTarget = if (nextSecond < currentValue % 60) nextSecond + 60f else nextSecond

            animatedSecond.animateTo(
                targetValue = animTarget,
                animationSpec = tween(durationMillis = 900)
            )
            animatedSecond.snapTo(animatedSecond.value % 60)
        }
    }

    val calendar = Calendar.getInstance()
    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = animatedSecond.value % 60

    Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        // Saat zemini ve çizimler aynı (önceki kod)
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF444444), Color(0xFF222222)),
                    center = center,
                    radius = radiusPx * 1.1f
                ),
                radius = radiusPx * 1.05f,
                center = center
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            val darkGray = Color(0xFFAAAAAA)

            repeat(60) { i ->
                val angleRad = Math.toRadians((i * 6 - 90).toDouble())
                val length = if (i % 5 == 0) radiusPx * 0.1f else radiusPx * 0.04f
                val stroke = if (i % 5 == 0) 4f else 2f
                val x1 = cx + cos(angleRad).toFloat() * (radiusPx - length)
                val y1 = cy + sin(angleRad).toFloat() * (radiusPx - length)
                val x2 = cx + cos(angleRad).toFloat() * radiusPx
                val y2 = cy + sin(angleRad).toFloat() * radiusPx
                drawLine(
                    color = darkGray,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = stroke
                )
            }

            drawLineWithShadow(cx, cy, (hours + minutes / 60f) * 30f - 90f, radiusPx * 0.5f, 8f, Color.Black)
            drawLineWithShadow(cx, cy, (minutes + seconds / 60f) * 6f - 90f, radiusPx * 0.7f, 6f, Color.Black)
            drawLineWithShadow(cx, cy, seconds * 6f - 90f, radiusPx * 0.9f, 2f, Color.Red)

            drawCircle(darkGray, radius = 8f, center = Offset(cx, cy))
        }

        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            val darkGrayText = Color(0xFFAAAAAA)
            for (i in 1..12) {
                val angle = Math.toRadians((i * 30 - 90).toDouble())
                val x = radiusPx * 0.8f * cos(angle)
                val y = radiusPx * 0.8f * sin(angle)

                Text(
                    text = i.toString(),
                    fontSize = 16.sp,
                    color = darkGrayText,
                    modifier = Modifier.offset {
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
                )

            }
        }
    }
}



private fun DrawScope.drawLineWithShadow(
    cx: Float, cy: Float,
    angleDeg: Float,
    length: Float,
    stroke: Float,
    color: Color
) {
    val angleRad = Math.toRadians(angleDeg.toDouble())
    val x2 = cx + cos(angleRad).toFloat() * length
    val y2 = cy + sin(angleRad).toFloat() * length

    drawContext.canvas.nativeCanvas.apply {
        withSave {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = color.toArgb()
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = stroke
                setShadowLayer(8f, 0f, 0f, android.graphics.Color.DKGRAY)
            }
            drawLine(cx, cy, x2, y2, paint)
        }
    }

    drawLine(
        color = color,
        start = Offset(cx, cy),
        end = Offset(x2, y2),
        strokeWidth = stroke
    )
}

