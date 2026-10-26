package com.cleantv.launcher

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import java.util.Random
import kotlin.math.sin

val PRESET_NAMES = listOf("Desert Dusk", "Alpine Night", "Aurora", "Ocean Sunset", "City Night", "Misty Hills")

private fun DrawScope.ridge(base: Float, amp: Float, color: Color, seed: Float, f: Float = 1f) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(0f, h)
        val n = 64
        for (i in 0..n) {
            val x = w * i / n
            val t = x / w * 6.2832f * f
            val y = base - amp * (0.5f * sin(t + seed) + 0.3f * sin(2.3f * t + seed * 1.7f) + 0.2f * sin(5.1f * t + seed * 0.6f))
            lineTo(x, y)
        }
        lineTo(w, h)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.stars(count: Int, seed: Long, maxY: Float) {
    val r = Random(seed); val u = size.width / 1000f
    repeat(count) {
        val x = r.nextFloat() * size.width; val y = r.nextFloat() * maxY
        drawCircle(Color.White.copy(alpha = 0.3f + r.nextFloat() * 0.7f), (0.5f + r.nextFloat() * 1.3f) * u, Offset(x, y))
    }
}

private fun DrawScope.glow(cx: Float, cy: Float, radius: Float, color: Color) {
    drawRect(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), Offset(cx, cy), radius))
}

private fun DrawScope.fog(y: Float, hgt: Float) {
    drawRect(
        Brush.verticalGradient(listOf(Color(0x00FFFFFF), Color(0x44FFFFFF), Color(0x00FFFFFF)), startY = y, endY = y + hgt),
        Offset(0f, y), Size(size.width, hgt)
    )
}

private fun DrawScope.cactus(x: Float, baseY: Float, hgt: Float, color: Color) {
    val t = hgt * 0.12f
    drawRoundRect(color, Offset(x - t / 2, baseY - hgt), Size(t, hgt), CornerRadius(t / 2))
    drawRoundRect(color, Offset(x - t * 2.2f, baseY - hgt * 0.55f), Size(t * 0.8f, hgt * 0.3f), CornerRadius(t * 0.4f))
    drawRoundRect(color, Offset(x - t * 2.2f, baseY - hgt * 0.40f), Size(t * 2.0f, t * 0.8f), CornerRadius(t * 0.4f))
    drawRoundRect(color, Offset(x + t * 1.4f, baseY - hgt * 0.72f), Size(t * 0.8f, hgt * 0.34f), CornerRadius(t * 0.4f))
    drawRoundRect(color, Offset(x + t * 0.1f, baseY - hgt * 0.50f), Size(t * 1.9f, t * 0.8f), CornerRadius(t * 0.4f))
}

private fun DrawScope.auroraBand(baseY: Float, thick: Float, color: Color, seed: Float) {
    val w = size.width; val h = size.height; val n = 48
    fun top(x: Float) = baseY + h * 0.05f * (sin(x / w * 9f + seed) + 0.6f * sin(x / w * 17f + seed * 2f))
    val p = Path().apply {
        moveTo(0f, top(0f))
        for (i in 1..n) { val x = w * i / n; lineTo(x, top(x)) }
        for (i in n downTo 0) { val x = w * i / n; lineTo(x, top(x) + thick) }
        close()
    }
    drawPath(p, Brush.verticalGradient(
        listOf(color.copy(alpha = 0f), color.copy(alpha = 0.55f), color.copy(alpha = 0f)),
        startY = baseY - h * 0.08f, endY = baseY + thick + h * 0.08f))
}

