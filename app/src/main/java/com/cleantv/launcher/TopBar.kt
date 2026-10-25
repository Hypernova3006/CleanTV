package com.cleantv.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val TbPill = Color(0xFFE8EAED)
private val TbGlass = Color(0x33FFFFFF)
private val TbText = Color(0xFFC4C7C5)

private fun DrawScope.searchIcon(c: Color) {
    val w = size.width; val sw = w * 0.1f
    drawCircle(c, radius = w * 0.3f, center = Offset(w * 0.42f, w * 0.42f), style = Stroke(sw))
    drawLine(c, Offset(w * 0.64f, w * 0.64f), Offset(w * 0.86f, w * 0.86f), sw, StrokeCap.Round)
}

private fun DrawScope.micIcon(c: Color) {
    val w = size.width; val sw = w * 0.09f
    drawRoundRect(c, Offset(w * 0.37f, w * 0.12f), Size(w * 0.26f, w * 0.46f), CornerRadius(w * 0.13f))
    drawArc(c, 0f, 180f, false, Offset(w * 0.25f, w * 0.28f), Size(w * 0.5f, w * 0.44f), style = Stroke(sw))
    drawLine(c, Offset(w * 0.5f, w * 0.72f), Offset(w * 0.5f, w * 0.88f), sw, StrokeCap.Round)
}

private fun DrawScope.personIcon(c: Color) {
    val w = size.width
    drawCircle(c, radius = w * 0.19f, center = Offset(w * 0.5f, w * 0.34f))
    drawArc(c, 180f, 180f, true, Offset(w * 0.16f, w * 0.58f), Size(w * 0.68f, w * 0.7f))
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RoundIconButton(onClick: () -> Unit, size: Dp = 40.dp, draw: DrawScope.(Color) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = TbGlass, focusedContainerColor = TbPill)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val c = if (focused) Color.Black else Color.White
            Canvas(Modifier.size(size * 0.55f)) { draw(c) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(selected: Int, onTab: (Int) -> Unit, onSearch: () -> Unit, onMic: () -> Unit, onProfile: () -> Unit) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(10_000) } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // search pill + mic
        var sFocused by remember { mutableStateOf(false) }
        Surface(
            onClick = onSearch,
            modifier = Modifier.height(40.dp).onFocusChanged { sFocused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
            colors = ClickableSurfaceDefaults.colors(containerColor = TbGlass, focusedContainerColor = TbPill)
        ) {
            val c = if (sFocused) Color.Black else Color.White
            Row(Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(20.dp)) { searchIcon(c) }
                Text("Search", color = c, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.width(8.dp))
        RoundIconButton(onMic) { micIcon(it) }
        Spacer(Modifier.width(24.dp))

        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Home", "Apps", "Customize").forEachIndexed { i, t ->
                var focused by remember { mutableStateOf(false) }
                val active = focused || selected == i
                Surface(
                    onClick = { onTab(i) },
                    modifier = Modifier.onFocusChanged { focused = it.isFocused },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (active) TbPill else Color.Transparent,
                        focusedContainerColor = TbPill)
                ) {
                    Text(t, color = if (active) Color.Black else TbText, fontSize = 16.sp,
                        fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                }
            }
        }
        Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(now), color = Color.White, fontSize = 18.sp)
        Spacer(Modifier.width(16.dp))
        RoundIconButton(onProfile) { personIcon(it) }
    }
}
