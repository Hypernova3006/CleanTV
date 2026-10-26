package com.cleantv.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

private val TileBg = Color(0x99000000)
private val LabelGray = Color(0xFFB8BBC0)

private fun DrawScope.reorderIcon(c: Color) {
    val w = size.width; val sw = w * 0.08f
    drawLine(c, Offset(w * 0.1f, w * 0.32f), Offset(w * 0.9f, w * 0.32f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.9f, w * 0.32f), Offset(w * 0.72f, w * 0.15f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.9f, w * 0.32f), Offset(w * 0.72f, w * 0.49f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.9f, w * 0.68f), Offset(w * 0.1f, w * 0.68f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.1f, w * 0.68f), Offset(w * 0.28f, w * 0.51f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * 0.1f, w * 0.68f), Offset(w * 0.28f, w * 0.85f), sw, StrokeCap.Round)
}

/** Round app icon with its name underneath; used on Home, Apps, Customize and Reorder. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCircle(
    a: AppItem, size: Dp, open: (AppItem) -> Unit, dim: Boolean = false,
    onLongClick: ((AppItem) -> Unit)? = null, highlight: Boolean = false, modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.width(size + 28.dp).zIndex(if (focused) 1f else 0f).then(if (dim) Modifier.alpha(0.35f) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { open(a) },
            onLongClick = onLongClick?.let { f -> { f(a) } },
            modifier = Modifier.size(size).onFocusChanged { focused = it.isFocused }
                .then(if (highlight) Modifier.border(4.dp, Color(0xFF8AB4F8), CircleShape) else Modifier),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = if (highlight) 1.2f else 1.12f),
            colors = ClickableSurfaceDefaults.colors(containerColor = TileBg, focusedContainerColor = TileBg)
        ) {
            Image(a.icon, a.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(8.dp))
        Text(if (highlight) "Moving…" else if (dim) "${a.label} (hidden)" else a.label,
            color = if (focused || highlight) Color.White else LabelGray, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionTile(label: String, size: Dp, square: Boolean, onClick: () -> Unit, draw: DrawScope.(Color) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = if (square) RoundedCornerShape(16.dp) else CircleShape
    Column(Modifier.width(size + 28.dp).zIndex(if (focused) 1f else 0f), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(size).onFocusChanged { focused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(shape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
            colors = ClickableSurfaceDefaults.colors(containerColor = TileBg, focusedContainerColor = TileBg)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(size * 0.38f)) { draw(Color.White) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = if (focused) Color.White else LabelGray, fontSize = 14.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Home(
    apps: List<AppItem>, watch: List<WatchItem>, open: (AppItem) -> Unit, onHide: (AppItem) -> Unit,
    onReorder: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val s = (maxHeight.value / 330f).coerceIn(0.7f, 1f)   // shrink a little on small screens
        val circle = 75.dp * s
        val cardH = 110.dp * s
        // rows sit at the bottom so the wallpaper stays visible above them
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            if (watch.isNotEmpty()) {
                Text("Continue watching", color = Color(0xFFE3E3E3), fontSize = 19.sp, modifier = Modifier.padding(start = 34.dp))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 34.dp, vertical = 10.dp)) {
                    items(watch) { WatchCard(it, cardH) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text("Your apps", color = Color.White, fontSize = 26.sp, modifier = Modifier.padding(start = 34.dp))
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)) {
                items(apps, key = { it.pkg }) { a -> AppCircle(a, circle, open, onLongClick = { _ -> onHide(a) }) }
                item { ActionTile("Reorder", circle, true, onReorder) { reorderIcon(it) } }
            }
        }
    }
}
