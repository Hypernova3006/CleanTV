package com.cleantv.launcher

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.math.min

/** What the background should react to: an app's colour, or a show's artwork. */
sealed class Backdrop {
    data class Tint(val color: Color) : Backdrop()
    data class Art(val uri: Uri) : Backdrop()
}

private val tintCache = HashMap<String, Color>()

fun tintOf(a: AppItem): Color = tintCache.getOrPut(a.pkg) { dominantTint(a.icon) }

/** Picks the most vivid colour in an icon, then tones it so it works as a soft glow. */
private fun dominantTint(icon: ImageBitmap): Color {
    val bmp = icon.asAndroidBitmap()
    val step = max(1, bmp.width / 16)
    var r = 0f; var g = 0f; var b = 0f; var total = 0f
    for (y in 0 until bmp.height step step) for (x in 0 until bmp.width step step) {
        val p = bmp.getPixel(x, y)
        if ((p ushr 24) < 128) continue
        val rr = (p shr 16) and 0xFF; val gg = (p shr 8) and 0xFF; val bb = p and 0xFF
        val mx = max(rr, max(gg, bb)); val mn = min(rr, min(gg, bb))
        val sat = if (mx == 0) 0f else (mx - mn) / mx.toFloat()
        val w = 0.05f + sat * sat   // vivid pixels count far more than greys
        r += rr * w; g += gg * w; b += bb * w; total += w
    }
    if (total == 0f) return Color(0xFF3A3F5C)
    val argb = android.graphics.Color.rgb((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    hsv[1] = hsv[1].coerceIn(0.35f, 1f)
    hsv[2] = hsv[2].coerceIn(0.45f, 0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Sits on top of the wallpaper and fades to match whatever is selected. */
@Composable
fun BackdropLayer(b: Backdrop?, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Crossfade(targetState = b, modifier = modifier, animationSpec = tween(400), label = "backdrop") { cur ->
        when (cur) {
            null -> Box(Modifier.fillMaxSize())
            is Backdrop.Tint -> Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, cur.color.copy(alpha = 0.55f)))))
            is Backdrop.Art -> {
                val img by produceState<ImageBitmap?>(null, cur.uri) { value = Img.load(ctx, cur.uri) }
                Box(Modifier.fillMaxSize()) {
                    img?.let {
                        Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        // dark overlay so the rows and text stay readable
                        Box(Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0x99000000), Color(0xE6000000)))))
                    }
                }
            }
        }
    }
}
