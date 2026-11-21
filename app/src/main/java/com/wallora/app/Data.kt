package com.wallora.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// ───────────── Wallpapers ─────────────

enum class WallCategory(val title: String, val c1: Int, val c2: Int) {
    MINIMALIST("Minimalist", 0xFFD9D4CA.toInt(), 0xFFF4F1EC.toInt()),
    AMOLED("AMOLED", 0xFF000000.toInt(), 0xFF222222.toInt()),
    NATURE("Nature", 0xFF2F5D50.toInt(), 0xFF9BC5A8.toInt()),
    MOUNTAINS("Mountains", 0xFF2D5B6B.toInt(), 0xFFEBA88A.toInt()),
    SPACE("Space", 0xFF0B0016.toInt(), 0xFF6A3FA0.toInt()),
    ABSTRACT("Abstract", 0xFF1A1033.toInt(), 0xFF9B6DE0.toInt()),
    ARCHITECTURE("Architecture", 0xFF40444B.toInt(), 0xFFA9AFB8.toInt()),
    CARS("Cars", 0xFF3A0D0D.toInt(), 0xFFC0392B.toInt()),
    ANIME("Anime", 0xFFFF9AA2.toInt(), 0xFFB5A7FF.toInt()),
    TECHNOLOGY("Technology", 0xFF0A1F33.toInt(), 0xFF2E86DE.toInt()),
    DARK("Dark Aesthetic", 0xFF0B0B1A.toInt(), 0xFF2A2A52.toInt()),
    GRADIENTS("Gradients", 0xFFFFB88C.toInt(), 0xFFDE6262.toInt()),
    UHD("4K & Ultra HD", 0xFF11998E.toInt(), 0xFF38EF7D.toInt()),
    SEASONAL("Seasonal", 0xFFA1C4FD.toInt(), 0xFFC2E9FB.toInt()),
    ART("Art", 0xFF8E5A3C.toInt(), 0xFFE6C79C.toInt())
}

sealed interface Source {
    data class Bundled(val asset: String) : Source
    data class Gradient(val hex: List<String>) : Source
    data class Procedural(val style: String, val seed: Int, val palette: List<String>) : Source
    data class Remote(val thumb: String, val regular: String, val full: String) : Source
}

data class Wallpaper(
    val id: String,
    val title: String,
    val category: WallCategory,
    val source: Source,
    val attribution: String? = null,
    val sourceUrl: String? = null
)

object Catalog {
    private fun g(id: String, title: String, cat: WallCategory, vararg hex: String) =
        Wallpaper("g-$id", title, cat, Source.Gradient(hex.toList()))
    private fun p(n: String, title: String, cat: WallCategory, style: String, seed: Int, vararg pal: String) =
        Wallpaper("p-$n", title, cat, Source.Procedural(style, seed, pal.toList()))

