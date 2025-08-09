package com.example.myapplication

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

@SuppressLint("MissingPermission")
@Composable
fun SpeedometerScreen(
    widthFraction: Float = 0.3f,
    modifier: Modifier = Modifier
) {
    val vm: SpeedViewModel = viewModel()
    val speed by vm.speedKmh.collectAsState(initial = 0f)

    StyledSpeedometer(
        currentSpeed = speed.toInt(),
        widthFraction = widthFraction,
        modifier = modifier
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun StyledSpeedometer(
    modifier: Modifier = Modifier,
    maxSpeed: Int = 240,
    currentSpeed: Int = 0,
    widthFraction: Float = 0.3f
) {
    // Ekran genişliğine göre boyut hesaplama
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val sizeDp = (screenWidthDp * widthFraction).dp
    val radiusPx = with(LocalDensity.current) { sizeDp.toPx() / 2f }

    val pointerAnimAngle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(1000)
        pointerAnimAngle.animateTo(200f, tween(1000))
        pointerAnimAngle.animateTo(0f, tween(1000))
    }

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {


            val cx = size.width / 2f
            val cy = size.height / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF444444), Color(0xFF222222)),
                    center = center,
                    radius = radiusPx * 1.1f
                ),
                radius = radiusPx,
                center = center
            )

            val tickCount = 120
            val startAngle = 135f
            val sweepAngle = 270f

            repeat(tickCount + 1) { i ->
                val speedValue = (maxSpeed / tickCount) * i
                val angleRad = Math.toRadians((startAngle + i * (sweepAngle / tickCount)).toDouble())

                val isBigTick = speedValue % 20 == 0
                val isMediumTick = speedValue % 10 == 0 && !isBigTick

                val length = when {
                    isBigTick -> radiusPx * 0.12f
                    isMediumTick -> radiusPx * 0.09f
                    else -> radiusPx * 0.05f
                }
                val stroke = when {
                    isBigTick -> 4f
                    isMediumTick -> 3f
                    else -> 2f
                }
                val color = when {
                    isBigTick -> Color(0xFFFF0064)
                    isMediumTick -> Color(0xFFFF6666)
                    else -> Color.Red
                }

                val x1 = cx + cos(angleRad).toFloat() * (radiusPx - length)
                val y1 = cy + sin(angleRad).toFloat() * (radiusPx - length)
                val x2 = cx + cos(angleRad).toFloat() * radiusPx
                val y2 = cy + sin(angleRad).toFloat() * radiusPx

                drawLine(color, Offset(x1, y1), Offset(x2, y2), stroke)

                if (isBigTick) {
                    val paint = android.graphics.Paint().apply {
                        this.color = Color(0xFFAAAAAA).toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = radiusPx / 8
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                    }

                    val labelRadius = radiusPx * 0.78f
                    val labelX = cx + labelRadius * cos(angleRad).toFloat()
                    val labelY = cy + labelRadius * sin(angleRad).toFloat()

                    drawContext.canvas.nativeCanvas.drawText(
                        speedValue.toString(),
                        labelX,
                        labelY + paint.textSize / 3,
                        paint
                    )
                }
            }

            val safeSpeed = currentSpeed.coerceIn(0, maxSpeed)
            val basePointerAngle = startAngle + (safeSpeed.toFloat() / maxSpeed) * sweepAngle
            val pointerAngleDeg = basePointerAngle + pointerAnimAngle.value
            val pointerAngleRad = Math.toRadians(pointerAngleDeg.toDouble())
            val pointerLength = radiusPx * 0.75f
            val pointerWidth = radiusPx * 0.03f

            val perp = pointerAngleRad + PI / 2
            val x1 = cx + cos(perp).toFloat() * pointerWidth
            val y1 = cy + sin(perp).toFloat() * pointerWidth
            val x2 = cx - cos(perp).toFloat() * pointerWidth
            val y2 = cy - sin(perp).toFloat() * pointerWidth
            val x3 = cx + cos(pointerAngleRad).toFloat() * pointerLength
            val y3 = cy + sin(pointerAngleRad).toFloat() * pointerLength

            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
                lineTo(x3, y3)
                close()
            }
            drawPath(path, Color.Red)

            drawCircle(
                color = Color.Black,
                center = Offset(cx, cy),
                radius = radiusPx * 0.08f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radiusPx * 0.055f)
            )
            drawCircle(
                color = Color(0xFF222222),
                center = Offset(cx, cy),
                radius = radiusPx * 0.06f
            )
        }
    }
}
