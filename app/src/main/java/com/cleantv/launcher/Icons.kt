package com.cleantv.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max

/**
 * Makes every app icon look the same size inside its circle:
 * trims empty margins, then fills the circle for round icons, and insets square icons and
 * transparent logos a little so their corners aren't cut off.
 */
fun normalizeIcon(src: Bitmap, out: Int = 160): Bitmap {
    val w = src.width; val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    fun alpha(x: Int, y: Int) = px[y * w + x] ushr 24

    var minX = w; var minY = h; var maxX = -1; var maxY = -1
    for (y in 0 until h) for (x in 0 until w) if (alpha(x, y) > 24) {
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
    }
    if (maxX < 0) return Bitmap.createScaledBitmap(src, out, out, true)

    val bw = maxX - minX + 1; val bh = maxY - minY + 1
    val corners = listOf(0.12f to 0.12f, 0.88f to 0.12f, 0.12f to 0.88f, 0.88f to 0.88f).map { c ->
        alpha(minX + (bw * c.first).toInt().coerceAtMost(bw - 1), minY + (bh * c.second).toInt().coerceAtMost(bh - 1))
    }
    val squareish = abs(bw - bh) <= 0.15f * max(bw, bh)
    val scale = when {
        squareish && corners.all { it < 100 } -> 1.0f   // already round: fill the circle
        squareish && corners.all { it > 200 } -> 0.9f   // opaque square or rounded-square icon
        else -> 0.78f                                    // logo on a transparent background
    }
    val k = out * scale / max(bw, bh)
    val dw = bw * k; val dh = bh * k
    val res = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888)
    Canvas(res).drawBitmap(
        src, Rect(minX, minY, maxX + 1, maxY + 1),
        RectF((out - dw) / 2, (out - dh) / 2, (out + dw) / 2, (out + dh) / 2),
        Paint(Paint.FILTER_BITMAP_FLAG)
    )
    return res
}