    val bundled: List<Wallpaper> = listOf(
        Wallpaper("b-dusk", "Dusk Ridges", WallCategory.MOUNTAINS, Source.Bundled("dusk-ridges.jpg")),
        Wallpaper("b-orbs", "Violet Orbs", WallCategory.ABSTRACT, Source.Bundled("violet-orbs.jpg")),

        p("golden", "Golden Hour", WallCategory.MOUNTAINS, "ridges", 11, "FBD3A1", "F29E75", "8DA9A6", "5C8A92", "3A6877", "22465A", "0F2A3A"),
        p("mist", "Misty Pines", WallCategory.MOUNTAINS, "ridges", 23, "CFE0DC", "A9C5C0", "8FB0AD", "5F8B8C", "3B6468", "1E3F45"),
        p("rose", "Rose Peaks", WallCategory.MOUNTAINS, "ridges", 37, "F6D5D0", "E6A3B1", "B58BA8", "865F8E", "5A3E73", "2E1F4D"),
        p("night", "Night Range", WallCategory.MOUNTAINS, "ridges", 41, "0B1026", "2A3A6B", "2F4A7A", "203A66", "142850", "0A1630"),
        p("desert", "Desert Dusk", WallCategory.MOUNTAINS, "ridges", 53, "FFD9A8", "F08A5D", "C46A55", "8E4A4F", "5A334A", "2F1F3D"),
        p("emerald", "Emerald Valley", WallCategory.NATURE, "ridges", 67, "D6F0E0", "9CD3B8", "6DB39A", "3F8C7C", "23665F", "0F3E3F"),
        p("forest", "Forest Haze", WallCategory.NATURE, "ridges", 71, "E4EFE0", "B7D3B0", "8FBA8C", "5E9A6B", "3A7050", "1F4535"),

        p("autumn", "Autumn Hills", WallCategory.SEASONAL, "ridges", 83, "FFE2B8", "F2A65A", "D9823B", "B25F2E", "7F3F25", "4A2A1E"),
        p("winter", "Winter Frost", WallCategory.SEASONAL, "ridges", 97, "E8F1FA", "C6DCEF", "A9C4DD", "7FA3C6", "567FA8", "34567F"),
        p("spring", "Spring Bloom", WallCategory.SEASONAL, "ridges", 101, "FDE5EC", "F9C0D0", "EBA3BC", "C98AB0", "8F6A9E", "5B4B85"),
        p("summer", "Summer Sunset", WallCategory.SEASONAL, "waves", 5, "FFB347", "FF5E62", "FF8F6B", "E8506A", "A83279"),

        p("cosmic", "Cosmic Dust", WallCategory.SPACE, "stars", 3, "05000C", "1A0B3A", "6A3FA0"),
        p("deepfield", "Deep Field", WallCategory.SPACE, "stars", 8, "000000", "050A24", "1F3A93"),
        p("crimson", "Crimson Night", WallCategory.SPACE, "stars", 13, "0A0003", "2A0A14", "C0392B"),
        p("tealneb", "Teal Nebula", WallCategory.SPACE, "stars", 19, "00060A", "06303A", "2EC4B6"),

        p("violet-ecl", "Violet Eclipse", WallCategory.ABSTRACT, "orbs", 0, "050507", "6A3FA0", "B28DFF"),
        p("amber-ecl", "Amber Eclipse", WallCategory.ABSTRACT, "orbs", 0, "060403", "C0702A", "FFC27A"),
        p("azure-ecl", "Azure Eclipse", WallCategory.ABSTRACT, "orbs", 0, "03060A", "1F6FEB", "8CC8FF"),
        p("rose-ecl", "Rose Eclipse", WallCategory.ABSTRACT, "orbs", 0, "070305", "C2185B", "FF8AB5"),
        p("ocean", "Ocean Drift", WallCategory.ABSTRACT, "waves", 29, "0B1D3A", "184E77", "1E6091", "168AAD", "76C893"),
        p("silk", "Sunset Silk", WallCategory.ABSTRACT, "waves", 31, "2B0F3A", "5B1A57", "A8326B", "E0577A", "FF9A76"),
        p("mint", "Mint Silk", WallCategory.ABSTRACT, "waves", 43, "0F2B2B", "164E4E", "2A7F7A", "3FB3A0", "9FE8C9"),

        p("neon", "Neon Grid", WallCategory.TECHNOLOGY, "grid", 0, "02040D", "0A1A3A", "2E86DE"),
        p("matrix", "Matrix Green", WallCategory.TECHNOLOGY, "grid", 0, "000A05", "001F12", "2EE59D"),
        p("magenta", "Magenta Grid", WallCategory.TECHNOLOGY, "grid", 0, "08020D", "1E0A33", "E040FB"),
        g("circuit", "Circuit", WallCategory.TECHNOLOGY, "02111F", "0B3D6B", "2E86DE"),

        p("halo", "Halo", WallCategory.MINIMALIST, "rings", 0, "F4F1EC", "E3DED3", "8A8577"),
        p("ripple-dark", "Ripple Dark", WallCategory.MINIMALIST, "rings", 0, "14161A", "1F2329", "8C96A3"),
        p("ripple-blue", "Ripple Blue", WallCategory.MINIMALIST, "rings", 0, "EAF2F8", "CFE0EE", "5B87B3"),
        g("paper", "Paper", WallCategory.MINIMALIST, "F4F1EC", "D9D4CA"),
        g("slate", "Slate", WallCategory.MINIMALIST, "3A3F47", "1A1D22"),

        g("void", "Void", WallCategory.AMOLED, "000000", "000000"),
        g("ember", "Ember Edge", WallCategory.AMOLED, "000000", "000000", "2A0E0E"),
        p("blackhalo", "Black Halo", WallCategory.AMOLED, "rings", 0, "000000", "000000", "6A6A6A"),
        p("violet-edge", "Violet Edge", WallCategory.AMOLED, "orbs", 0, "000000", "3A1C71", "9B6DE0"),

        g("midnight", "Midnight", WallCategory.DARK, "0B0B1A", "1B1B3A"),
        g("graphite", "Graphite", WallCategory.DARK, "0D0D0D", "2B2B2B"),
        p("dunes", "Dark Dunes", WallCategory.DARK, "waves", 47, "0B0B12", "15151F", "1D1D2B", "262638", "31314A"),
        p("ink", "Ink Ridges", WallCategory.DARK, "ridges", 59, "1B1B2A", "0F0F1A", "1A1A2C", "141424", "0E0E1C", "08080F"),

        g("aurora", "Aurora", WallCategory.GRADIENTS, "0F2027", "2C5364", "6DD5ED"),
        g("peach", "Peach Fizz", WallCategory.GRADIENTS, "FFB88C", "DE6262"),
        g("lagoon", "Lagoon", WallCategory.GRADIENTS, "0F3443", "34E89E"),
        g("violetnight", "Violet Night", WallCategory.GRADIENTS, "1A1033", "6A3FA0", "D76D77"),
        g("frost", "Frost", WallCategory.SEASONAL, "A1C4FD", "C2E9FB"),
        g("nebula", "Nebula", WallCategory.GRADIENTS, "05000C", "3A1C71", "D76D77"),
        g("deepspace", "Deep Space", WallCategory.GRADIENTS, "000000", "0B1026", "1F3A93")
    )
}

