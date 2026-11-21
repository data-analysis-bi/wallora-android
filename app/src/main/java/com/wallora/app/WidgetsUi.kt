package com.wallora.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun DesignPreview(d: WidgetDesign, wDp: Int, hDp: Int, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val dens = ctx.resources.displayMetrics.density
    val bmp = remember(d, wDp, hDp) { WidgetRenderer.render(ctx, d, (wDp * dens).toInt(), (hDp * dens).toInt()) }
    Image(bmp.asImageBitmap(), d.name, modifier.size(wDp.dp, hDp.dp))
}

private fun template(name: String, kind: Kind, text: String, symbol: String, theme: WalloraTheme, font: FontStyle, lang: ShlokaLang = ShlokaLang.SANSKRIT_EN) =
    WidgetDesign(name = name, kind = kind, text = text, symbol = symbol, font = font, language = lang).withTheme(theme)

private fun pinWidget(ctx: Context) {
    val mgr = AppWidgetManager.getInstance(ctx)
    if (mgr.isRequestPinAppWidgetSupported) {
        mgr.requestPinAppWidget(ComponentName(ctx, CustomWidgetProvider::class.java), null, null)
    } else {
        Toast.makeText(ctx, "Long-press your Home Screen → Widgets → Wallora", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun WidgetsScreen() {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<WidgetDesign?>(null) }
    var reader by remember { mutableStateOf(false) }
    val e = editing
    when {
        e != null -> {
            BackHandler { editing = null }
            StudioScreen(e, "Save design", onConfirm = { DesignStore.upsert(ctx, it); editing = null }, onCancel = { editing = null })
        }
        reader -> {
            BackHandler { reader = false }
            ReaderScreen { reader = false }
        }
        else -> {
            val templates = remember {
                listOf(
                    template("Saffron Shloka", Kind.SHLOKA, "", "✦", WalloraTheme.SAFFRON, FontStyle.SERIF),
                    template("Midnight Hindi", Kind.SHLOKA, "", "☾", WalloraTheme.MIDNIGHT, FontStyle.SERIF, ShlokaLang.HINDI),
                    template("Parchment Gujarati", Kind.SHLOKA, "", "ॐ", WalloraTheme.PARCHMENT, FontStyle.SERIF, ShlokaLang.GUJARATI),
                    template("Countdown", Kind.COUNTDOWN, "Diwali", "❁", WalloraTheme.LOTUS, FontStyle.ROUNDED),
                    template("Focus", Kind.TEXT, "Stay present", "❀", WalloraTheme.FOREST, FontStyle.ROUNDED)
                )
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item { Text("Widgets", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
                item {
                    Card(shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add a widget", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Text("Tap the button, confirm, then design it on the setup screen and tap Add widget. Or: long-press your Home Screen → Widgets → Wallora.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Button(onClick = { pinWidget(ctx) }, modifier = Modifier.fillMaxWidth()) { Text("Add “Ramayana & Custom” to Home Screen") }
                            Text("Tip: touch and hold a widget → Reconfigure (or tap Settings) to change it later.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.clickable { reader = true }) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Valmiki Ramayana", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Text("All 7 kandas · Sanskrit in Devanagari or Gujarati script", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
                item {
                    Text("Preview & design", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 10.dp)) {
                        items(templates, key = { it.id }) { t ->
                            Column(Modifier.clickable { editing = t }) {
                                DesignPreview(t, 158, 158)
                                Text(t.name, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
                item { OutlinedButton(onClick = { editing = WidgetDesign() }, modifier = Modifier.fillMaxWidth()) { Text("Create a design") } }
                item {
                    Text("Built-in widgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("• Ramayana & Custom — resizable, configurable\n• Calendar — black, bold day and month grid\n(Both are in the Widgets list on your Home Screen.)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun HexField(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(String.format("%06X", value and 0xFFFFFF)) }
    OutlinedTextField(
        value = text, singleLine = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        onValueChange = { t ->
            text = t.uppercase().take(7)
            val clean = t.replace("#", "")
            if (clean.length == 6 && clean.all { it.isDigitOrHex() }) onChange(0xFF000000.toInt() or clean.toInt(16))
        }
    )
}

private fun Char.isDigitOrHex() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/** The widget designer. Used by the Widgets tab AND by the screen that opens when you add a widget. */
@Composable
fun StudioScreen(initial: WidgetDesign, confirmLabel: String, onConfirm: (WidgetDesign) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    var d by remember { mutableStateOf(initial) }
    var sizeIdx by remember { mutableIntStateOf(0) }
    var langMenu by remember { mutableStateOf(false) }
    var savedMenu by remember { mutableStateOf(false) }
    var daysText by remember { mutableStateOf((initial.targetEpochDay - LocalDate.now().toEpochDay()).toString()) }
    val dims = listOf(158 to 158, 338 to 158, 338 to 354)[sizeIdx]
    val saved = remember { DesignStore.load(ctx) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) DesignStore.savePhoto(ctx, uri, d.id)?.let { d = d.copy(photo = it) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Small", "Medium", "Large").forEachIndexed { i, t -> FilterChip(selected = sizeIdx == i, onClick = { sizeIdx = i }, label = { Text(t) }) }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DesignPreview(d, dims.first, dims.second) }

        if (saved.isNotEmpty()) {
            Box {
                OutlinedButton(onClick = { savedMenu = true }) { Text("Load a saved design") }
                DropdownMenu(savedMenu, { savedMenu = false }) {
                    saved.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { d = s; savedMenu = false }) }
                }
            }
        }

        OutlinedTextField(d.name, { d = d.copy(name = it) }, label = { Text("Design name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("Type", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Kind.values().forEach { k -> FilterChip(selected = d.kind == k, onClick = { d = d.copy(kind = k) }, label = { Text(k.title) }) }
        }

        if (d.kind == Kind.SHLOKA) {
            Box {
                OutlinedButton(onClick = { langMenu = true }) { Text("Language: ${d.language.title}") }
                DropdownMenu(langMenu, { langMenu = false }) {
                    ShlokaLang.values().forEach { l -> DropdownMenuItem(text = { Text(l.title) }, onClick = { d = d.copy(language = l); langMenu = false }) }
                }
            }
            Text("Change shloka", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(24 to "Daily", 12 to "12 h", 6 to "6 h", 3 to "3 h", 1 to "Hourly").forEach { (h, label) ->
                    FilterChip(selected = d.rotationHours == h, onClick = { d = d.copy(rotationHours = h) }, label = { Text(label) })
                }
            }
        }
        if (d.kind == Kind.TEXT || d.kind == Kind.COUNTDOWN) {
            OutlinedTextField(d.text, { d = d.copy(text = it) }, label = { Text(if (d.kind == Kind.TEXT) "Your text" else "Event name") }, modifier = Modifier.fillMaxWidth())
        }
        if (d.kind == Kind.COUNTDOWN) {
            OutlinedTextField(
                daysText, { t ->
                    daysText = t.filter { it.isDigit() }.take(5)
                    daysText.toLongOrNull()?.let { n -> d = d.copy(targetEpochDay = LocalDate.now().toEpochDay() + n) }
                },
                label = { Text("Days from today") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Text("Theme", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            WalloraTheme.values().forEach { t ->
                Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(t.bg), Color(t.bg2))))
                    .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape).clickable { d = d.withTheme(t) })
            }
        }
        Text("Font", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FontStyle.values().forEach { f -> FilterChip(selected = d.font == f, onClick = { d = d.copy(font = f) }, label = { Text(f.title) }) }
        }
        HexField("Text color (hex)", d.fg) { d = d.copy(fg = it) }
        HexField("Background color (hex)", d.bg) { d = d.copy(bg = it, bg2 = null) }

        Text("Symbol", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("✦", "☀", "☾", "❁", "❀", "❤", "⚡", "📖", "ॐ", "").forEach { s ->
                FilterChip(selected = d.symbol == s, onClick = { d = d.copy(symbol = s) }, label = { Text(if (s.isEmpty()) "None" else s, fontSize = 18.sp) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch("image/*") }) { Text("Background photo") }
            if (d.photo != null) OutlinedButton(onClick = { d = d.copy(photo = null) }) { Text("Remove photo") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            if (confirmLabel != "Save design") {
                OutlinedButton(onClick = { DesignStore.upsert(ctx, d); Toast.makeText(ctx, "Saved to Favorites", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Text("Save design") }
            }
            Button(onClick = { onConfirm(d) }, modifier = Modifier.weight(1.3f)) { Text(confirmLabel) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun FavoritesScreen(onOpen: (Wallpaper) -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<WidgetDesign?>(null) }
    var designs by remember { mutableStateOf(DesignStore.load(ctx)) }
    val e = editing
    if (e != null) {
        BackHandler { editing = null }
        StudioScreen(e, "Save design", onConfirm = { DesignStore.upsert(ctx, it); designs = DesignStore.load(ctx); editing = null }, onCancel = { editing = null })
        return
    }
    Column(Modifier.fillMaxSize()) {
        Text("Favorites", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Wallpapers") })
            FilterChip(selected = tab == 1, onClick = { tab = 1; designs = DesignStore.load(ctx) }, label = { Text("Widgets") })
        }
        if (tab == 0) {
            val items = (CatalogState.all + Prefs.favoriteItems).distinctBy { it.id }.filter { it.id in Prefs.favorites }
            if (items.isEmpty()) Text("No favorites yet. Tap the heart on any wallpaper.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyVerticalGrid(
                columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { wallpaperItems(items, onOpen) }
        } else {
            if (designs.isEmpty()) Text("No saved widget designs. Create one in the Widgets tab.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(designs, key = { it.id }) { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clickable { editing = d }) { DesignPreview(d, 338, 158) }
                        IconButton(onClick = { DesignStore.delete(ctx, d); designs = DesignStore.load(ctx) }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }
}
