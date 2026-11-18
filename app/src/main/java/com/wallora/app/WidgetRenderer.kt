package com.wallora.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Draws widgets as images. The in-app preview and the real Home Screen widget use this same code,
 * so what you design is exactly what you get (Devanagari and Gujarati text included).
 */
object WidgetRenderer {
    private fun typeface(f: FontStyle, bold: Boolean): Typeface {
        val base = when (f) {
            FontStyle.SERIF -> Typeface.SERIF
            FontStyle.ROUNDED -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            FontStyle.STANDARD -> Typeface.SANS_SERIF
            FontStyle.MONO -> Typeface.MONOSPACE
        }
        return Typeface.create(base, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun layout(text: String, size: Float, color: Int, tf: Typeface, width: Int, maxLines: Int = Int.MAX_VALUE): StaticLayout {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = size; typeface = tf }
        return StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).setLineSpacing(0f, 1.08f).build()
    }

    /** Largest text size (shrinking from maxSize) that fits inside maxH. */
    private fun fit(text: String, maxSize: Float, minSize: Float, step: Float, color: Int, tf: Typeface, width: Int, maxH: Float): StaticLayout {
        var s = maxSize
        while (s > minSize) {
            val l = layout(text, s, color, tf, width)
            if (l.height <= maxH) return l
            s -= step
        }
        return layout(text, minSize, color, tf, width, maxOf(1, (maxH / (minSize * 1.25f)).toInt()))
    }