/** Draws wallpaper number [i] to fill the current drawing area (used for the screen and the small previews). */
fun DrawScope.paintScene(i: Int) {
    val w = size.width; val h = size.height
    when (i) {
        0 -> {  // Desert Dusk
            drawRect(Brush.verticalGradient(
                listOf(Color(0xFF141C3A), Color(0xFF4B3A6B), Color(0xFFC9707A), Color(0xFFF2A65A)), startY = 0f, endY = h * 0.72f))
            glow(w * 0.72f, h * 0.66f, w * 0.35f, Color(0xBBFFC27A))
            drawCircle(Color(0xFFFFD9A0), w * 0.035f, Offset(w * 0.72f, h * 0.66f))
            ridge(h * 0.70f, h * 0.10f, Color(0xFF5A3F63), 1.3f)
            ridge(h * 0.78f, h * 0.08f, Color(0xFF3A2A4A), 4.1f, 1.4f)
            ridge(h * 0.88f, h * 0.05f, Color(0xFF1C1626), 2.2f, 2f)
            val c = Color(0xFF0E0A14)
            cactus(w * 0.16f, h * 0.97f, h * 0.30f, c)
            cactus(w * 0.30f, h * 0.98f, h * 0.18f, c)
            cactus(w * 0.84f, h * 0.97f, h * 0.26f, c)
        }
        1 -> {  // Alpine Night
            drawRect(Brush.verticalGradient(
                listOf(Color(0xFF04061A), Color(0xFF0E2447), Color(0xFF1F4F7A)), startY = 0f, endY = h * 0.72f))
            stars(110, 9L, h * 0.55f)
            glow(w * 0.28f, h * 0.25f, w * 0.22f, Color(0x55CFE3FF))
            drawCircle(Color(0xFFEAF2FF), w * 0.022f, Offset(w * 0.28f, h * 0.25f))
            ridge(h * 0.62f, h * 0.12f, Color(0xFF1A3558), 0.9f)
            ridge(h * 0.72f, h * 0.10f, Color(0xFF10243E), 3.2f, 1.4f)
            ridge(h * 0.84f, h * 0.07f, Color(0xFF081423), 5.0f, 1.9f)
            drawRect(Brush.verticalGradient(listOf(Color(0x551F4F7A), Color(0xFF050B16)), startY = h * 0.86f, endY = h),
                Offset(0f, h * 0.86f), Size(w, h * 0.14f))
        }
        2 -> {  // Aurora
            drawRect(Brush.verticalGradient(
                listOf(Color(0xFF010710), Color(0xFF06222A), Color(0xFF0C3B3D)), startY = 0f, endY = h * 0.85f))
            stars(90, 3L, h * 0.6f)
            auroraBand(h * 0.22f, h * 0.20f, Color(0xFF3CFFB0), 0.5f)
            auroraBand(h * 0.30f, h * 0.18f, Color(0xFF5AD1FF), 2.1f)
            auroraBand(h * 0.18f, h * 0.16f, Color(0xFFB36BFF), 4.0f)
            ridge(h * 0.86f, h * 0.06f, Color(0xFF04141A), 1.1f, 1.6f)
            ridge(h * 0.94f, h * 0.04f, Color(0xFF020A0E), 3.3f, 2.2f)
        }
        3 -> {  // Ocean Sunset
            val hz = h * 0.62f
            drawRect(Brush.verticalGradient(
                listOf(Color(0xFF1D1B4D), Color(0xFF8E3B7E), Color(0xFFFF7A5A), Color(0xFFFFC27A)), startY = 0f, endY = hz))
            glow(w * 0.5f, hz, w * 0.4f, Color(0xAAFFB070))
            drawCircle(Color(0xFFFFE2A8), w * 0.06f, Offset(w * 0.5f, hz))
            drawRect(Brush.verticalGradient(
                listOf(Color(0xFF7A3A6A), Color(0xFF1A1646), Color(0xFF0B0B2A)), startY = hz, endY = h),
                Offset(0f, hz), Size(w, h - hz))
            for (k in 0 until 14) {
                val ww = w * 0.2f * (1f - k * 0.04f); val y = hz + h * (0.015f + k * 0.022f)
                drawRoundRect(Color(0xFFFFC27A).copy(alpha = 0.55f * (1f - k / 16f)),
                    Offset(w * 0.5f - ww / 2, y), Size(ww, h * 0.006f), CornerRadius(h * 0.003f))
            }
        }
        4 -> {  // City Night
            drawRect(Brush.verticalGradient(
                listOf(Color(0xFF070A24), Color(0xFF2B2260), Color(0xFF8A4A8C)), startY = 0f, endY = h * 0.85f))
            stars(60, 5L, h * 0.45f)
            val r = Random(11L)
            var x = -w * 0.01f
            while (x < w) {
                val bw = w * (0.04f + r.nextFloat() * 0.05f); val bh = h * (0.18f + r.nextFloat() * 0.25f)
                drawRect(Color(0xFF1B1745), Offset(x, h - bh), Size(bw, bh))
                x += bw
            }
            x = -w * 0.005f
            while (x < w) {
                val bw = w * (0.035f + r.nextFloat() * 0.045f); val bh = h * (0.10f + r.nextFloat() * 0.32f)
                drawRect(Color(0xFF0A0A1C), Offset(x, h - bh), Size(bw, bh))
                val step = w * 0.011f
                var wx = x + step * 0.5f
                while (wx < x + bw - step * 0.6f) {
                    var wy = h - bh + step
                    while (wy < h - step) {
                        if (r.nextFloat() < 0.35f)
                            drawRect(Color(0xFFFFD27A).copy(alpha = 0.55f + r.nextFloat() * 0.4f),
                                Offset(wx, wy), Size(step * 0.45f, step * 0.6f))
                        wy += step * 1.5f
                    }
                    wx += step * 1.4f
                }
                x += bw + w * 0.004f
            }
        }
        else -> {  // Misty Hills
            drawRect(Brush.verticalGradient(
                listOf(Color(0xFF16323A), Color(0xFF3E6B64), Color(0xFFA7C4A0)), startY = 0f, endY = h * 0.75f))
            glow(w * 0.3f, h * 0.35f, w * 0.35f, Color(0x55FFF4C8))
            ridge(h * 0.58f, h * 0.10f, Color(0xFF6E9988), 0.7f)
            fog(h * 0.55f, h * 0.15f)
            ridge(h * 0.68f, h * 0.09f, Color(0xFF446E60), 2.6f, 1.3f)
            fog(h * 0.66f, h * 0.14f)
            ridge(h * 0.80f, h * 0.07f, Color(0xFF28493F), 4.4f, 1.7f)
            fog(h * 0.78f, h * 0.12f)
            ridge(h * 0.92f, h * 0.05f, Color(0xFF122823), 1.9f, 2.4f)
        }
    }
}
