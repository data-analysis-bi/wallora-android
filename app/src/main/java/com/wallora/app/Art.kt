package com.wallora.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

/** Code-generated wallpapers: crisp at any resolution, no image files, no licensing issues. */
object Art {
    private fun col(hex: String) = Color.parseColor("#$hex")
    private fun alpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    fun render(src: Source, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        when (src) {
            is Source.Gradient -> {
                val cols = src.hex.map(::col)
                vGradient(c, w, h, cols)
                glow(c, 0.75f * w, 0.8f * h, 0.45f * h, alpha(cols.last(), 90))
            }
            is Source.Procedural -> procedural(c, w, h, src)
            else -> {}
        }
        return bmp
    }

    private fun vGradient(c: Canvas, w: Int, h: Int, cols: List<Int>) {
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), cols.toIntArray(), null, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    }

    private fun glow(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r, intArrayOf(color, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r, p)
    }

    private fun fill(c: Canvas, path: Path, color: Int) {
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL })
    }

    private fun procedural(c: Canvas, w: Int, h: Int, s: Source.Procedural) {
        val cols = s.palette.map(::col)
        val wf = w.toFloat()
        val hf = h.toFloat()
        when (s.style) {
            "ridges" -> {
                vGradient(c, w, h, listOf(cols[0], cols[1]))
                val layers = cols.drop(2)
                layers.forEachIndexed { i, color ->
                    fill(c, ridgePath(s.seed + i * 17, 0.36f + 0.55f * i / maxOf(layers.size, 1), 0.07f, wf, hf), color)
                }
            }
            "waves" -> {
                vGradient(c, w, h, listOf(cols[0], cols[1]))
                cols.drop(2).forEachIndexed { i, color ->
                    fill(c, wavePath(s.seed + i * 31, 0.42f + 0.14f * i, 0.06f, wf, hf), color)
                }
            }
            "stars" -> {
                vGradient(c, w, h, listOf(cols[0], cols[1]))
                glow(c, 0.3f * wf, 0.78f * hf, 0.45f * hf, alpha(cols[2], 140))
                glow(c, 0.85f * wf, 0.2f * hf, 0.3f * hf, alpha(cols[2], 64))
                stars(c, wf, hf, s.seed, 260, 0.0014f, 217)
                stars(c, wf, hf, s.seed + 1, 70, 0.0026f, 178)
                stars(c, wf, hf, s.seed + 2, 12, 0.0042f, 255)
            }
            "orbs" -> {
                c.drawColor(cols[0])
                orb(c, cols, wf * 1.9f, 0.85f * wf, 0.1f * hf)
                orb(c, cols, wf * 1.5f, 0.2f * wf, 0.9f * hf)
            }
            "grid" -> {
                vGradient(c, w, h, listOf(cols[0], cols[1]))
                glow(c, 0.5f * wf, 0.9f * hf, 0.5f * hf, alpha(cols[2], 100))
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = alpha(cols[2], 71); style = Paint.Style.STROKE; strokeWidth = maxOf(1f, wf * 0.002f) }
                val step = wf / 10f
                var x = 0f
                while (x <= wf) { c.drawLine(x, 0f, x, hf, p); x += step }
                var y = 0f
                while (y <= hf) { c.drawLine(0f, y, wf, y, p); y += step }
            }
            else -> { // rings
                vGradient(c, w, h, listOf(cols[0], cols[1]))
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = alpha(cols[2], 115); style = Paint.Style.STROKE; strokeWidth = maxOf(1f, wf * 0.0016f) }
                for (i in 1..14) c.drawCircle(wf / 2, 0.6f * hf, wf * 0.09f * i, p)
            }
        }
    }

    private fun orb(c: Canvas, cols: List<Int>, d: Float, x: Float, y: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x, y, d / 2, intArrayOf(cols[0], cols[0], alpha(cols[1], 140), cols[2]),
                floatArrayOf(0f, 0.44f, 0.72f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawCircle(x, y, d / 2, p)
    }

    private fun stars(c: Canvas, w: Float, h: Float, seed: Int, count: Int, size: Float, a: Int) {
        val r = Random(seed.toLong())
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(a, 255, 255, 255) }
        repeat(count) {
            val x = r.nextFloat() * w
            val y = r.nextFloat() * h
            val rad = size * w * (0.5f + r.nextFloat() * 0.5f)
            c.drawCircle(x, y, rad, p)
        }
    }

    private fun ridgePath(seed: Int, baseline: Float, amp: Float, w: Float, h: Float): Path {
        val r = Random(seed.toLong())
        fun rnd(a: Double, b: Double) = a + r.nextDouble() * (b - a)
        val p1 = rnd(0.0, 6.28); val p2 = rnd(0.0, 6.28); val p3 = rnd(0.0, 6.28)
        val f1 = rnd(0.8, 1.6); val f2 = rnd(2.0, 3.4); val f3 = rnd(4.5, 7.0)
        val path = Path()
        path.moveTo(0f, h)
        val steps = 120
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val n = sin(t * f1 * PI * 2 + p1) * 0.55 + sin(t * f2 * PI * 2 + p2) * 0.3 + sin(t * f3 * PI * 2 + p3) * 0.15
            path.lineTo((w * t).toFloat(), (h * (baseline - amp * n)).toFloat())
        }
        path.lineTo(w, h)
        path.close()
        return path
    }

    private fun wavePath(seed: Int, baseline: Float, amp: Float, w: Float, h: Float): Path {
        val r = Random(seed.toLong())
        val ph = r.nextDouble() * 6.28
        val fr = 0.7 + r.nextDouble() * 0.8
        val ph2 = r.nextDouble() * 6.28
        val path = Path()
        path.moveTo(0f, h)
        for (i in 0..100) {
            val t = i / 100.0
            val n = sin(t * fr * PI * 2 + ph) * 0.7 + sin(t * fr * 2.3 * PI * 2 + ph2) * 0.3
            path.lineTo((w * t).toFloat(), (h * (baseline - amp * n)).toFloat())
        }
        path.lineTo(w, h)
        path.close()
        return path
    }
}