// ───────────── Preferences ─────────────

object Prefs {
    private lateinit var sp: SharedPreferences

    var favorites by mutableStateOf(setOf<String>())
        private set
    var quality by mutableStateOf("high")
        private set
    var favoriteItems by mutableStateOf(listOf<Wallpaper>())
        private set
    var downloads by mutableStateOf(listOf<String>())
        private set

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("wallora", Context.MODE_PRIVATE)
        favorites = (sp.getStringSet("fav", emptySet()) ?: emptySet()).toSet()
        quality = sp.getString("quality", "high") ?: "high"
        favoriteItems = runCatching { Online.fromJson(JSONArray(sp.getString("fav_items", "[]"))) }.getOrDefault(emptyList())
        downloads = (sp.getString("downloads", "") ?: "").split("\n").filter { it.isNotBlank() }
    }

    fun toggleFavorite(w: Wallpaper) {
        val adding = w.id !in favorites
        favorites = if (adding) favorites + w.id else favorites - w.id
        // keep a saved copy of online wallpapers so favorites survive restarts and work offline
        favoriteItems = if (adding && w.source is Source.Remote) favoriteItems + w else favoriteItems.filter { it.id != w.id }
        sp.edit().putStringSet("fav", favorites).putString("fav_items", Online.toJson(favoriteItems).toString()).apply()
    }
    fun updateQuality(q: String) { quality = q; sp.edit().putString("quality", q).apply() }
    fun addDownload(title: String) {
        downloads = (listOf("$title · ${LocalDate.now()}") + downloads).take(50)
        sp.edit().putString("downloads", downloads.joinToString("\n")).apply()
    }

    // Each widget on the Home Screen keeps its own copy of the design chosen in the setup screen.
    fun widgetDesign(widgetId: Int): WidgetDesign? =
        sp.getString("wd_$widgetId", null)?.let { runCatching { WidgetDesign.fromJson(JSONObject(it)) }.getOrNull() }
    fun setWidgetDesign(widgetId: Int, d: WidgetDesign) { sp.edit().putString("wd_$widgetId", d.toJson().toString()).apply() }
    fun removeWidget(widgetId: Int) { sp.edit().remove("wd_$widgetId").apply() }
}

