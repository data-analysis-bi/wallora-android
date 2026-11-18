package com.wallora.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

object Images {
    private val mem = object : LruCache<String, Bitmap>(96 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun load(ctx: Context, w: Wallpaper, full: Boolean): Bitmap? = withContext(Dispatchers.Default) {
        val key = "${w.id}-$full-${Prefs.quality}"
        mem.get(key)?.let { return@withContext it }
        val bmp: Bitmap? = runCatching {
            when (val s = w.source) {
                is Source.Bundled -> decodeAsset(ctx, s.asset, if (full) 1 else 4)
                is Source.Gradient -> Art.render(s, if (full) 1290 else 330, if (full) 2796 else 715)
                is Source.Procedural -> Art.render(s, if (full) 1290 else 330, if (full) 2796 else 715)
                is Source.Remote -> remote(ctx, if (full) (if (Prefs.quality == "ultra") s.full else s.regular) else s.thumb)
            }
        }.getOrNull()
        if (bmp != null) mem.put(key, bmp)
        bmp
    }

    private fun decodeAsset(ctx: Context, name: String, sample: Int): Bitmap? =
        ctx.assets.open(name).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }

    private fun cacheDir(ctx: Context) = File(ctx.cacheDir, "wallpapers").apply { mkdirs() }

    private fun remote(ctx: Context, url: String): Bitmap? {
        val name = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) } + ".jpg"
        val f = File(cacheDir(ctx), name)
        if (!f.exists()) {
            val bytes = URL(url).openStream().use { it.readBytes() }
            f.writeBytes(bytes)
        }
        return BitmapFactory.decodeFile(f.path)
    }

    fun clearCache(ctx: Context) {
        cacheDir(ctx).deleteRecursively()
        mem.evictAll()
    }
}

object MediaSaver {
    /** Saves to Pictures/Wallora using MediaStore (no storage permission needed on Android 10+). */
    fun save(ctx: Context, bmp: Bitmap, title: String): Uri? {
        val safe = title.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifEmpty { "wallpaper" }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$safe-${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Wallora")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        return uri
    }
}

private class UnsplashError(message: String) : Exception(message)

private fun JSONObject.strOrNull(k: String): String? = if (isNull(k)) null else getString(k)

object Unsplash {
    private fun get(url: String, key: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("Authorization", "Client-ID $key")
        c.connectTimeout = 15000
        c.readTimeout = 20000
        val code = c.responseCode
        if (code != 200) throw UnsplashError(
            when (code) {
                401 -> "Unsplash rejected the key (401). Check you pasted the Access Key."
                403 -> "Unsplash rate limit reached (demo keys: 50 requests/hour). Try again later."
                else -> "Unsplash error $code."
            }
        )
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    fun search(key: String, query: String, cat: WallCategory): List<Wallpaper> {
        val url = "https://api.unsplash.com/search/photos?query=${URLEncoder.encode(query, "UTF-8")}&orientation=portrait&per_page=20"
        val results = JSONObject(get(url, key)).getJSONArray("results")
        return List(results.length()) {
            val o = results.getJSONObject(it)
            val urls = o.getJSONObject("urls")
            val title = (o.strOrNull("description") ?: o.strOrNull("alt_description") ?: "Photo")
                .replaceFirstChar { ch -> ch.uppercase() }.take(60)
            Wallpaper(
                id = "u-" + o.getString("id"), title = title, category = cat,
                source = Source.Remote(urls.getString("small"), urls.getString("regular"), urls.getString("full")),
                attribution = "Photo by " + o.getJSONObject("user").getString("name") + " on Unsplash",
                downloadLocation = o.getJSONObject("links").getString("download_location")
            )
        }
    }

    /** Unsplash's terms ask apps to ping this when a photo is downloaded. */
    fun track(key: String, url: String) { runCatching { get(url, key) } }

    fun toJson(list: List<Wallpaper>): JSONArray = JSONArray().also { arr ->
        list.forEach { w ->
            val s = w.source as? Source.Remote ?: return@forEach
            arr.put(JSONObject().apply {
                put("id", w.id); put("title", w.title); put("cat", w.category.name)
                put("thumb", s.thumb); put("regular", s.regular); put("full", s.full)
                put("attr", w.attribution); put("dl", w.downloadLocation)
            })
        }
    }

    fun fromJson(arr: JSONArray): List<Wallpaper> = List(arr.length()) {
        val o = arr.getJSONObject(it)
        Wallpaper(o.getString("id"), o.getString("title"),
            WallCategory.values().firstOrNull { c -> c.name == o.getString("cat") } ?: WallCategory.NATURE,
            Source.Remote(o.getString("thumb"), o.getString("regular"), o.getString("full")),
            o.strOrNull("attr"), o.strOrNull("dl"))
    }
}

object CatalogState {
    var all by mutableStateOf(Catalog.bundled)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    private var remote: List<Wallpaper> = emptyList()
    val hasRemote: Boolean get() = remote.isNotEmpty()

