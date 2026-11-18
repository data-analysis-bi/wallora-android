package com.wallora.app

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        enableEdgeToEdge()
        setContent { WalloraMaterialTheme { WalloraApp() } }
    }
}

@Composable
fun WalloraMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content)
}

private data class TabDef(val label: String, val icon: ImageVector)

@Composable
fun WalloraApp() {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var viewer by remember { mutableStateOf<Wallpaper?>(null) }
    LaunchedEffect(Unit) { CatalogState.load(ctx) }
    val tabs = listOf(
        TabDef("Discover", Icons.Filled.AutoAwesome), TabDef("Categories", Icons.Filled.GridView),
        TabDef("Widgets", Icons.Filled.Widgets), TabDef("Favorites", Icons.Filled.Favorite), TabDef("Profile", Icons.Filled.Person)
    )
    Box(Modifier.fillMaxSize()) {
        Scaffold(bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(t.icon, t.label) }, label = { Text(t.label, maxLines = 1) })
                }
            }
        }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> DiscoverScreen { viewer = it }
                    1 -> CategoriesScreen { viewer = it }
                    2 -> WidgetsScreen()
                    3 -> FavoritesScreen { viewer = it }
                    else -> ProfileScreen()
                }
            }
        }
        viewer?.let { w ->
            BackHandler { viewer = null }
            ViewerScreen(w) { viewer = null }
        }
    }
}

@Composable
fun WallpaperThumb(w: Wallpaper, modifier: Modifier = Modifier, full: Boolean = false) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, w.id, full) { value = Images.load(ctx, w, full) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bmp?.let { Image(it.asImageBitmap(), w.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

fun LazyGridScope.wallpaperItems(list: List<Wallpaper>, onOpen: (Wallpaper) -> Unit) {
    items(list, key = { it.id }) { w ->
        Box(Modifier.aspectRatio(9f / 19.5f).clip(RoundedCornerShape(22.dp)).clickable { onOpen(w) }) {
            WallpaperThumb(w, Modifier.fillMaxSize())
            Text(
                w.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun DiscoverScreen(onOpen: (Wallpaper) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = CatalogState.all
    val list = remember(query, all) { if (query.isBlank()) CatalogState.featured() else CatalogState.search(query) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Discover", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onOpen(all.random()) }) { Icon(Icons.Filled.Casino, "Random wallpaper") }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search wallpapers & categories") }, shape = RoundedCornerShape(16.dp)
                )
            }
        }
        if (query.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val daily = CatalogState.daily()
                Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(30.dp)).clickable { onOpen(daily) }) {
                    WallpaperThumb(daily, Modifier.fillMaxSize(), full = true)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = 400f)))
                    Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Text("WALLPAPER OF THE DAY", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                        Text(daily.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { ShlokaCard() }
            item(span = { GridItemSpan(maxLineSpan) }) { Text("Featured", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        }
        wallpaperItems(list, onOpen)
        if (query.isBlank() && !CatalogState.hasRemote) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Add an Unsplash key in Profile to unlock thousands more photos.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ShlokaCard() {
    val ctx = LocalContext.current
    var lang by rememberSaveable { mutableStateOf(ShlokaLang.SANSKRIT_EN.name) }
    var menu by remember { mutableStateOf(false) }
    val l = ShlokaLang.valueOf(lang)
    val s = remember { ShlokaStore.pick(ctx, LocalDateTime.now(), 24) }
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8761E).copy(alpha = 0.18f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SHLOKA OF THE DAY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp, modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { menu = true }) { Text(l.title) }
                    DropdownMenu(menu, { menu = false }) {
                        ShlokaLang.values().forEach { opt -> DropdownMenuItem(text = { Text(opt.title) }, onClick = { lang = opt.name; menu = false }) }
                    }
                }
            }
            if (s != null) {
                SelectionContainer {
                    Column {
                        ShlokaStore.lines(s, l).forEachIndexed { i, line ->
                            Text(line, fontFamily = FontFamily.Serif, fontSize = if (i == 0) 19.sp else 14.sp,
                                fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (i == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                Text("Valmiki Ramayana · ${s.ref}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun CategoriesScreen(onOpen: (Wallpaper) -> Unit) {
    var selected by remember { mutableStateOf<WallCategory?>(null) }
    val sel = selected
    if (sel != null) {
        BackHandler { selected = null }
        val items = CatalogState.inCategory(sel)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selected = null }) { Icon(Icons.Filled.ArrowBack, "Back") }
                Text(sel.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (items.isEmpty()) {
                Text("Nothing here yet. Add an Unsplash access key in Profile to browse licensed photos.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                ) { wallpaperItems(items, onOpen) }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Categories", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            items(WallCategory.values().toList(), key = { it.name }) { c ->
                Box(
                    Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(c.c1), Color(c.c2)))).clickable { selected = c }
                ) {
                    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Text(c.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("${CatalogState.inCategory(c).size} wallpapers", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
