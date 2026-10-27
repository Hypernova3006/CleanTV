package com.cleantv.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
private val TbText = Color(0xFFCFD0D2)

private fun DrawScope.micIcon(c: Color) {
    val w = size.width; val sw = w * 0.09f
    drawRoundRect(c, Offset(w * 0.37f, w * 0.12f), Size(w * 0.26f, w * 0.46f), CornerRadius(w * 0.13f))
    drawArc(c, 0f, 180f, false, Offset(w * 0.25f, w * 0.28f), Size(w * 0.5f, w * 0.44f), style = Stroke(sw))
    drawLine(c, Offset(w * 0.5f, w * 0.72f), Offset(w * 0.5f, w * 0.88f), sw, StrokeCap.Round)
}

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

/** Solid gear: 8 teeth around a body with a round hole in the middle. */
private fun DrawScope.gearIcon(c: Color) {
    val w = size.width; val cx = w / 2; val cy = w / 2
    val rOut = (w * 0.47f).toDouble(); val rIn = (w * 0.35f).toDouble()
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        for (i in 0 until 8) {
            val b = i * 45.0
            val pts = listOf(b - 11 to rIn, b - 7 to rOut, b + 7 to rOut, b + 11 to rIn)
            pts.forEachIndexed { k, pt ->
                val a = Math.toRadians(pt.first)
                val x = cx + (pt.second * cos(a)).toFloat()
                val y = cy + (pt.second * sin(a)).toFloat()
                if (i == 0 && k == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        close()
        addOval(Rect(cx - w * 0.16f, cy - w * 0.16f, cx + w * 0.16f, cy + w * 0.16f))
    }
    drawPath(path, c)
}

private fun DrawScope.wifiIcon(c: Color) {
    val w = size.width; val sw = w * 0.09f
    val cx = w * 0.5f; val cy = w * 0.8f
    for (r in listOf(0.6f, 0.4f, 0.2f)) {
        val rad = w * r
        drawArc(c, 225f, 90f, false, Offset(cx - rad, cy - rad), Size(rad * 2, rad * 2),
            style = Stroke(width = sw, cap = StrokeCap.Round))
    }
    drawCircle(c, w * 0.07f, Offset(cx, cy))
}

/** True while the TV is connected to Wi-Fi; updates live as the connection comes and goes. */
@Composable
private fun rememberWifiConnected(): Boolean {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val nets = mutableSetOf<Network>()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { synchronized(nets) { nets.add(network); on = true } }
            override fun onLost(network: Network) { synchronized(nets) { nets.remove(network); on = nets.isNotEmpty() } }
        }
        val req = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        runCatching { cm?.registerNetworkCallback(req, cb) }
        onDispose { runCatching { cm?.unregisterNetworkCallback(cb) } }
    }
    return on
}

private fun DrawScope.cloud(c: Color, dy: Float = 0f) {
    val w = size.width
    drawCircle(c, w * 0.17f, Offset(w * 0.32f, w * (0.52f + dy)))
    drawCircle(c, w * 0.23f, Offset(w * 0.52f, w * (0.42f + dy)))
    drawCircle(c, w * 0.16f, Offset(w * 0.72f, w * (0.54f + dy)))
    drawRoundRect(c, Offset(w * 0.15f, w * (0.52f + dy)), Size(w * 0.7f, w * 0.18f), CornerRadius(w * 0.09f))
}