// ───────────── Ramayana ─────────────

enum class ShlokaLang(val title: String) {
    SANSKRIT_EN("Sanskrit + English"),
    SANSKRIT("Sanskrit (देवनागरी)"),
    SANSKRIT_GU("Sanskrit (ગુજરાતી લિપિ)"),
    ENGLISH("English"),
    HINDI("हिन्दी"),
    GUJARATI("ગુજરાતી")
}

data class Shloka(val id: String, val ref: String, val sa: String, val en: String, val hi: String, val gu: String)

object Script {
    /** Devanagari to Gujarati script: the two Unicode blocks are laid out in parallel, 0x180 apart. */
    fun gujarati(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            i += Character.charCount(cp)
            if (cp in 0x0901..0x0963 || cp in 0x0966..0x096F) {
                val n = cp + 0x180
                if (Character.getType(n) != Character.UNASSIGNED.toInt()) sb.appendCodePoint(n) else sb.appendCodePoint(cp)
            } else sb.appendCodePoint(cp)
        }
        return sb.toString()
    }
}

object ShlokaStore {
    private var cache: List<Shloka>? = null

    fun all(ctx: Context): List<Shloka> {
        cache?.let { return it }
        val txt = ctx.assets.open("ramayana_curated.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(txt)
        val list = List(arr.length()) {
            val o = arr.getJSONObject(it)
            Shloka(o.getString("id"), o.getString("ref"), o.getString("sa"), o.getString("en"), o.getString("hi"), o.getString("gu"))
        }
        cache = list
        return list
    }

    /** Consecutive slots step through the list: a new verse every day (or every N hours). */
    fun pick(ctx: Context, now: LocalDateTime, rotationHours: Int): Shloka? {
        val list = all(ctx)
        if (list.isEmpty()) return null
        val h = rotationHours.coerceIn(1, 24)
        val index = now.toLocalDate().toEpochDay() * (24 / h) + now.hour / h
        val n = list.size.toLong()
        return list[(((index % n) + n) % n).toInt()]
    }

    fun lines(s: Shloka, l: ShlokaLang): List<String> = when (l) {
        ShlokaLang.SANSKRIT -> listOf(s.sa)
        ShlokaLang.SANSKRIT_GU -> listOf(Script.gujarati(s.sa))
        ShlokaLang.ENGLISH -> listOf(s.en)
        ShlokaLang.HINDI -> listOf(s.hi)
        ShlokaLang.GUJARATI -> listOf(s.gu)
        ShlokaLang.SANSKRIT_EN -> listOf(s.sa, s.en)
    }
}

data class VerseRow(val k: Int, val s: Int, val n: Int, val t: String)

object RamayanaText {
    private var cache: List<VerseRow>? = null
    fun load(ctx: Context): List<VerseRow> {
        cache?.let { return it }
        val txt = ctx.assets.open("ramayana_full.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(txt)
        val list = List(arr.length()) {
            val o = arr.getJSONObject(it)
            VerseRow(o.getInt("k"), o.getInt("s"), o.getInt("n"), o.getString("t"))
        }
        cache = list
        return list
    }
    val kandas = listOf(
        "Bala Kanda" to "बालकाण्ड", "Ayodhya Kanda" to "अयोध्याकाण्ड", "Aranya Kanda" to "अरण्यकाण्ड",
        "Kishkindha Kanda" to "किष्किन्धाकाण्ड", "Sundara Kanda" to "सुन्दरकाण्ड",
        "Yuddha Kanda" to "युद्धकाण्ड", "Uttara Kanda" to "उत्तरकाण्ड"
    )
}

// ───────────── Widget designs ─────────────

enum class Kind(val title: String) { SHLOKA("Ramayana shloka"), TEXT("Custom text"), COUNTDOWN("Countdown"), DATE("Date") }
enum class FontStyle(val title: String) { SERIF("Serif"), ROUNDED("Rounded"), STANDARD("Standard"), MONO("Mono") }

enum class WalloraTheme(val title: String, val fg: Int, val bg: Int, val bg2: Int) {
    SAFFRON("Saffron", 0xFFFFF8E7.toInt(), 0xFFE8761E.toInt(), 0xFFB5460F.toInt()),
    MIDNIGHT("Midnight", 0xFFF2E9D8.toInt(), 0xFF1B1F3B.toInt(), 0xFF0B0D1C.toInt()),
    PARCHMENT("Parchment", 0xFF3B2A1A.toInt(), 0xFFF4E9D3.toInt(), 0xFFE3D0A8.toInt()),
    LOTUS("Lotus", 0xFFFFE9F3.toInt(), 0xFFA8467F.toInt(), 0xFF5C1F4A.toInt()),
    FOREST("Forest", 0xFFE6F2E6.toInt(), 0xFF2E5A45.toInt(), 0xFF14302A.toInt()),
    AMOLED("AMOLED", 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt())
}

data class WidgetDesign(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My Widget",
    val kind: Kind = Kind.SHLOKA,
    val text: String = "Jai Shri Ram",
    val symbol: String = "✦",
    val language: ShlokaLang = ShlokaLang.SANSKRIT_EN,
    val rotationHours: Int = 24,
    val fg: Int = WalloraTheme.SAFFRON.fg,
    val bg: Int = WalloraTheme.SAFFRON.bg,
    val bg2: Int? = WalloraTheme.SAFFRON.bg2,
    val font: FontStyle = FontStyle.SERIF,
    val targetEpochDay: Long = LocalDate.now().toEpochDay() + 30,
    val photo: String? = null
) {
    fun withTheme(t: WalloraTheme) = copy(fg = t.fg, bg = t.bg, bg2 = t.bg2)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("kind", kind.name); put("text", text); put("symbol", symbol)
        put("language", language.name); put("rotationHours", rotationHours); put("fg", fg); put("bg", bg)
        if (bg2 != null) put("bg2", bg2)
        put("font", font.name); put("targetEpochDay", targetEpochDay)
        if (photo != null) put("photo", photo)
    }

