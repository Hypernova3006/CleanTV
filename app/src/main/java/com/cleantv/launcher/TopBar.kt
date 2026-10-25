package com.cleantv.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

private val TbPill = Color(0xFFE8EAED)
private val TbGlass = Color(0x38FFFFFF)
private val TbSel = Color(0x40FFFFFF)

private fun DrawScope.wallpaperIcon(c: Color) {
    val w = size.width; val sw = w * 0.09f
    drawRoundRect(c, Offset(w * 0.1f, w * 0.18f), Size(w * 0.8f, w * 0.64f), CornerRadius(w * 0.1f), style = Stroke(sw))
    drawCircle(c, w * 0.07f, Offset(w * 0.34f, w * 0.38f))
    val p = Path().apply {
        moveTo(w * 0.14f, w * 0.74f); lineTo(w * 0.42f, w * 0.5f); lineTo(w * 0.58f, w * 0.64f)
        lineTo(w * 0.68f, w * 0.55f); lineTo(w * 0.86f, w * 0.74f)
    }
    drawPath(p, c, style = Stroke(width = sw * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.gearIcon(c: Color) {
    val w = size.width; val ctr = Offset(w / 2, w / 2); val sw = w * 0.1f
    drawCircle(c, w * 0.2f, ctr, style = Stroke(sw))
    for (k in 0 until 8) {
        val a = Math.toRadians(k * 45.0)
        val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
        drawLine(c, Offset(ctr.x + ca * w * 0.3f, ctr.y + sa * w * 0.3f),
            Offset(ctr.x + ca * w * 0.42f, ctr.y + sa * w * 0.42f), sw, StrokeCap.Round)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RoundIconButton(onClick: () -> Unit, draw: DrawScope.(Color) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = TbPill)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val c = if (focused) Color.Black else Color.White
            Canvas(Modifier.size(24.dp)) { draw(c) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) TbSel else Color.Transparent, focusedContainerColor = TbPill)
    ) {
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 18.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
    }
}

/** selected: 0 = Home, 1 = Live, 2 = Apps, -1 = none. onTab gets the same index. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(selected: Int, onTab: (Int) -> Unit, onWallpaper: () -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(10_000) } }
    val fmt = if (android.text.format.DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clip(RoundedCornerShape(50)).background(TbGlass).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Home", "Live", "Apps").forEachIndexed { i, t -> TabPill(t, selected == i) { onTab(i) } }
        }
        Spacer(Modifier.width(14.dp))
        Row(Modifier.clip(RoundedCornerShape(50)).background(TbGlass).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RoundIconButton(onWallpaper) { wallpaperIcon(it) }
            RoundIconButton(onSettings) { gearIcon(it) }
        }
        Spacer(Modifier.weight(1f))
        Text(SimpleDateFormat(fmt, Locale.getDefault()).format(now), color = Color(0xFFCFD0D2), fontSize = 22.sp)
    }
}
