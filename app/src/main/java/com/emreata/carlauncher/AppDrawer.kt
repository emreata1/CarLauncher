@file:Suppress("DEPRECATION")

package com.emreata.carlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ResolveInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.clipToBounds
import androidx.core.content.edit
import com.emreata.carlauncher.utils.drawableToBitmap

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
    val screenWidthDp = LocalContext.current.resources.displayMetrics.widthPixels /
            LocalContext.current.resources.displayMetrics.density
    val columnsPerRow = (screenWidthDp / 80f).toInt().coerceIn(3, 8)
    val iconSize = remember { (screenWidthDp / columnsPerRow).dp * 0.5f }
    val boxHeight = iconSize + 28.dp
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
    val itemsPerPage = 2 * columnsPerRow
    val pages = paginateList(apps, itemsPerPage)
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val itemPositions = rememberItemPositions()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    val iconCache = remember(initialApps) {
        initialApps.associate { it.activityInfo.packageName to drawableToBitmap(it.loadIcon(pm)).asImageBitmap() }.toMutableMap()
    }
    apps.forEach { app ->
        val pkg = app.activityInfo.packageName
        if (!iconCache.containsKey(pkg)) {
            iconCache[pkg] = drawableToBitmap(app.loadIcon(pm)).asImageBitmap()
        }
    }

    Box(modifier) {

    Column(
        Modifier
            .padding(8.dp, bottom = 56.dp)   // ⬅️ sonra padding → alt kısmı boş bırakır
            .fillMaxWidth()
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xDD000000))   // ⬅️ önce background
            .clipToBounds()
    )  {
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top  // Dikey hizalamayı üstten başlat
            ) { page ->

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly // satırlar arası boşluk eşit
                ) {
                    for (row in 0 until 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        )  {
                            for (col in 0 until columnsPerRow) {
                                val globalIndex = page * itemsPerPage + row * columnsPerRow + col
                                val app = apps.getOrNull(globalIndex)

                                if (app != null) {
                                    val label = app.loadLabel(pm).toString()
                                    val isDragging = draggingIndex == globalIndex
                                    val bitmap = iconCache[app.activityInfo.packageName]!!

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
                                        if (!isDragging) AppIcon(bitmap = bitmap, label, iconSize) else Box(Modifier.size(iconSize))
                                    }
                                } else {
                                    // Boş alan yer tutucu
                                    Spacer(Modifier.width(iconSize).height(boxHeight))
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
                val currentPos = dragStartOffset + dragOffset
                val bitmap = iconCache[app.activityInfo.packageName]!!
                Box(
                    Modifier
                        .offset { IntOffset(currentPos.x.roundToInt(), currentPos.y.roundToInt()) }
                        .size(iconSize)
                        .background(Color(0x55FFFFFF), shape = CircleShape)
                ) {
                    AppIcon(bitmap=bitmap, label, iconSize)

                }
            }
        }

     }


}

@Composable
fun AppIcon(bitmap: androidx.compose.ui.graphics.ImageBitmap, label: String, size: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.wrapContentSize()
    ) {
        Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(size))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label.ifEmpty { "Uygulama" },
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 2
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

@Composable
fun rememberItemPositions(): MutableMap<Int, Rect> = remember { mutableStateMapOf() }

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