private fun DrawScope.weatherIcon(c: Color, kind: Int) {
    val w = size.width; val sw = w * 0.08f
    when (kind) {
        SUN -> {
            val ctr = Offset(w / 2, w / 2)
            drawCircle(c, w * 0.2f, ctr, style = Stroke(sw))
            for (k in 0 until 8) {
                val a = Math.toRadians(k * 45.0)
                val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
                drawLine(c, Offset(ctr.x + ca * w * 0.3f, ctr.y + sa * w * 0.3f),
                    Offset(ctr.x + ca * w * 0.43f, ctr.y + sa * w * 0.43f), sw, StrokeCap.Round)
            }
        }
        MOON -> {
            drawCircle(c, w * 0.36f, Offset(w * 0.46f, w * 0.5f))
            drawCircle(Color.Black, w * 0.3f, Offset(w * 0.62f, w * 0.4f), blendMode = BlendMode.Clear)
        }
        CLOUD -> cloud(c)
        FOG -> for (y in listOf(0.32f, 0.5f, 0.68f))
            drawLine(c, Offset(w * 0.15f, w * y), Offset(w * 0.85f, w * y), sw, StrokeCap.Round)
        RAIN -> {
            cloud(c, -0.12f)
            for (x in listOf(0.3f, 0.5f, 0.7f))
                drawLine(c, Offset(w * x, w * 0.72f), Offset(w * (x - 0.06f), w * 0.9f), sw, StrokeCap.Round)
        }
        SNOW -> {
            cloud(c, -0.12f)
            for (x in listOf(0.3f, 0.5f, 0.7f)) drawCircle(c, w * 0.05f, Offset(w * x, w * 0.82f))
        }
        else -> {
            cloud(c, -0.12f)
            drawLine(c, Offset(w * 0.54f, w * 0.62f), Offset(w * 0.42f, w * 0.78f), sw, StrokeCap.Round)
            drawLine(c, Offset(w * 0.42f, w * 0.78f), Offset(w * 0.58f, w * 0.78f), sw, StrokeCap.Round)
            drawLine(c, Offset(w * 0.58f, w * 0.78f), Offset(w * 0.46f, w * 0.94f), sw, StrokeCap.Round)
        }
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
private fun TabPill(label: String, selected: Boolean, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
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
fun TopBar(
    selected: Int, onTab: (Int) -> Unit, onMic: () -> Unit,
    onWallpaper: () -> Unit, onSettings: () -> Unit, onWifi: () -> Unit, weather: Weather?, onFocusTop: () -> Unit, onLiveLongPress: () -> Unit
) {
    val ctx = LocalContext.current
    val wifi = rememberWifiConnected()
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(10_000) } }
    val is24 = android.text.format.DateFormat.is24HourFormat(ctx)
    Row(Modifier.fillMaxWidth().onFocusChanged { if (it.hasFocus) onFocusTop() }, verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clip(RoundedCornerShape(50)).background(TbGlass).padding(6.dp)) {
            RoundIconButton(onMic) { micIcon(it) }
        }
        Spacer(Modifier.width(14.dp))
        Row(Modifier.clip(RoundedCornerShape(50)).background(TbGlass).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Home", "Live", "Apps").forEachIndexed { i, t -> TabPill(t, selected == i, onLongClick = if (i == 1) onLiveLongPress else null) { onTab(i) } }
        }
        Spacer(Modifier.width(14.dp))
        Row(Modifier.clip(RoundedCornerShape(50)).background(TbGlass).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RoundIconButton(onWallpaper) { wallpaperIcon(it) }
            RoundIconButton(onSettings) { gearIcon(it) }
        }
        if (wifi) {
            Spacer(Modifier.width(14.dp))
            Row(Modifier.clip(RoundedCornerShape(50)).background(TbGlass).padding(6.dp)) {
                RoundIconButton(onWifi) { wifiIcon(it) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(now),
                    color = TbText, fontSize = 22.sp)
                if (!is24) {
                    Text(SimpleDateFormat("a", Locale.getDefault()).format(now).uppercase(),
                        color = TbText, fontSize = 14.sp, modifier = Modifier.padding(start = 5.dp, bottom = 3.dp))
                }
            }
            if (weather != null) {
                Box(Modifier.padding(horizontal = 14.dp).width(2.dp).height(24.dp).background(Color(0x80FFFFFF)))
                Canvas(Modifier.size(24.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                    weatherIcon(TbText, weather.kind)
                }
                Spacer(Modifier.width(6.dp))
                Text("${weather.temp}\u00B0", color = TbText, fontSize = 22.sp)
            }
        }
    }
}
