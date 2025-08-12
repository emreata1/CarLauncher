package com.astechsoft.carlauncher

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.core.graphics.withSave
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AnalogClockWithNumbers(
    widthFraction: Float = 0.3f,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val sizeDp = (screenWidthDp * widthFraction).dp
    val radiusPx = with(LocalDensity.current) { sizeDp.toPx() / 2f }
    val animatedSecond = remember { Animatable(35f) }

    LaunchedEffect(Unit) {
        delay(1000)

        animatedSecond.animateTo(
            targetValue = 70f,
            animationSpec = tween(durationMillis = 1000)
        )
        animatedSecond.snapTo(animatedSecond.value % 60)

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


            fun DrawScope.drawLineWithMetallicEffect(
                start: Offset,
                end: Offset,
                stroke: Float
            ) {
                val metallicBrush = Brush.linearGradient(
                    colors = listOf(Color.LightGray, Color.White, Color.Gray, Color.DarkGray),
                    start = start,
                    end = end,
                    tileMode = TileMode.Mirror
                )
                drawLine(
                    brush = metallicBrush,
                    start = start,
                    end = end,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            repeat(60) { i ->
                val isHourMark = i % 5 == 0
                val isMainMark = i == 0 || i == 15 || i == 30 || i == 45

                val length = when {
                    isMainMark -> radiusPx * 0.18f
                    isHourMark -> radiusPx * 0.13f
                    else -> radiusPx * 0.05f
                }
                val stroke = when {
                    isMainMark -> 8f
                    isHourMark -> 6f
                    else -> 2f
                }

                val angleRad = Math.toRadians((i * 6 - 90).toDouble())
                val start = Offset(
                    cx + cos(angleRad).toFloat() * (radiusPx - length),
                    cy + sin(angleRad).toFloat() * (radiusPx - length)
                )
                val end = Offset(
                    cx + cos(angleRad).toFloat() * radiusPx,
                    cy + sin(angleRad).toFloat() * radiusPx
                )

                if (isHourMark) {
                    drawLineWithMetallicEffect(start, end, stroke)
                } else {
                    drawLine(
                        color = Color.Red,
                        start = start,
                        end = end,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            fun drawDoubleTaperedMetallicHand(angleDeg: Float, lengthRatio: Float, baseWidth: Float) {
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val cx = size.width / 2f
                val cy = size.height / 2f

                val handLength = radiusPx * lengthRatio
                val backLength = handLength * 0.2f // Geriye doğru kısım


                val frontTip = Offset(
                    cx + cos(angleRad).toFloat() * handLength,
                    cy + sin(angleRad).toFloat() * handLength
                )
                val backTip = Offset(
                    cx - cos(angleRad).toFloat() * backLength,
                    cy - sin(angleRad).toFloat() * backLength
                )

                val perpAngle = angleRad + Math.PI / 2

                val halfWidth = baseWidth / 2f

                val points = mutableListOf<Offset>()

                points.add(
                    Offset(
                        backTip.x + cos(perpAngle).toFloat() * 0f,
                        backTip.y + sin(perpAngle).toFloat() * 0f
                    )
                )
                points.add(
                    Offset(
                        cx + cos(perpAngle).toFloat() * halfWidth,
                        cy + sin(perpAngle).toFloat() * halfWidth
                    )
                )
                points.add(
                    Offset(
                        frontTip.x + cos(perpAngle).toFloat() * 0f,
                        frontTip.y + sin(perpAngle).toFloat() * 0f
                    )
                )
                points.add(
                    Offset(
                        frontTip.x - cos(perpAngle).toFloat() * 0f,
                        frontTip.y - sin(perpAngle).toFloat() * 0f
                    )
                )
                points.add(
                    Offset(
                        cx - cos(perpAngle).toFloat() * halfWidth,
                        cy - sin(perpAngle).toFloat() * halfWidth
                    )
                )
                points.add(
                    Offset(
                        backTip.x - cos(perpAngle).toFloat() * 0f,
                        backTip.y - sin(perpAngle).toFloat() * 0f
                    )
                )

                val handPath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    close()
                }
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFC0C0C0), // Açık gümüş grisi
                        Color(0xFFE0E0E0), // Parlak beyaza yakın
                        Color(0xFFF8F8F8), // Çok açık, neredeyse beyaz
                        Color(0xFFE0E0E0),
                        Color(0xFFC0C0C0)
                    ),
                    start = Offset(cx, cy - handLength * 0.5f),
                    end = Offset(cx, cy + handLength * 0.5f),
                    tileMode = TileMode.Clamp
                )


                drawContext.canvas.nativeCanvas.apply {
                    withSave {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.FILL
                            shader = null
                            alpha = 255  // Opaklık tam
                            setShadowLayer(18f, 0f, 0f, android.graphics.Color.DKGRAY)  // Gölge biraz daha belirgin
                        }
                        drawPath(handPath.asAndroidPath(), paint)
                    }
                }

                drawPath(handPath, brush = gradientBrush, alpha = 1.0f) // Opaklık tam

                drawContext.canvas.nativeCanvas.apply {
                    withSave {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.FILL
                            shader = null
                            alpha = 255  // Opaklık tam
                            setShadowLayer(18f, 0f, 0f, android.graphics.Color.DKGRAY)  // Gölge biraz daha belirgin
                        }
                        drawPath(handPath.asAndroidPath(), paint)
                    }
                }

                drawPath(handPath, brush = gradientBrush, alpha = 0.95f)
            }
            drawDoubleTaperedMetallicHand((hours + minutes / 60f) * 30f - 90f, 0.5f, 24f)
            drawDoubleTaperedMetallicHand((minutes + seconds / 60f) * 6f - 90f, 0.7f, 18f)
            val secondColor = Color(0xFFFF4444)
            val secondLength = radiusPx * 0.9f
            val secondBackLength = secondLength * 0.15f // Arkaya doğru kısa uzantı
            val angleRad = Math.toRadians(seconds * 6f - 90f.toDouble())
            val start = Offset(
                cx - cos(angleRad).toFloat() * secondBackLength,
                cy - sin(angleRad).toFloat() * secondBackLength
            )
            val end = Offset(
                cx + cos(angleRad).toFloat() * secondLength,
                cy + sin(angleRad).toFloat() * secondLength
            )
            drawContext.canvas.nativeCanvas.apply {
                withSave {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = secondColor.toArgb()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4f
                        setShadowLayer(12f, 0f, 0f, android.graphics.Color.RED)
                    }
                    drawLine(start.x, start.y, end.x, end.y, paint)
                }
            }

            drawLine(
                color = secondColor,
                start = start,
                end = end,
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            drawCircle(secondColor, radius = 8f, center = Offset(cx, cy))

            val rectWidth = 16f
            val rectHeight = 8f
            val perpAngle = angleRad + Math.PI / 2

            val rectPoints = listOf(
                Offset(
                    start.x + cos(perpAngle).toFloat() * rectHeight / 2,
                    start.y + sin(perpAngle).toFloat() * rectHeight / 2
                ),
                Offset(
                    start.x + cos(perpAngle).toFloat() * rectHeight / 2 + cos(angleRad).toFloat() * rectWidth,
                    start.y + sin(perpAngle).toFloat() * rectHeight / 2 + sin(angleRad).toFloat() * rectWidth
                ),
                Offset(
                    start.x - cos(perpAngle).toFloat() * rectHeight / 2 + cos(angleRad).toFloat() * rectWidth,
                    start.y - sin(perpAngle).toFloat() * rectHeight / 2 + sin(angleRad).toFloat() * rectWidth
                ),
                Offset(
                    start.x - cos(perpAngle).toFloat() * rectHeight / 2,
                    start.y - sin(perpAngle).toFloat() * rectHeight / 2
                ),
            )

            val rectPath = Path().apply {
                moveTo(rectPoints[0].x, rectPoints[0].y)
                for (i in 1 until rectPoints.size) {
                    lineTo(rectPoints[i].x, rectPoints[i].y)
                }
                close()
            }

            drawPath(
                path = rectPath,
                color = secondColor,
                style = Stroke(width = 3f)
            )

        }
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {}
    }
}
