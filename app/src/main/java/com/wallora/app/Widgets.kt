package com.wallora.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

private class Sz(val w: Int, val h: Int, val dens: Float)

private fun sizeOf(ctx: Context, mgr: AppWidgetManager, id: Int): Sz {
    val o = mgr.getAppWidgetOptions(id)
    val dens = ctx.resources.displayMetrics.density
    val wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 158).coerceAtLeast(60)
    val hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 158).coerceAtLeast(60)
    var w = wDp * dens
    var h = hDp * dens
    var d = dens
    val cap = 1000f   // keep the bitmap small: RemoteViews has a memory limit
    val m = maxOf(w, h)
    if (m > cap) { val s = cap / m; w *= s; h *= s; d *= s }
    return Sz(w.toInt(), h.toInt(), d)
}

private fun push(ctx: Context, mgr: AppWidgetManager, id: Int, bmp: android.graphics.Bitmap) {
    val rv = RemoteViews(ctx.packageName, R.layout.widget_image)
    rv.setImageViewBitmap(R.id.img, bmp)
    rv.setOnClickPendingIntent(
        R.id.img,
        PendingIntent.getActivity(ctx, id, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    )
    mgr.updateAppWidget(id, rv)
}

class CustomWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) { ids.forEach { update(context, mgr, it) } }
    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) { update(context, mgr, id) }
    override fun onDeleted(context: Context, ids: IntArray) { Prefs.init(context); ids.forEach { Prefs.removeWidget(it) } }

    companion object {
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            Prefs.init(ctx)
            val s = sizeOf(ctx, mgr, id)
            val d = Prefs.widgetDesign(id) ?: WidgetDesign()
            push(ctx, mgr, id, WidgetRenderer.render(ctx, d, s.w, s.h, dens = s.dens))
        }
    }
}

class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) { ids.forEach { update(context, mgr, it) } }
    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) { update(context, mgr, id) }

    companion object {
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val s = sizeOf(ctx, mgr, id)
            push(ctx, mgr, id, WidgetRenderer.renderCalendar(ctx, s.w, s.h, dens = s.dens))
        }
    }
}

/** Opens when you add a "Ramayana & Custom" widget: design it, tap Add, done. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setResult(RESULT_CANCELED)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        enableEdgeToEdge()
        val initial = Prefs.widgetDesign(id) ?: DesignStore.load(this).firstOrNull() ?: WidgetDesign()
        setContent {
            WalloraMaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    StudioScreen(
                        initial = initial,
                        confirmLabel = "Add widget",
                        onConfirm = { d ->
                            Prefs.setWidgetDesign(id, d)
                            CustomWidgetProvider.update(this@WidgetConfigActivity, AppWidgetManager.getInstance(this@WidgetConfigActivity), id)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}
