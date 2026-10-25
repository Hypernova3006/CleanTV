package com.cleantv.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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

class Preset(val name: String, val brush: Brush)

private fun solid(c: Long) = Brush.verticalGradient(listOf(Color(c), Color(c)))
private fun grad(vararg c: Long) = Brush.verticalGradient(c.map { Color(it) })

val PRESETS = listOf(
    Preset("Midnight", solid(0xFF0B0B0F)),
    Preset("Aurora", grad(0xFF0B1B2B, 0xFF0F3D3E, 0xFF0B0B0F)),
    Preset("Dusk", grad(0xFF1A1033, 0xFF4A1F4A, 0xFF0B0B0F)),
    Preset("Ember", grad(0xFF2A0F0F, 0xFF5C2A12, 0xFF0B0B0F)),
    Preset("Ocean", grad(0xFF04111F, 0xFF0A3B5C, 0xFF0B0B0F)),
)

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
        val i = wall.removePrefix("preset:").toIntOrNull()?.coerceIn(0, PRESETS.lastIndex) ?: 0
        Box(modifier.background(PRESETS[i].brush))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Customize(
    apps: List<AppItem>, hidden: Set<String>, wall: String,
    onWall: (String) -> Unit, onPick: () -> Unit, onToggle: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text("Wallpaper", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(4.dp)) {
            itemsIndexed(PRESETS) { i, p -> Swatch(p.name, p.brush, wall == "preset:$i") { onWall("preset:$i") } }
            item { Swatch("Choose image", null, wall.startsWith("uri:"), onPick) }
        }
        Spacer(Modifier.height(20.dp))
        Text("Hide apps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text("Select an app to hide or show it. You can also long-press an app on Home.",
            color = Color(0xFFC4C7C5), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.weight(1f), contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(apps, key = { it.pkg }) { a -> AppCard(a, { onToggle(a.pkg) }, 72.dp, dim = a.pkg in hidden) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Swatch(label: String, brush: Brush?, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(128.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(128.dp, 72.dp).onFocusChanged { focused = it.isFocused }
                .then(when {
                    focused -> Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                    selected -> Modifier.border(2.dp, Color(0xFF8AB4F8), RoundedCornerShape(12.dp))
                    else -> Modifier
                }),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1E1F25), focusedContainerColor = Color(0xFF1E1F25))
        ) {
            Box(Modifier.fillMaxSize().then(if (brush != null) Modifier.background(brush) else Modifier),
                contentAlignment = Alignment.Center) {
                if (brush == null) Text("+", color = Color.White, fontSize = 28.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (focused || selected) Color.White else Color(0xFFC4C7C5), fontSize = 13.sp, maxLines = 1)
    }
}
