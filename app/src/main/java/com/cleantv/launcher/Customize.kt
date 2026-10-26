package com.cleantv.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private suspend fun loadWallpaper(ctx: Context, uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var s = 1
        while (bounds.outWidth / (s * 2) >= 1920) s *= 2
        val o = BitmapFactory.Options().apply { inSampleSize = s; inPreferredConfig = Bitmap.Config.RGB_565 }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun WallpaperLayer(wall: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    if (wall.startsWith("uri:")) {
        val img by produceState<ImageBitmap?>(null, wall) { value = loadWallpaper(ctx, Uri.parse(wall.removePrefix("uri:"))) }
        Box(modifier.background(Color(0xFF0B0B0F))) {
            img?.let {
                Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color(0x99000000)))  // scrim keeps text readable
            }
        }
    } else {
        val i = wall.removePrefix("preset:").toIntOrNull()?.coerceIn(0, PRESET_NAMES.lastIndex) ?: 0
        Canvas(modifier) {
            paintScene(i)
            drawRect(Color(0x40000000))  // slight dimming keeps the white text readable
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0x38FFFFFF), focusedContainerColor = Color(0xFFE8EAED))
    ) {
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Customize(
    apps: List<AppItem>, hidden: Set<String>, wall: String,
    onWall: (String) -> Unit, onPick: () -> Unit, onToggle: (String) -> Unit,
    placeName: String, fahrenheit: Boolean, onUnit: () -> Unit, onPlace: (Place) -> Unit
) {
    var searching by remember { mutableStateOf(false) }
    if (searching) {
        CitySearch(onPick = { onPlace(it); searching = false }, onCancel = { searching = false })
    } else Column(Modifier.fillMaxSize()) {
        Text("Wallpaper", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(4.dp)) {
            itemsIndexed(PRESET_NAMES) { i, n -> Swatch(n, i, wall == "preset:$i") { onWall("preset:$i") } }
            item { Swatch("Choose image", null, wall.startsWith("uri:"), onPick) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Weather", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            PillButton("Change city") { searching = true }
            PillButton(if (fahrenheit) "\u00B0F" else "\u00B0C", onUnit)
            Text(placeName, color = Color(0xFFC4C7C5), fontSize = 14.sp, maxLines = 1)
        }
        Spacer(Modifier.height(12.dp))
        Text("Show or hide apps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text("Select an app to hide or show it. You can also long-press an app on Home.",
            color = Color(0xFFC4C7C5), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(GridCells.Adaptive(100.dp), Modifier.weight(1f), contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(apps, key = { it.pkg }) { a -> AppCircle(a, 60.dp, { onToggle(a.pkg) }, dim = a.pkg in hidden) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CitySearch(onPick: (Place) -> Unit, onCancel: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    fun go() {
        if (query.isBlank()) return
        status = "Searching..."
        scope.launch {
            results = searchPlaces(query.trim())
            status = if (results.isEmpty()) "No results. Check the spelling and your internet connection." else ""
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxSize()) {
        Text("Change weather city", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text("Type a city name, then choose Search or press Search on the keyboard.",
            color = Color(0xFFC4C7C5), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { go() }),
            modifier = Modifier.fillMaxWidth(0.6f).focusRequester(focus)
                .background(Color(0x38FFFFFF), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF8AB4F8), RoundedCornerShape(12.dp)).padding(16.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Search") { go() }
            PillButton("Cancel", onCancel)
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(status, color = Color(0xFFC4C7C5), fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(4.dp)) {
            items(results) { p -> PillButton(p.name) { onPick(p) } }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Swatch(label: String, scene: Int?, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(112.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(112.dp, 63.dp).onFocusChanged { focused = it.isFocused }
                .then(when {
                    focused -> Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                    selected -> Modifier.border(2.dp, Color(0xFF8AB4F8), RoundedCornerShape(12.dp))
                    else -> Modifier
                }),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1E1F25), focusedContainerColor = Color(0xFF1E1F25))
        ) {
            if (scene != null) Canvas(Modifier.fillMaxSize()) { paintScene(scene) }
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("+", color = Color.White, fontSize = 28.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (focused || selected) Color.White else Color(0xFFC4C7C5), fontSize = 13.sp, maxLines = 1)
    }
}
