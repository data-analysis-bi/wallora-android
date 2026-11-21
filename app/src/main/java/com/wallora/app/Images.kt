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
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "Wallora/1.0")
            c.connectTimeout = 15000
            c.readTimeout = 30000
            f.writeBytes(c.inputStream.use { it.readBytes() })
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


const val CATALOG_URL = "https://data-analysis-bi.github.io/wallora-catalog/index.json"

private fun JSONObject.strOrNull(k: String): String? =
    if (!has(k) || isNull(k)) null else getString(k).ifEmpty { null }

/** The online catalog: static JSON on GitHub Pages (public-domain / CC0 museum + NASA images). */
object Online {
    private fun cacheFile(ctx: Context, url: String): File {
        val name = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(File(ctx.filesDir, "net").apply { mkdirs() }, "$name.json")
    }

    /** Network first; if offline, the last copy saved on the device. */
    fun fetchJson(ctx: Context, url: String): JSONObject? {
        val f = cacheFile(ctx, url)
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "Wallora/1.0")
            c.connectTimeout = 15000
            c.readTimeout = 20000
            if (c.responseCode == 200) {
                val text = c.inputStream.bufferedReader().use { it.readText() }
                val o = JSONObject(text)
                f.writeText(text)
                return o
            }
        } catch (_: Exception) {
        }
        return runCatching { JSONObject(f.readText()) }.getOrNull()
    }

    private fun parseItem(o: JSONObject): Wallpaper? {
        val cat = WallCategory.values().firstOrNull { it.name.equals(o.getString("category"), ignoreCase = true) } ?: return null
        val url = o.getString("url")
        return Wallpaper(
            id = o.getString("id"), title = o.getString("title"), category = cat,
            source = Source.Remote(o.getString("thumb"), url, o.strOrNull("hires") ?: url),
            attribution = o.strOrNull("credit"), sourceUrl = o.strOrNull("sourceUrl")
        )
    }

    fun parseItems(arr: JSONArray): List<Wallpaper> =
        (0 until arr.length()).mapNotNull { runCatching { parseItem(arr.getJSONObject(it)) }.getOrNull() }

    fun toJson(list: List<Wallpaper>): JSONArray = JSONArray().also { arr ->
        list.forEach { w ->
            val s = w.source as? Source.Remote ?: return@forEach
            arr.put(JSONObject().apply {
                put("id", w.id); put("title", w.title); put("category", w.category.name.lowercase())
                put("thumb", s.thumb); put("url", s.regular); put("hires", s.full)
                put("credit", w.attribution ?: ""); put("sourceUrl", w.sourceUrl ?: "")
            })
        }
    }

    fun fromJson(arr: JSONArray): List<Wallpaper> = parseItems(arr)
}

object CatalogState {
    var all by mutableStateOf(Catalog.bundled)
        private set
    var feed by mutableStateOf<List<Wallpaper>>(emptyList())
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var hasMoreFeed by mutableStateOf(false)
        private set

    private var remote: List<Wallpaper> = emptyList()
    private var base = ""
    private var generated = ""
    private var feedPages = 0
    private var feedLoaded = 0
    private var catPages = mapOf<String, Int>()
    private var catCounts = mapOf<String, Int>()
    private val catLoaded = HashMap<String, Int>()
    private var busy = false

    /** Loads the catalog index and the first page. Offline: uses the last saved copy. */
    suspend fun load(ctx: Context, force: Boolean = false) {
        if (loading) return
        loading = true
        try {
            val idx = withContext(Dispatchers.IO) { Online.fetchJson(ctx, CATALOG_URL) }
            if (idx == null) { status = "Offline — showing built-in wallpapers"; return }
            val gen = idx.optString("generated")
            if (force || gen != generated) { remote = emptyList(); feed = emptyList(); feedLoaded = 0; catLoaded.clear(); all = Catalog.bundled }
            generated = gen
            base = idx.getString("base")
            feedPages = idx.getJSONObject("all").getInt("pages")
            val cats = idx.getJSONArray("categories")
            val pages = HashMap<String, Int>()
            val counts = HashMap<String, Int>()
            for (i in 0 until cats.length()) {
                val c = cats.getJSONObject(i)
                pages[c.getString("id")] = c.getInt("pages")
                counts[c.getString("id")] = c.getInt("count")
            }
            catPages = pages
            catCounts = counts
            hasMoreFeed = feedLoaded < feedPages
            if (feedLoaded == 0) loadFeedPage(ctx)
            status = "${idx.getInt("total")} online wallpapers · updated $gen"
        } finally {
            loading = false
        }
    }

    private suspend fun loadFeedPage(ctx: Context) {
        val page = withContext(Dispatchers.IO) { Online.fetchJson(ctx, "${base}all/${feedLoaded + 1}.json") } ?: return
        feedLoaded++
        val items = Online.parseItems(page.getJSONArray("items"))
        val have = feed.map { it.id }.toHashSet()
        feed = feed + items.filter { have.add(it.id) }
        merge(items)
        hasMoreFeed = feedLoaded < feedPages
    }

    suspend fun loadMore(ctx: Context) {
        if (busy || feedLoaded >= feedPages) return
        busy = true
        try { loadFeedPage(ctx) } finally { busy = false }
    }

    fun hasMore(c: WallCategory): Boolean {
        val id = c.name.lowercase()
        return (catLoaded[id] ?: 0) < (catPages[id] ?: 0)
    }

    suspend fun loadMore(ctx: Context, c: WallCategory) {
        if (busy || !hasMore(c)) return
        busy = true
        try {
            val id = c.name.lowercase()
            val n = (catLoaded[id] ?: 0) + 1
            val page = withContext(Dispatchers.IO) { Online.fetchJson(ctx, "${base}c/$id/$n.json") } ?: return
            catLoaded[id] = n
            merge(Online.parseItems(page.getJSONArray("items")))
        } finally { busy = false }
    }

    private fun merge(items: List<Wallpaper>) {
        val seen = remote.map { it.id }.toHashSet()
        remote = remote + items.filter { seen.add(it.id) }
        all = Catalog.bundled + remote
    }

    /** Discover feed: a few built-in wallpapers first, then the online feed (loads more as you scroll). */
    fun discoverFeed(): List<Wallpaper> = Catalog.bundled.take(10) + feed
    fun count(c: WallCategory): Int = maxOf(inCategory(c).size, catCounts[c.name.lowercase()] ?: 0)

    fun daily(): Wallpaper = all[java.time.LocalDate.now().dayOfYear % all.size]
    fun inCategory(c: WallCategory) = all.filter { it.category == c }
    fun search(q: String): List<Wallpaper> {
        val t = q.lowercase()
        return all.filter { it.title.lowercase().contains(t) || it.category.title.lowercase().contains(t) }
    }
}
