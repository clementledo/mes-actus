package fr.mesactus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.mesactus.data.Article
import fr.mesactus.data.Category
import fr.mesactus.data.Country
import fr.mesactus.data.Level
import fr.mesactus.data.Zone
import fr.mesactus.data.weatherLabel
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenArticle: (Article) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    // 0..2 = pays, 3 = favoris
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val countries = Country.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab < 3) "Mes Actus · ${countries[tab].label}" else "Mes favoris") },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Rechercher") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Réglages") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                countries.forEachIndexed { i, c ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(c.flag, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(c.label) },
                    )
                }
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("Favoris") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (tab < 3) CountryFeed(vm, countries[tab], onOpenArticle)
            else FavoritesList(vm, onOpenArticle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryFeed(vm: MainViewModel, country: Country, onOpenArticle: (Article) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val feeds by vm.feeds.collectAsStateWithLifecycle()
    val zones = Zone.of(country).filter { it.level == Level.NATIONAL || it !in settings.hiddenZones }

    var zoneIndex by rememberSaveable(country) { mutableIntStateOf(0) }
    if (zoneIndex >= zones.size) zoneIndex = 0
    val zone = zones[zoneIndex]
    var category by rememberSaveable(country, zone) { mutableStateOf<String?>(null) }

    LaunchedEffect(zone) { vm.loadZone(zone) }
    LaunchedEffect(country) { vm.loadWeather(country) }

    val feed = feeds[zone] ?: ZoneFeed(loading = true)
    val selectedCat = category?.let { runCatching { Category.valueOf(it) }.getOrNull() }
    val articles = if (selectedCat == null) feed.articles else feed.articles.filter { selectedCat in it.categories }
    val listState = rememberLazyListState()
    LaunchedEffect(zone, category) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize()) {
        if (zones.size > 1) {
            ScrollableTabRow(selectedTabIndex = zoneIndex, edgePadding = 8.dp) {
                zones.forEachIndexed { i, z ->
                    Tab(selected = i == zoneIndex, onClick = { zoneIndex = i }, text = { Text(z.label) })
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = feed.loading && feed.loaded,
            onRefresh = { vm.loadZone(zone, force = true); vm.loadWeather(country, force = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                item(key = "weather") { WeatherHeader(vm, country) }
                item(key = "categories") {
                    val counts = Category.entries.associateWith { c -> feed.articles.count { c in it.categories } }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCat == null,
                                onClick = { category = null },
                                label = { Text("Tout (${feed.articles.size})") },
                            )
                        }
                        items(Category.entries.filter { (counts[it] ?: 0) > 0 }) { c ->
                            FilterChip(
                                selected = selectedCat == c,
                                onClick = { category = if (selectedCat == c) null else c.name },
                                label = { Text("${c.label} (${counts[c]})") },
                            )
                        }
                    }
                }
                if (feed.failedSources.isNotEmpty() && feed.loaded) {
                    item(key = "errors") {
                        Text(
                            "Indisponible pour l'instant : " + feed.failedSources.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                when {
                    !feed.loaded -> item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    articles.isEmpty() -> item(key = "empty") {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                if (feed.articles.isEmpty()) "Aucun article. Vérifiez votre connexion ou les sources de cette zone."
                                else "Aucun article dans ce thème.",
                                textAlign = TextAlign.Center,
                            )
                            OutlinedButton(onClick = { vm.loadZone(zone, force = true) }) { Text("Réessayer") }
                        }
                    }
                    else -> items(articles, key = { it.id }) { a ->
                        ArticleItem(a, settings.listStyle, onClick = { onOpenArticle(a) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherHeader(vm: MainViewModel, country: Country) {
    val weather by vm.weather.collectAsStateWithLifecycle()
    val w = weather[country]
    val localTime = remember(country, w) {
        ZonedDateTime.now(ZoneId.of(country.timeZone)).format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (w == null) {
                Text("${country.city} · $localTime · météo en chargement…", style = MaterialTheme.typography.bodyMedium)
            } else {
                val (emoji, label) = weatherLabel(w.code, w.isDay)
                Text(emoji, style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        "${w.temperature.roundToInt()}°  ${country.city}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "$label · ${w.min.roundToInt()}° / ${w.max.roundToInt()}° · vent ${w.wind.roundToInt()} km/h",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(localTime, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FavoritesList(vm: MainViewModel, onOpenArticle: (Article) -> Unit) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "Aucun favori.\nOuvrez un article puis touchez l'étoile pour l'enregistrer : il restera lisible sans réseau.",
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(favorites, key = { it.article.id }) { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    ArticleItem(f.article, settings.listStyle, onClick = { onOpenArticle(f.article) }, showZone = true)
                }
                IconButton(onClick = { vm.removeFavorite(f.article.id) }) {
                    Icon(Icons.Default.Delete, "Retirer des favoris")
                }
            }
        }
    }
}
