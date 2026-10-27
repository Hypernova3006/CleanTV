package com.cleantv.launcher

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private fun toCircle(b: Bitmap): Bitmap {
    val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.shader = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    Canvas(out).drawCircle(b.width / 2f, b.height / 2f, b.width / 2f, p)
    return out
}

/**
 * Turns any app icon into a perfect, fully filled circle:
 * - square or rounded-square icons are cropped to fill the circle,
 * - icons that are already round are stretched to fill it,
 * - logos on a transparent background are placed on a solid light or dark circle.
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
    if (maxX < 0) return toCircle(Bitmap.createScaledBitmap(src, out, out, true))

    val bw = maxX - minX + 1; val bh = maxY - minY + 1
    val corners = listOf(0.12f to 0.12f, 0.88f to 0.12f, 0.12f to 0.88f, 0.88f to 0.88f).map { c ->
        alpha(minX + (bw * c.first).toInt().coerceAtMost(bw - 1), minY + (bh * c.second).toInt().coerceAtMost(bh - 1))
    }
    val cornersOpaque = corners.all { it > 200 }
    val cornersClear = corners.all { it < 100 }
    val squareish = abs(bw - bh) <= 0.15f * max(bw, bh)

    var opaque = 0; var rSum = 0L; var gSum = 0L; var bSum = 0L
    for (y in minY..maxY) for (x in minX..maxX) {
        val p = px[y * w + x]
        if ((p ushr 24) > 128) {
            opaque++
            rSum += (p shr 16) and 0xFF; gSum += (p shr 8) and 0xFF; bSum += p and 0xFF
        }
    }
    val fill = opaque.toFloat() / (bw * bh)

    val tmp = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888)
    val c = Canvas(tmp)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    val full = RectF(0f, 0f, out.toFloat(), out.toFloat())
    when {
        cornersOpaque -> {  // full-bleed icon: centre-crop to a square and fill the circle
            val side = min(bw, bh)
            val sx = minX + (bw - side) / 2; val sy = minY + (bh - side) / 2
            c.drawBitmap(src, Rect(sx, sy, sx + side, sy + side), full, paint)
        }
        cornersClear && squareish && fill > 0.6f ->  // already round
            c.drawBitmap(src, Rect(minX, minY, maxX + 1, maxY + 1), full, paint)
        else -> {  // logo on a transparent background: sit it on a solid circle
            val n = max(1, opaque)
            val lum = (0.299 * (rSum / n) + 0.587 * (gSum / n) + 0.114 * (bSum / n)) / 255.0
            c.drawColor(if (lum > 0.55) 0xFF1F2024.toInt() else 0xFFF1F3F4.toInt())
            val k = out * 0.62f / max(bw, bh)
            val dw = bw * k; val dh = bh * k
            c.drawBitmap(src, Rect(minX, minY, maxX + 1, maxY + 1),
                RectF((out - dw) / 2, (out - dh) / 2, (out + dw) / 2, (out + dh) / 2), paint)
        }
    }
    return toCircle(tmp)
}
