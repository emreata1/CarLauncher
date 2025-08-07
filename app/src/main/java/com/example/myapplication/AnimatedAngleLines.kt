package com.example.myapplication

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedCurvedLines() {
    val segmentCount = 20  // Boğum sayısı
    val animationProgress = rememberInfiniteTransition()
    val animatedFloat = animationProgress.animateFloat(
        initialValue = 0f,
        targetValue = segmentCount.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val lineLength = 450f
    val offsetY = 265f
    val baseColor = Color(0xFFFF3B3B)
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val carviewSizePx = with(density) { 200.dp.toPx() }
            val extraSpace = 50f

            val leftStart = Offset(center.x - carviewSizePx / 2 + extraSpace, center.y - offsetY)
            val rightStart = Offset(center.x + carviewSizePx / 2 - extraSpace, center.y - offsetY)

            // İki nokta arası yayı parametreleştirip segment segment çizelim
            fun getQuadraticBezierPoint(t: Float, start: Offset, control: Offset, end: Offset): Offset {
                // Quadratic Bezier formülü: (1-t)^2 * P0 + 2*(1-t)*t*P1 + t^2 * P2
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

                    // Animasyonu ters çeviriyoruz
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
                        strokeWidth = 20f,
                        cap = StrokeCap.Round
                    )
                }
            }


            val leftEnd = Offset(leftStart.x -30, leftStart.y + lineLength)
            val leftControl = Offset(leftStart.x + lineLength * 0.1f, leftStart.y + lineLength * 0.5f)

            val rightEnd = Offset(rightStart.x+30, rightStart.y + lineLength)
            val rightControl = Offset(rightStart.x - lineLength * 0.1f, rightStart.y + lineLength * 0.5f)

            drawSegmentedCurve(leftStart, leftControl, leftEnd, segmentCount, animatedFloat.value)
            drawSegmentedCurve(rightStart, rightControl, rightEnd, segmentCount, animatedFloat.value)
        }
    }
}
