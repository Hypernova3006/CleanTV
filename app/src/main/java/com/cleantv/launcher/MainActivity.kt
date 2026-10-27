package com.cleantv.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val Bg = Color(0xFF0B0B0F)
private val Surface = Color(0xFF1E1F25)
private val Pill = Color(0xFFE8EAED)

data class AppItem(val label: String, val pkg: String, val launch: Intent, val icon: ImageBitmap)

private val iconCache = HashMap<String, ImageBitmap>()

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    fun q(cat: String) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
    return (q(Intent.CATEGORY_LEANBACK_LAUNCHER) + q(Intent.CATEGORY_LAUNCHER))
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .mapNotNull { ri ->
            val ai = ri.activityInfo
            val icon = iconCache[ai.packageName]
                ?: runCatching { normalizeIcon(ri.loadIcon(pm).toBitmap(192, 192)).asImageBitmap() }.getOrNull()
                    ?.also { iconCache[ai.packageName] = it }
                ?: return@mapNotNull null
            val launch = pm.getLeanbackLaunchIntentForPackage(ai.packageName)
                ?: pm.getLaunchIntentForPackage(ai.packageName) ?: return@mapNotNull null
            AppItem(ri.loadLabel(pm).toString(), ai.packageName, launch, icon)
        }.sortedBy { it.label.lowercase() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Sounds.init(this)
        setContent { Launcher() }
    }

    // one place to play navigation sounds for every screen
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> Sounds.move()
                android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER,
                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> if (event.repeatCount == 0) Sounds.select()
                android.view.KeyEvent.KEYCODE_BACK -> if (event.repeatCount == 0) Sounds.back()
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        Sounds.release()
        super.onDestroy()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Launcher() {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppItem>()) }
    var tab by remember { mutableStateOf(0) }
    var backdrop by remember { mutableStateOf<Backdrop?>(null) }
    LaunchedEffect(tab) { backdrop = null }
    var watch by remember { mutableStateOf(emptyList<WatchItem>()) }
    BackHandler { tab = 0 }  // Home screen never exits

    // refresh app list periodically so installs/uninstalls show up
    LaunchedEffect(Unit) {
        while (true) {
            val fresh = withContext(Dispatchers.IO) { loadApps(ctx) }
            if (fresh.map { it.pkg } != apps.map { it.pkg }) apps = fresh
            watch = withContext(Dispatchers.IO) { loadWatchNext(ctx) }
            delay(15_000)
        }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) watch = loadWatchNext(ctx)
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, WATCH_PERMISSION) != PackageManager.PERMISSION_GRANTED)
            runCatching { permLauncher.launch(WATCH_PERMISSION) }
    }

    val prefs = remember { Prefs(ctx) }
    var hidden by remember { mutableStateOf(prefs.hidden) }
    var wall by remember { mutableStateOf(prefs.wallpaper) }
    var place by remember { mutableStateOf(prefs.place) }
    var fahrenheit by remember { mutableStateOf(prefs.fahrenheit) }
    var sounds by remember { mutableStateOf(prefs.sounds) }
    SideEffect { Sounds.enabled = sounds }
    var weather by remember { mutableStateOf<Weather?>(null) }
    LaunchedEffect(place, fahrenheit) {
        weather = null
        while (true) {
            fetchWeather(place, fahrenheit)?.let { weather = it }
            delay(30 * 60_000L)
        }
    }
    fun toggle(pkg: String) { hidden = if (pkg in hidden) hidden - pkg else hidden + pkg; prefs.hidden = hidden }
    fun setWall(w: String) { wall = w; prefs.wallpaper = w }
    fun hide(a: AppItem) {
        toggle(a.pkg)
        Toast.makeText(ctx, "${a.label} hidden. Restore it in Customize.", Toast.LENGTH_SHORT).show()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            setWall("uri:$uri")
        }
    }
    fun pick() {
        try { picker.launch(arrayOf("image/*")) }
        catch (e: Exception) { Toast.makeText(ctx, "No file picker on this TV. Use a preset.", Toast.LENGTH_LONG).show() }
    }
    var order by remember { mutableStateOf(prefs.order) }
    val ordered = apps.sortedWith(compareBy({ a -> order.indexOf(a.pkg).let { if (it < 0) Int.MAX_VALUE else it } }, { a -> a.label.lowercase() }))
    val visible = ordered.filter { it.pkg !in hidden }
    // swap an app with its neighbour in the visible list (delta = -1 left, +1 right)
    fun move(pkg: String, delta: Int) {
        val vis = visible.map { it.pkg }
        val i = vis.indexOf(pkg)
        val j = i + delta
        if (i < 0 || j !in vis.indices) return
        val full = ordered.map { it.pkg }.toMutableList()
        java.util.Collections.swap(full, full.indexOf(vis[i]), full.indexOf(vis[j]))
        order = full
        prefs.order = full
    }

    fun launchFirst(vararg intents: Intent?) {
        for (i in intents) {
            if (i == null) continue
            if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
        Toast.makeText(ctx, "Not available on this TV", Toast.LENGTH_SHORT).show()
    }
    fun live() = launchFirst(
        Intent(Intent.ACTION_VIEW, android.media.tv.TvContract.Channels.CONTENT_URI),
        ctx.packageManager.getLeanbackLaunchIntentForPackage("com.google.android.tv"))
    fun voice() = launchFirst(Intent(Intent.ACTION_ASSIST), Intent(Intent.ACTION_VOICE_COMMAND))
    fun wifiSettings() = launchFirst(Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun systemSettings() = launchFirst(Intent(Settings.ACTION_SETTINGS))

    fun open(a: AppItem) = runCatching { ctx.startActivity(a.launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
          WallpaperLayer(wall, Modifier.fillMaxSize())
          BackdropLayer(backdrop, Modifier.fillMaxSize())
          Column(Modifier.fillMaxSize().padding(start = 56.dp, end = 56.dp, top = 32.dp, bottom = 28.dp)) {
            TopBar(
                selected = when (tab) { 0 -> 0; 1 -> 2; else -> -1 },
                onTab = { i -> when (i) { 0 -> { tab = 0 }; 1 -> { live() }; else -> { tab = 1 } } },
                onMic = ::voice, onWallpaper = { tab = 2 }, onSettings = ::systemSettings, onWifi = ::wifiSettings, weather = weather, onFocusTop = { backdrop = null })
            Spacer(Modifier.height(16.dp))
            when (tab) {
                0 -> Home(visible, watch, ::open, ::hide, onReorder = { tab = 3 }, onBackdrop = { backdrop = it })
                1 -> AllApps(visible, ::open, ::hide, onBackdrop = { backdrop = it })
                2 -> Customize(ordered, hidden, wall, ::setWall, ::pick, ::toggle,
                    placeName = place.name, fahrenheit = fahrenheit,
                    onUnit = { fahrenheit = !fahrenheit; prefs.fahrenheit = fahrenheit },
                    onPlace = { place = it; prefs.place = it },
                    soundsOn = sounds, onSounds = { sounds = !sounds; prefs.sounds = sounds })
                else -> ReorderScreen(visible, ::move)
            }
          }
        }
    }
}

@Composable
fun AllApps(apps: List<AppItem>, open: (AppItem) -> Unit, onHide: (AppItem) -> Unit, onBackdrop: (Backdrop?) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(110.dp), contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(apps, key = { it.pkg }) { a ->
            AppCircle(a, 75.dp, open, onLongClick = { _ -> onHide(a) },
                onFocus = { x -> onBackdrop(Backdrop.Tint(tintOf(x))) })
        }
    }
}
