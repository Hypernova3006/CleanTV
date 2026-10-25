package com.cleantv.launcher

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

const val WATCH_PERMISSION = "com.android.providers.tv.permission.READ_EPG_DATA"

data class WatchItem(val title: String, val poster: Uri?, val progress: Float, val launch: Intent, val engaged: Long)

/** Reads the system "Watch Next" channel that streaming apps publish to. Returns empty if not permitted. */
fun loadWatchNext(ctx: Context): List<WatchItem> = runCatching {
    val out = mutableListOf<WatchItem>()
    ctx.contentResolver.query(
        TvContractCompat.WatchNextPrograms.CONTENT_URI, WatchNextProgram.PROJECTION, null, null, null
    )?.use { c ->
        while (c.moveToNext()) {
            val p = WatchNextProgram.fromCursor(c)
            val intent = p.intent ?: continue
            val dur = p.durationMillis
            val prog = if (dur > 0) (p.lastPlaybackPositionMillis.toFloat() / dur).coerceIn(0f, 1f) else 0f
            out += WatchItem(p.title ?: "", p.posterArtUri ?: p.thumbnailUri, prog,
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), p.lastEngagementTimeUtcMillis)
        }
    }
    out.sortedByDescending { it.engaged }.take(15)
}.getOrDefault(emptyList())

private object Img {
    private val cache = LruCache<String, ImageBitmap>(30)
    suspend fun load(ctx: Context, uri: Uri): ImageBitmap? {
        cache.get(uri.toString())?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val stream = if (uri.scheme == "http" || uri.scheme == "https") URL(uri.toString()).openStream()
                else ctx.contentResolver.openInputStream(uri)
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                stream?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
            }.getOrNull()?.also { cache.put(uri.toString(), it) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WatchCard(w: WatchItem, h: Dp = 149.dp, onFocus: ((WatchItem, ImageBitmap?) -> Unit)? = null) {
    val ctx = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val img by produceState<ImageBitmap?>(null, w.poster) { value = w.poster?.let { Img.load(ctx, it) } }
    LaunchedEffect(focused, img) { if (focused) onFocus?.invoke(w, img) }
    Column(Modifier.width(h * 16f / 9f)) {
        Surface(
            onClick = { runCatching { ctx.startActivity(w.launch) } },
            modifier = Modifier.size(h * 16f / 9f, h).onFocusChanged { focused = it.isFocused }
                .then(if (focused) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1E1F25), focusedContainerColor = Color(0xFF1E1F25))
        ) {
            Box(Modifier.fillMaxSize()) {
                img?.let { Image(it, w.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                if (w.progress > 0f) {
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x66FFFFFF)))
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(w.progress).height(4.dp).background(Color(0xFF8AB4F8)))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(w.title, color = if (focused) Color.White else Color(0xFFC4C7C5), fontSize = 14.sp, maxLines = 1)
    }
}
