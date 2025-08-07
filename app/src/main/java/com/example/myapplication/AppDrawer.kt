package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ResolveInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.utils.drawableToBitmap
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import kotlin.math.sqrt

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalPagerApi::class)
@Composable
fun CustomDrawer(
    initialApps: List<ResolveInfo>,
    onAppClick: (ResolveInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager

    // Ekran genişliği dp cinsinden
    val screenWidthDp = LocalContext.current.resources.displayMetrics.widthPixels /
            LocalContext.current.resources.displayMetrics.density

    val columnsPerRow = (screenWidthDp / 80f).toInt().coerceIn(3, 8)
    val iconSize = remember { (screenWidthDp / columnsPerRow).dp * 0.5f }  // Kare ikon boyutu
    val boxHeight = iconSize + 28.dp  // İkon + yazı için yeterli yükseklik

    // Kayıtlı uygulama sırasını yükle ve uygula
    val savedOrder = loadAppOrder(context)
    var apps by remember {
        mutableStateOf(
            if (savedOrder.isNotEmpty()) {
                val ordered = initialApps.filter { savedOrder.contains(it.activityInfo.packageName) }
                    .sortedBy { savedOrder.indexOf(it.activityInfo.packageName) }
                val newApps = initialApps.filter { !savedOrder.contains(it.activityInfo.packageName) }
                (ordered + newApps).toMutableList()
            } else {
                initialApps.toMutableList()
            }
        )
    }

    // Sayfalara böl
    val itemsPerPage = 2 * columnsPerRow
    val pages = paginateList(apps, itemsPerPage)
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    // Her item'ın ekran konumu tutuluyor (sürükleme için)
    val itemPositions = rememberItemPositions(apps.size)

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.83f)
                .background(Color(0xDD000000))
                .padding(8.dp)
        ) {
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // 2 satır, her satır columnsPerRow kadar ikon gösterilecek
                    for (row in 0 until 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (col in 0 until columnsPerRow) {
                                val globalIndex = page * itemsPerPage + row * columnsPerRow + col
                                val app = apps.getOrNull(globalIndex)

                                if (app != null) {
                                    val label = app.loadLabel(pm).toString()
                                    val icon = app.loadIcon(pm)
                                    val isDragging = draggingIndex == globalIndex

                                    Box(
                                        modifier = Modifier
                                            .width(iconSize)
                                            .height(boxHeight)
                                            .pointerInput(globalIndex) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggingIndex = globalIndex
                                                        val itemRect = itemPositions[globalIndex]
                                                        dragStartOffset = Offset(itemRect?.left ?: 0f, itemRect?.top ?: 0f)
                                                        dragOffset = Offset.Zero
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount
                                                    },
                                                    onDragEnd = {
                                                        val target = calculateNearestIndex(globalIndex, dragOffset, itemPositions)
                                                        if (target != null && target != globalIndex) {
                                                            apps = apps.toMutableList().apply {
                                                                val item = removeAt(globalIndex)
                                                                add(target, item)
                                                            }
                                                            saveAppOrder(context, apps)
                                                        }
                                                        draggingIndex = null
                                                        dragOffset = Offset.Zero
                                                    },
                                                    onDragCancel = {
                                                        draggingIndex = null
                                                        dragOffset = Offset.Zero
                                                    }
                                                )
                                            }
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                onAppClick(app)
                                            }
                                            .onGloballyPositioned { coords ->
                                                itemPositions[globalIndex] = coords.boundsInWindow()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isDragging) AppIcon(icon, label, iconSize) else Box(Modifier.size(iconSize))
                                    }
                                } else {
                                    Spacer(Modifier.width(iconSize))
                                }
                            }
                        }
                    }
                }
            }

            // Sayfa kontrol çubuğu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "\u2190",
                    fontSize = 30.sp,
                    color = Color.White,
                    modifier = Modifier.clickable {
                        scope.launch {
                            if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                )
                PageIndicatorDots(totalPages = pages.size, currentPage = pagerState.currentPage)
                Text(
                    "\u2192",
                    fontSize = 30.sp,
                    color = Color.White,
                    modifier = Modifier.clickable {
                        scope.launch {
                            if (pagerState.currentPage < pages.lastIndex) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                )
            }
        }

        // Sürüklenen öğe overlay
        draggingIndex?.let { idx ->
            val app = apps.getOrNull(idx)
            app?.let {
                val label = it.loadLabel(pm).toString()
                val icon = it.loadIcon(pm)
                val currentPos = dragStartOffset + dragOffset

                Box(
                    Modifier
                        .offset { IntOffset(currentPos.x.roundToInt(), currentPos.y.roundToInt()) }
                        .align(Alignment.TopStart)
                        .size(iconSize)
                        .background(Color(0x55FFFFFF), shape = CircleShape)
                ) {
                    AppIcon(icon, label, iconSize)
                }
            }
        }
    }
}

@Composable
fun AppIcon(icon: android.graphics.drawable.Drawable, label: String, size: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.wrapContentSize()
    ) {
        Image(
            bitmap = drawableToBitmap(icon).asImageBitmap(),
            contentDescription = label,
            modifier = Modifier.size(size)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label.take(14).ifEmpty { "Uygulama" },
            fontSize = 8.sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

fun calculateNearestIndex(currentIndex: Int, dragOffset: Offset, itemPositions: Map<Int, Rect>): Int? {
    val draggedRect = itemPositions[currentIndex]?.translate(dragOffset.x, dragOffset.y) ?: return null
    val center = Offset(draggedRect.left + draggedRect.width / 2, draggedRect.top + draggedRect.height / 2)
    return itemPositions.minByOrNull { (_, rect) ->
        val rectCenter = Offset(rect.left + rect.width / 2, rect.top + rect.height / 2)
        (center - rectCenter).getDistance()
    }?.key
}

fun Offset.getDistance(): Float = sqrt(x * x + y * y)

@Composable
fun rememberItemPositions(size: Int): MutableMap<Int, Rect> = remember { mutableStateMapOf() }

fun <T> paginateList(list: List<T>, pageSize: Int): List<List<T>> = list.chunked(pageSize)

@Composable
fun PageIndicatorDots(totalPages: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(totalPages) { index ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(7.dp)
                    .background(if (index == currentPage) Color.White else Color.Gray, shape = CircleShape)
            )
        }
    }
}

fun saveAppOrder(context: Context, apps: List<ResolveInfo>) {
    val prefs = context.getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
    val order = apps.joinToString(",") { it.activityInfo.packageName }
    prefs.edit { putString("app_order", order) }
}

fun loadAppOrder(context: Context): List<String> {
    val prefs = context.getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
    val order = prefs.getString("app_order", "") ?: ""
    return if (order.isNotEmpty()) order.split(",") else emptyList()
}
