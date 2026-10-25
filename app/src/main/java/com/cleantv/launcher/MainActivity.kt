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

data class AppItem(val label: String, val pkg: String, val launch: Intent, val image: ImageBitmap, val wide: Boolean)

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    fun q(cat: String) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
    return (q(Intent.CATEGORY_LEANBACK_LAUNCHER) + q(Intent.CATEGORY_LAUNCHER))
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .mapNotNull { ri ->
            val ai = ri.activityInfo
            val banner = runCatching { ai.loadBanner(pm) }.getOrNull()
            val wide = banner != null
            val d = banner ?: ai.loadIcon(pm)
            val bmp = runCatching { if (wide) d.toBitmap(320, 180) else d.toBitmap(160, 160) }.getOrNull() ?: return@mapNotNull null
            val launch = pm.getLeanbackLaunchIntentForPackage(ai.packageName)
                ?: pm.getLaunchIntentForPackage(ai.packageName) ?: return@mapNotNull null
            AppItem(ri.loadLabel(pm).toString(), ai.packageName, launch, bmp.asImageBitmap(), wide)
        }.sortedBy { it.label.lowercase() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Launcher() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Launcher() {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf(loadApps(ctx)) }
    var tab by remember { mutableStateOf(0) }
    var watch by remember { mutableStateOf(emptyList<WatchItem>()) }
    BackHandler { tab = 0 }  // Home screen never exits

    // refresh app list periodically so installs/uninstalls show up
    LaunchedEffect(Unit) {
        while (true) {
            apps = loadApps(ctx)
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
    val visible = apps.filter { it.pkg !in hidden }

    fun launchFirst(vararg intents: Intent) {
        for (i in intents) if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        Toast.makeText(ctx, "Not available on this TV", Toast.LENGTH_SHORT).show()
    }
    fun search() = launchFirst(Intent("android.search.action.GLOBAL_SEARCH"), Intent(Intent.ACTION_ASSIST))
    fun voice() = launchFirst(Intent(Intent.ACTION_ASSIST), Intent(Intent.ACTION_VOICE_COMMAND))
    fun systemSettings() = launchFirst(Intent(Settings.ACTION_SETTINGS))

    fun open(a: AppItem) = runCatching { ctx.startActivity(a.launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
          WallpaperLayer(wall, Modifier.fillMaxSize())
          Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp)) {
            TopBar(tab, onTab = { tab = it }, onSearch = ::search, onMic = ::voice, onProfile = ::systemSettings)
            Spacer(Modifier.height(16.dp))
            when (tab) {
                0 -> Home(visible, watch, ::open, ::hide)
                1 -> AllApps(visible, ::open, ::hide)
                else -> Customize(apps, hidden, wall, ::setWall, ::pick, ::toggle)
            }
          }
        }
    }
}

data class Hero(val title: String, val sub: String, val image: ImageBitmap? = null, val icon: ImageBitmap? = null)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Home(apps: List<AppItem>, watch: List<WatchItem>, open: (AppItem) -> Unit, onHide: (AppItem) -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val showWatch = watch.isNotEmpty()
    val heroH = if (showWatch) 72.dp else 180.dp
    // space left for the card images after hero, titles, labels and padding
    val avail = maxHeight - heroH - 16.dp - 74.dp - (if (showWatch) 98.dp else 0.dp)
    val appH = if (showWatch) (avail * 0.42f).coerceIn(60.dp, 99.dp) else 99.dp
    val watchH = (avail - appH).coerceIn(80.dp, 149.dp)
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greet = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    var hero by remember { mutableStateOf<Hero?>(null) }  // follows whichever card is focused
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(heroH).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1B2A4A), Color(0xFF3B1F4A), Color(0xFF0B0B0F))))) {
            Crossfade(hero, animationSpec = tween(250), label = "hero") { h ->
                Box(Modifier.fillMaxSize()) {
                    val art = h?.image
                    if (art != null) {
                        Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(listOf(Color(0xE6000000), Color(0x00000000)))))
                    }
                    val title = h?.title ?: greet
                    val sub = h?.sub ?: date
                    if (showWatch) {
                        Row(Modifier.align(Alignment.CenterStart).padding(horizontal = 28.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Text(sub, color = Color(0xFFC4C7C5), fontSize = 16.sp, maxLines = 1)
                        }
                    } else {
                        Column(Modifier.align(Alignment.BottomStart).padding(32.dp)) {
                            Text(title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sub, color = Color(0xFFC4C7C5), fontSize = 18.sp, maxLines = 1)
                        }
                    }
                    h?.icon?.let {
                        Image(it, null, Modifier.align(Alignment.CenterEnd).padding(end = 28.dp).size(heroH * 0.55f),
                            contentScale = ContentScale.Fit)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Your apps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(4.dp)) {
            items(apps, key = { it.pkg }) {
                AppCard(it, open, appH, onLongClick = { _ -> onHide(it) },
                    onFocus = { a -> hero = Hero(a.label, "Your apps", icon = a.image) })
            }
        }
        if (showWatch) {
            Spacer(Modifier.height(24.dp))
            Text("Continue watching", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(4.dp)) {
                items(watch) {
                    WatchCard(it, watchH, onFocus = { w, img ->
                        val sub = if (w.progress > 0f) "Continue watching · ${(w.progress * 100).toInt()}% watched" else "Continue watching"
                        hero = Hero(w.title, sub, image = img)
                    })
                }
            }
        }
    }
  }
}

@Composable
fun AllApps(apps: List<AppItem>, open: (AppItem) -> Unit, onHide: (AppItem) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(180.dp), contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(apps, key = { it.pkg }) { AppCard(it, open, onLongClick = { _ -> onHide(it) }) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(a: AppItem, open: (AppItem) -> Unit, h: Dp = 99.dp, dim: Boolean = false,
            onLongClick: ((AppItem) -> Unit)? = null, onFocus: ((AppItem) -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(h * 16f / 9f).alpha(if (dim) 0.35f else 1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { open(a) },
            onLongClick = onLongClick?.let { f -> { f(a) } },
            modifier = Modifier.size(h * 16f / 9f, h).onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus?.invoke(a) }
                .then(if (focused) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Surface, focusedContainerColor = Surface)
        ) {
            Image(a.image, a.label, Modifier.fillMaxSize().padding(if (a.wide) 0.dp else 18.dp),
                contentScale = if (a.wide) ContentScale.Crop else ContentScale.Fit)
        }
        Spacer(Modifier.height(8.dp))
        Text(if (dim) "${a.label} (hidden)" else a.label, color = if (focused) Color.White else Color(0xFFC4C7C5), fontSize = 14.sp, maxLines = 1)
    }
}
