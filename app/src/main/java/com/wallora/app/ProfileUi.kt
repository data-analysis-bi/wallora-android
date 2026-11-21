package com.wallora.app

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Profile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            Text("Download quality", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(selected = Prefs.quality == "high", onClick = { Prefs.updateQuality("high") }, label = { Text("High") })
                FilterChip(selected = Prefs.quality == "ultra", onClick = { Prefs.updateQuality("ultra") }, label = { Text("Ultra (largest)") })
            }
        }
        item {
            Text("Online catalog", fontWeight = FontWeight.SemiBold)
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !CatalogState.loading, onClick = { scope.launch { CatalogState.load(ctx, force = true) } }) { Text("Refresh catalog") }
                if (CatalogState.loading) CircularProgressIndicator(Modifier.padding(4.dp))
            }
            CatalogState.status?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
            Text("Online wallpapers come from public-domain collections: The Met, Art Institute of Chicago (CC0) and NASA. The credit is shown on each wallpaper.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Text("Widgets", fontWeight = FontWeight.SemiBold)
            OutlinedButton(modifier = Modifier.padding(top = 8.dp), onClick = {
                val json = DesignStore.load(ctx).let { list -> org.json.JSONArray().also { a -> list.forEach { a.put(it.toJson()) } }.toString(2) }
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, json); putExtra(Intent.EXTRA_SUBJECT, "Wallora widget designs") }
                ctx.startActivity(Intent.createChooser(send, "Back up widget designs"))
            }) { Text("Back up widget designs") }
        }
        item {
            Text("Downloads (${Prefs.downloads.size})", fontWeight = FontWeight.SemiBold)
            Prefs.downloads.take(10).forEach { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
        }
        item {
            OutlinedButton(onClick = { Images.clearCache(ctx) }) { Text("Clear image cache") }
        }
        item {
            HorizontalDivider()
            Text(
                "Wallora sets your Home and Lock Screen wallpaper directly. Widget refresh timing is controlled by Android; " +
                    "most Android phones don't offer Lock Screen widgets, so Wallora provides Home Screen widgets only.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun ReaderScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var rows by remember { mutableStateOf<List<VerseRow>?>(null) }
    var kanda by remember { mutableStateOf<Int?>(null) }
    var sarga by remember { mutableStateOf<Int?>(null) }
    var gu by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { rows = withContext(Dispatchers.Default) { RamayanaText.load(ctx) } }
    BackHandler { when { sarga != null -> sarga = null; kanda != null -> kanda = null; else -> onBack() } }

    val all = rows
    val k = kanda
    val s = sarga
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { when { s != null -> sarga = null; k != null -> kanda = null; else -> onBack() } }) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text(
                when { k == null -> "Valmiki Ramayana"; s == null -> RamayanaText.kandas[k].first; else -> "${RamayanaText.kandas[k].first} · $s" },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            if (s != null) FilterChip(selected = gu, onClick = { gu = !gu }, label = { Text(if (gu) "ગુ" else "दे") })
        }
        if (all == null) {
            CircularProgressIndicator(Modifier.padding(24.dp))
        } else if (k == null) {
            LazyColumn {
                items(RamayanaText.kandas.indices.toList()) { i ->
                    Column(Modifier.fillMaxWidth().clickable { kanda = i }.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text(RamayanaText.kandas[i].second, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(RamayanaText.kandas[i].first, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
                item {
                    Text("${all.size} shlokas. Text from the open DharmicData / Valmiki Ramayana dataset (ODbL licence). It may contain typos — check a printed edition for recitation.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
                }
            }
        } else if (s == null) {
            val sargas = remember(k, all) { all.filter { it.k == k }.map { it.s }.distinct().sorted() }
            LazyColumn {
                items(sargas) { n ->
                    Text("Sarga $n", Modifier.fillMaxWidth().clickable { sarga = n }.padding(horizontal = 20.dp, vertical = 14.dp))
                    HorizontalDivider()
                }
            }
        } else {
            val verses = remember(k, s, all) { all.filter { it.k == k && it.s == s } }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(verses) { v ->
                    Column {
                        Text(if (gu) Script.gujarati(v.t) else v.t, fontFamily = FontFamily.Serif, fontSize = 17.sp)
                        Text("${v.k + 1}.${v.s}.${v.n}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
