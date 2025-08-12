package com.astechsoft.carlauncher.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

@Composable
fun AnimatedCurvedLines(
    segmentCount: Int = 20,
    lineLengthFraction: Float = 0.5f,
    strokeWidthDp: Float = 20f,
    baseColor: Color = Color(0xFFFF3B3B),
    animationDurationMs: Int = 2000,
    analogClockWidthFraction: Float = 0.2f,
    speedometerWidthFraction: Float = 0.21f,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidthDp.dp.toPx() }

    val infiniteTransition = rememberInfiniteTransition()
    val animatedFloat = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = segmentCount.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val totalWidth = size.width
            val totalHeight = size.height

            val aspectRatio = totalWidth / totalHeight

            val distanceFractionFromWidgets = when {
                aspectRatio < 0.3f -> 0.0001f   // Neredeyse üst üste, çok çok yakın
                aspectRatio < 0.5f -> lerp(0.0001f, 0.0003f, (aspectRatio - 0.3f) / 0.2f)
                aspectRatio < 0.8f -> lerp(0.0003f, 0.001f, (aspectRatio - 0.5f) / 0.3f)
                aspectRatio < 1.2f -> lerp(0.001f, 0.003f, (aspectRatio - 0.8f) / 0.4f)
                aspectRatio < 1.5f -> lerp(0.003f, 0.005f, (aspectRatio - 1.2f) / 0.3f)
                aspectRatio < 2.0f -> lerp(0.005f, 0.008f, (aspectRatio - 1.5f) / 0.5f)
                aspectRatio < 3.0f -> lerp(0.008f, 0.01f, (aspectRatio - 2.0f) / 1.0f)
                else -> 0.01f
            }

            val distancePx = totalWidth * distanceFractionFromWidgets

            val leftX = centerX - (analogClockWidthFraction * totalWidth) / 2 - distancePx
            val rightX = centerX + (speedometerWidthFraction * totalWidth) / 2 + distancePx

            val lineLengthPx = totalHeight * lineLengthFraction

            val leftStart = Offset(leftX, centerY - lineLengthPx * 0.6f)
            val rightStart = Offset(rightX, centerY - lineLengthPx * 0.6f)

            fun getQuadraticBezierPoint(t: Float, start: Offset, control: Offset, end: Offset): Offset {
                val oneMinusT = 1 - t
                val x = oneMinusT * oneMinusT * start.x +
                        2 * oneMinusT * t * control.x +
                        t * t * end.x
                val y = oneMinusT * oneMinusT * start.y +
                        2 * oneMinusT * t * control.y +
                        t * t * end.y
                return Offset(x, y)
            }

            fun drawSegmentedCurve(
                start: Offset,
                control: Offset,
                end: Offset,
                segments: Int,
                progress: Float
            ) {
                val segmentLength = 1f / segments
                for (i in 0 until segments) {
                    val tStart = i * segmentLength
                    val tEnd = (i + 1) * segmentLength
                    val pStart = getQuadraticBezierPoint(tStart, start, control, end)
                    val pEnd = getQuadraticBezierPoint(tEnd, start, control, end)

                    val distanceFromHighlight = (progress - i + segments) % segments

                    val alpha = when {
                        distanceFromHighlight < 1f -> 1f
                        distanceFromHighlight < 2f -> 0.7f
                        distanceFromHighlight < 3f -> 0.4f
                        else -> 0.15f
                    }
                    val color = baseColor.copy(alpha = alpha)

                    drawLine(
                        color = color,
                        start = pStart,
                        end = pEnd,
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            val leftEnd = Offset(leftStart.x - 30, leftStart.y + lineLengthPx)
            val leftControl = Offset(leftStart.x + lineLengthPx * 0.1f, leftStart.y + lineLengthPx * 0.5f)

            val rightEnd = Offset(rightStart.x + 30, rightStart.y + lineLengthPx)
            val rightControl = Offset(rightStart.x - lineLengthPx * 0.1f, rightStart.y + lineLengthPx * 0.5f)

            drawSegmentedCurve(leftStart, leftControl, leftEnd, segmentCount, animatedFloat.value)
            drawSegmentedCurve(rightStart, rightControl, rightEnd, segmentCount, animatedFloat.value)
        }
    }
}

