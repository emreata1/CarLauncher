package com.emreata.carlauncher.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedCurvedLines(
    painter: Painter,
    widthDp: Dp,
    segmentCount: Int = 20,
    lineLengthFraction: Float = 0.5f,
    strokeWidthDp: Float = 20f,
    baseColor: Color = Color(0xFFFF3B3B),
    animationDurationMs: Int = 2000,
    analogClockWidthFraction: Float = 0.2f,
    speedometerWidthFraction: Float = 0.21f,
    remainingwidth: Float,
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

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(remainingwidth)

    ) {
        Canvas(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val centerY = totalHeight / 2

            val distancePx = totalWidth * 0.01f

            val leftX = totalWidth * analogClockWidthFraction + distancePx
            val rightX = totalWidth - totalWidth * speedometerWidthFraction - distancePx

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
                    drawLine(
                        color = baseColor.copy(alpha = alpha),
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

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.5f)
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }
}