    private fun drawCrop(c: Canvas, b: Bitmap, w: Int, h: Int) {
        val s = maxOf(w / b.width.toFloat(), h / b.height.toFloat())
        val dw = b.width * s
        val dh = b.height * s
        c.drawBitmap(b, null, RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun roundClip(c: Canvas, w: Int, h: Int, r: Float) {
        c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, Path.Direction.CW) })
    }

    fun render(ctx: Context, d: WidgetDesign, w: Int, h: Int, now: LocalDateTime = LocalDateTime.now(),
               dens: Float = ctx.resources.displayMetrics.density): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        fun dp(x: Float) = x * dens
        roundClip(c, w, h, dp(26f))

        // background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val b2 = d.bg2
        if (b2 != null) bgPaint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), d.bg, b2, Shader.TileMode.CLAMP)
        else bgPaint.color = d.bg
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        DesignStore.photo(ctx, d.photo)?.let { drawCrop(c, it, w, h); c.drawColor(Color.argb(80, 0, 0, 0)) }

        val wDp = w / dens
        val hDp = h / dens
        val roomy = wDp > 250f || hDp > 250f
        val big = hDp > 250f

        val pad = dp(14f)
        val cw = (w - 2 * pad).toInt()
        val tf = typeface(d.font, false)
        val tfb = typeface(d.font, true)
        val fg = d.fg
        val soft = ColorUtils.setAlphaComponent(fg, 175)
        val faint = ColorUtils.setAlphaComponent(fg, 140)

        var topY = pad
        if (d.symbol.isNotEmpty()) {
            val sp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; textSize = dp(16f) }
            c.drawText(d.symbol, pad, pad + dp(14f), sp)
            topY = pad + dp(28f)
        }
        val availH = h - pad - topY
        val stack = ArrayList<StaticLayout>()

        when (d.kind) {
            Kind.SHLOKA -> {
                val s = ShlokaStore.pick(ctx, now, d.rotationHours)
                if (s == null) {
                    stack.add(layout("Shlokas unavailable", dp(14f), fg, tf, cw))
                } else {
                    val lines = ShlokaStore.lines(s, d.language)
                    val base = if (big) 23f else if (roomy) 17f else 14f
                    val two = lines.size > 1 && roomy
                    stack.add(fit(lines[0], dp(base), dp(8f), dp(0.5f), fg, tfb, cw, availH * if (two) 0.5f else 0.78f))
                    if (two) stack.add(fit(lines[1], dp(base * 0.72f), dp(7f), dp(0.5f), soft, tf, cw, availH * 0.3f))
                    stack.add(layout(s.ref, dp(9f), faint, tf, cw, 1))
                }
            }
            Kind.TEXT -> {
                stack.add(fit(d.text, dp(if (roomy) 30f else 22f), dp(10f), dp(1f), fg, tfb, cw, availH * 0.75f))
                stack.add(layout(d.name, dp(10f), faint, tf, cw, 1))
            }
            Kind.COUNTDOWN -> {
                val days = d.targetEpochDay - now.toLocalDate().toEpochDay()
                stack.add(layout(maxOf(days, 0).toString(), dp(if (roomy) 70f else 52f), fg, tfb, cw, 1))
                stack.add(layout("days · ${d.text}", dp(11f), soft, tf, cw, 1))
            }
            Kind.DATE -> {
                val t = now.toLocalDate()
                stack.add(layout(t.dayOfMonth.toString(), dp(if (roomy) 70f else 52f), fg, tfb, cw, 1))
                stack.add(layout(
                    t.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ", " + t.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    dp(11f), soft, tf, cw, 1))
            }
        }

        val gap = dp(6f)
        val total = stack.sumOf { it.height.toDouble() }.toFloat() + gap * (stack.size - 1)
        var y = h - pad - total
        for (l in stack) {
            c.save(); c.translate(pad, y); l.draw(c); c.restore()
            y += l.height + gap
        }
        return bmp
    }

    // ───────── Calendar widget (black, bold) ─────────

    fun renderCalendar(ctx: Context, w: Int, h: Int, today: LocalDate = LocalDate.now(),
                       dens: Float = ctx.resources.displayMetrics.density): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        fun dp(x: Float) = x * dens
        roundClip(c, w, h, dp(26f))
        c.drawColor(Color.BLACK)
        val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        fun paint(size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = medium; textAlign = align }

        val pad = dp(14f)
        val wide = w > h * 1.4f
        val loc = Locale.getDefault()
        val weekday = today.dayOfWeek.getDisplayName(TextStyle.SHORT, loc).uppercase()
        val month = today.month.getDisplayName(TextStyle.FULL, loc)

        if (!wide) {
            c.drawText(weekday, pad, pad + dp(22f), paint(dp(22f), Color.rgb(255, 69, 58)))
            c.drawText(today.dayOfMonth.toString(), pad, pad + dp(22f) + dp(66f), paint(dp(64f), Color.WHITE))
            c.drawText(month, pad, h - pad, paint(dp(13f), Color.argb(150, 255, 255, 255)))
        } else {
            val lx = w * 0.25f
            c.drawText(weekday, lx, h * 0.42f, paint(dp(36f), Color.WHITE, Paint.Align.CENTER))
            c.drawText(today.dayOfMonth.toString(), lx, h * 0.80f, paint(dp(64f), Color.WHITE, Paint.Align.CENTER))
            val gx = w * 0.5f
            val right = w - pad
            c.drawText(month.uppercase(), right, pad + dp(12f), paint(dp(13f), Color.WHITE, Paint.Align.RIGHT))
            val gTop = pad + dp(22f)
            val cellW = (right - gx) / 7f
            val cellH = (h - pad - gTop) / 6f
            val first = today.withDayOfMonth(1)
            val fdow = WeekFields.of(loc).firstDayOfWeek
            val offset = (first.dayOfWeek.value - fdow.value + 7) % 7
            val numSize = minOf(dp(11f), cellH * 0.62f)
            val num = paint(numSize, Color.WHITE, Paint.Align.CENTER)
            val numDark = paint(numSize, Color.BLACK, Paint.Align.CENTER)
            val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            for (i in 0 until today.lengthOfMonth()) {
                val idx = offset + i
                val cx = gx + (idx % 7) * cellW + cellW / 2
                val cy = gTop + (idx / 7) * cellH + cellH / 2
                val isToday = i + 1 == today.dayOfMonth
                if (isToday) c.drawRoundRect(RectF(cx - cellW * 0.45f, cy - cellH * 0.42f, cx + cellW * 0.45f, cy + cellH * 0.42f), dp(4f), dp(4f), hi)
                c.drawText((i + 1).toString(), cx, cy + numSize / 3f, if (isToday) numDark else num)
            }
        }
        return bmp
    }
}