    companion object {
        private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default

        fun fromJson(o: JSONObject): WidgetDesign {
            val base = WidgetDesign()
            return WidgetDesign(
                id = o.optString("id", base.id),
                name = o.optString("name", base.name),
                kind = enumOr(o.optString("kind"), Kind.SHLOKA),
                text = o.optString("text", base.text),
                symbol = o.optString("symbol", base.symbol),
                language = enumOr(o.optString("language"), ShlokaLang.SANSKRIT_EN),
                rotationHours = o.optInt("rotationHours", 24),
                fg = o.optInt("fg", base.fg),
                bg = o.optInt("bg", base.bg),
                bg2 = if (o.has("bg2")) o.getInt("bg2") else null,
                font = enumOr(o.optString("font"), FontStyle.SERIF),
                targetEpochDay = o.optLong("targetEpochDay", base.targetEpochDay),
                photo = if (o.has("photo")) o.getString("photo") else null
            )
        }
    }
}

object DesignStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "designs.json")

    fun load(ctx: Context): List<WidgetDesign> = runCatching {
        val arr = JSONArray(file(ctx).readText())
        List(arr.length()) { WidgetDesign.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun save(ctx: Context, list: List<WidgetDesign>) {
        val arr = JSONArray(); list.forEach { arr.put(it.toJson()) }
        file(ctx).writeText(arr.toString())
    }
    fun upsert(ctx: Context, d: WidgetDesign) {
        val list = load(ctx).toMutableList()
        val i = list.indexOfFirst { it.id == d.id }
        if (i >= 0) list[i] = d else list.add(d)
        save(ctx, list)
    }
    fun delete(ctx: Context, d: WidgetDesign) = save(ctx, load(ctx).filter { it.id != d.id })

    /** Shrinks the picked photo (widgets have a small memory budget) and stores it privately. */
    fun savePhoto(ctx: Context, uri: Uri, id: String): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val name = "bg-$id-${System.currentTimeMillis()}.jpg"
        File(ctx.filesDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    }.getOrNull()

    fun photo(ctx: Context, name: String?): Bitmap? =
        name?.let { runCatching { BitmapFactory.decodeFile(File(ctx.filesDir, it).path) }.getOrNull() }
}