    private val queries = listOf(
        WallCategory.NATURE to "nature landscape", WallCategory.MOUNTAINS to "mountains",
        WallCategory.SPACE to "galaxy night sky", WallCategory.ABSTRACT to "abstract art",
        WallCategory.ARCHITECTURE to "modern architecture", WallCategory.CARS to "supercar",
        WallCategory.ANIME to "illustration art", WallCategory.TECHNOLOGY to "technology dark",
        WallCategory.DARK to "dark moody", WallCategory.MINIMALIST to "minimal",
        WallCategory.AMOLED to "black minimal", WallCategory.GRADIENTS to "gradient",
        WallCategory.UHD to "landscape", WallCategory.SEASONAL to "autumn winter"
    )

    private fun apply() { all = Catalog.bundled + remote }

    /** Cached for 12 h so we stay well under Unsplash's rate limit. */
    suspend fun load(ctx: Context, force: Boolean = false) {
        val key = Prefs.unsplashKey
        if (key.isBlank()) { remote = emptyList(); apply(); status = null; return }
        withContext(Dispatchers.IO) {
            val f = File(ctx.filesDir, "remote-catalog.json")
            var fresh = false
            if (f.exists()) {
                runCatching {
                    val o = JSONObject(f.readText())
                    val items = Unsplash.fromJson(o.getJSONArray("items"))
                    if (items.isNotEmpty()) {
                        remote = items; apply()
                        fresh = System.currentTimeMillis() - o.getLong("date") < 12 * 3600_000L
                        if (fresh) status = "${items.size} photos (cached)"
                    }
                }
            }
            if (fresh && !force) return@withContext
            loading = true
            val out = ArrayList<Wallpaper>()
            val seen = HashSet<String>()
            var lastError: String? = null
            for ((cat, q) in queries) {
                try {
                    Unsplash.search(key, q, cat).forEach { if (seen.add(it.id)) out.add(it) }
                } catch (e: Exception) {
                    lastError = e.message
                    if (e.message?.startsWith("Unsplash") == true) break
                }
            }
            loading = false
            if (out.isEmpty()) { status = lastError ?: "No photos returned."; return@withContext }
            remote = out; apply()
            status = "${out.size} photos loaded" + (lastError?.let { " — $it" } ?: "")
            f.writeText(JSONObject().put("date", System.currentTimeMillis()).put("items", Unsplash.toJson(out)).toString())
        }
    }

    fun daily(): Wallpaper {
        val d = java.time.LocalDate.now().dayOfYear
        return all[d % all.size]
    }
    fun featured(): List<Wallpaper> = WallCategory.values().flatMap { c -> all.filter { it.category == c }.take(2) }
    fun inCategory(c: WallCategory) = all.filter { it.category == c }
    fun search(q: String): List<Wallpaper> {
        val t = q.lowercase()
        return all.filter { it.title.lowercase().contains(t) || it.category.title.lowercase().contains(t) }
    }
}
