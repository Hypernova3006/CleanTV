package com.cleantv.launcher

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Short synthesized UI sounds (no audio files needed): a soft tick for moving, a chime for select, a falling blip for back. */
object Sounds {
    @Volatile var enabled = true
    private var pool: SoundPool? = null
    private var moveId = 0
    private var selectId = 0
    private var backId = 0
    private var lastMove = 0L

    private class Tone(val freq: Float, val dur: Float, val decay: Float)

    fun init(ctx: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        pool = p
        runCatching {
            moveId = p.load(wav(ctx, "move", listOf(Tone(1000f, 0.035f, 90f))), 1)
            selectId = p.load(wav(ctx, "select", listOf(Tone(640f, 0.05f, 60f), Tone(960f, 0.07f, 45f))), 1)
            backId = p.load(wav(ctx, "back", listOf(Tone(520f, 0.05f, 60f), Tone(390f, 0.07f, 45f))), 1)
        }
    }

    fun release() {
        pool?.release()
        pool = null
    }

    fun move() {
        if (!enabled) return
        val now = SystemClock.uptimeMillis()
        if (now - lastMove < 55) return   // keeps holding a button from sounding like a buzz
        lastMove = now
        pool?.play(moveId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun select() { if (enabled) pool?.play(selectId, 0.7f, 0.7f, 1, 0, 1f) }
    fun back() { if (enabled) pool?.play(backId, 0.6f, 0.6f, 1, 0, 1f) }

    private fun wav(ctx: Context, name: String, tones: List<Tone>): String {
        val rate = 22050
        val total = tones.sumOf { (it.dur * rate).toInt() }
        val data = ShortArray(total)
        var pos = 0
        for (t in tones) {
            val n = (t.dur * rate).toInt()
            for (i in 0 until n) {
                val tt = i / rate.toFloat()
                val attack = min(1f, i / (rate * 0.003f))
                val env = attack * exp(-t.decay * tt)
                val s = sin(2.0 * PI * t.freq * tt) + 0.3 * sin(4.0 * PI * t.freq * tt)
                data[pos++] = (s * env * 0.45 * Short.MAX_VALUE).toInt().toShort()
            }
        }
        val bb = ByteBuffer.allocate(44 + total * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + total * 2); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(rate); bb.putInt(rate * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(total * 2)
        for (s in data) bb.putShort(s)
        val f = File(ctx.cacheDir, "snd_$name.wav")
        f.writeBytes(bb.array())
        return f.absolutePath
    }
}
