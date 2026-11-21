package com.wallora.app

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ViewerScreen(w: Wallpaper, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var mode by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var tint by remember { mutableStateOf(Color.White) }

    LaunchedEffect(w.id) {
        val b = Images.load(ctx, w, true)
        bmp = b
        if (b != null) {
            val px = withContext(Dispatchers.Default) { Bitmap.createScaledBitmap(b, 1, 1, true).getPixel(0, 0) }
            tint = lerp(Color(px), Color.White, 0.6f)
        }
    }

    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

    fun setWallpaper(which: Int, label: String) {
        val b = bmp ?: return
        scope.launch {
            busy = true
            val r = withContext(Dispatchers.IO) { runCatching { WallpaperManager.getInstance(ctx).setBitmap(b, null, true, which) } }
            busy = false
            if (r.isSuccess) {
                toast("Wallpaper set: $label")
                Prefs.addDownload(w.title)
            } else toast("Couldn't set wallpaper: ${r.exceptionOrNull()?.message}")
        }
    }

    fun save(share: Boolean) {
        val b = bmp ?: return
        scope.launch {
            busy = true
            val uri = withContext(Dispatchers.IO) { runCatching { MediaSaver.save(ctx, b, w.title) }.getOrNull() }
            busy = false
            if (uri == null) { toast("Couldn't save the image"); return@launch }
            Prefs.addDownload(w.title)
            if (share) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(send, "Share wallpaper"))
            } else toast("Saved to Pictures/Wallora")
        }
    }

    val fav = w.id in Prefs.favorites
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), w.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)

        when (mode) {
            1 -> LockOverlay()
            2 -> HomeOverlay()
        }

        Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))) {
                Icon(Icons.Filled.Close, "Close", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            listOf("Clean", "Lock", "Home").forEachIndexed { i, t ->
                FilterChip(selected = mode == i, onClick = { mode = i }, label = { Text(t) })
                Spacer(Modifier.width(6.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { Prefs.toggleFavorite(w) }, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))) {
                Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorite", tint = if (fav) Color(0xFFFF4D8D) else Color.White)
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            w.attribution?.let {
                Text(it, color = Color.White, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = w.sourceUrl != null) {
                        w.sourceUrl?.let { u -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
                    }
                    .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            val enabled = bmp != null && !busy
            val colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Color.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { setWallpaper(WallpaperManager.FLAG_SYSTEM, "Home screen") }, enabled = enabled, colors = colors, modifier = Modifier.weight(1f)) { Text("Home", maxLines = 1) }
                Button(onClick = { setWallpaper(WallpaperManager.FLAG_LOCK, "Lock screen") }, enabled = enabled, colors = colors, modifier = Modifier.weight(1f)) { Text("Lock", maxLines = 1) }
                Button(onClick = { setWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "Home + Lock") }, enabled = enabled, colors = colors, modifier = Modifier.weight(1f)) { Text("Both", maxLines = 1) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { save(false) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Save to Photos", color = Color.White, maxLines = 1) }
                OutlinedButton(onClick = { save(true) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Share", color = Color.White) }
            }
        }
    }
}

@Composable
private fun LockOverlay() {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm")), color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Thin)
    }
}

@Composable
private fun HomeOverlay() {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 110.dp, start = 24.dp, end = 24.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
        repeat(4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(4) { Box(Modifier.size(58.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.28f))) }
            }
        }
    }
}
