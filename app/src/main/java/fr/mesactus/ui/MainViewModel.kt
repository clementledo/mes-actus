package fr.mesactus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mesactus.data.Article
import fr.mesactus.data.Country
import fr.mesactus.data.Favorite
import fr.mesactus.data.FavoriteStore
import fr.mesactus.data.FrenchTranslator
import fr.mesactus.data.ReaderContent
import fr.mesactus.data.ReaderExtractor
import fr.mesactus.data.Repository
import fr.mesactus.data.Settings
import fr.mesactus.data.SettingsStore
import fr.mesactus.data.Source
import fr.mesactus.data.SourceStore
import fr.mesactus.data.TextNormalizer
import fr.mesactus.data.Weather
import fr.mesactus.data.Zone
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ZoneFeed(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val articles: List<Article> = emptyList(),
    val failedSources: List<String> = emptyList(),
)

data class ReaderState(
    val article: Article? = null,
    val loading: Boolean = false,
    val content: ReaderContent? = null,
    val error: String? = null,
    val translated: ReaderContent? = null,
    val translating: Boolean = false,
    val showTranslated: Boolean = false,
    val translateError: String? = null,
)

data class SearchState(
    val query: String = "",
    val loading: Boolean = false,
    val searched: Boolean = false,
    val results: List<Article> = emptyList(),
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository()
    private val sourceStore = SourceStore(app)
    private val favoriteStore = FavoriteStore(app)
    private val settingsStore = SettingsStore(app)

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _sources = MutableStateFlow(sourceStore.load())
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _favorites = MutableStateFlow(favoriteStore.load())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    private val _feeds = MutableStateFlow<Map<Zone, ZoneFeed>>(emptyMap())
    val feeds: StateFlow<Map<Zone, ZoneFeed>> = _feeds.asStateFlow()

    private val _weather = MutableStateFlow<Map<Country, Weather>>(emptyMap())
    val weather: StateFlow<Map<Country, Weather>> = _weather.asStateFlow()

    private val _reader = MutableStateFlow(ReaderState())
    val reader: StateFlow<ReaderState> = _reader.asStateFlow()

    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search.asStateFlow()

    private var readerJob: Job? = null
    private var searchJob: Job? = null

    // ---------------- Fils d'actualité ----------------

    fun loadZone(zone: Zone, force: Boolean = false) {
        val cur = _feeds.value[zone]
        if (cur?.loading == true) return
        if (!force && cur?.loaded == true) return
        viewModelScope.launch { loadZoneNow(zone) }
    }

    private suspend fun loadZoneNow(zone: Zone) {
        _feeds.update { it + (zone to (it[zone] ?: ZoneFeed()).copy(loading = true)) }
        val enabled = _sources.value.filter { it.zone == zone && it.enabled }
        val result = repo.fetchZone(enabled)
        _feeds.update { it + (zone to ZoneFeed(false, true, result.articles, result.failedSources)) }
    }

    fun loadWeather(country: Country, force: Boolean = false) {
        if (!force && _weather.value.containsKey(country)) return
        viewModelScope.launch {
            runCatching { repo.fetchWeather(country) }.onSuccess { w -> _weather.update { it + (country to w) } }
        }
    }

    // ---------------- Recherche ----------------

    fun search(query: String) {
        val q = query.trim()
        _search.value = SearchState(query = q, loading = q.isNotEmpty())
        searchJob?.cancel()
        if (q.isEmpty()) return
        searchJob = viewModelScope.launch {
            val zones = Zone.entries.filter { it !in _settings.value.hiddenZones }
            zones.filter { _feeds.value[it]?.loaded != true }
                .map { z -> async { loadZoneNow(z) } }
                .awaitAll()
            val terms = TextNormalizer.normalize(q).trim().split(" ").filter { it.isNotBlank() }
            val results = zones.flatMap { _feeds.value[it]?.articles.orEmpty() }
                .filter { a ->
                    val text = TextNormalizer.normalize(a.title + " " + a.description + " " + a.sourceName)
                    terms.all { text.contains(" $it") }
                }
                .distinctBy { it.id }
                .sortedByDescending { it.published }
            _search.value = SearchState(query = q, loading = false, searched = true, results = results)
        }
    }

    // ---------------- Lecture ----------------

    fun openArticle(article: Article) {
        readerJob?.cancel()
        val fav = _favorites.value.firstOrNull { it.article.id == article.id }
        if (fav?.content != null) {
            _reader.value = ReaderState(article = article, content = fav.content)
            return
        }
        _reader.value = ReaderState(article = article, loading = true)
        readerJob = viewModelScope.launch {
            val content = runCatching { repo.fetchReader(article) }
            content.onSuccess { c ->
                _reader.update { it.copy(loading = false, content = c) }
                // Favori enregistré avant que le texte ne soit chargé : on complète.
                if (_favorites.value.any { it.article.id == article.id && it.content == null }) {
                    updateFavorites { list -> list.map { if (it.article.id == article.id) it.copy(content = c) else it } }
                }
            }.onFailure { e ->
                _reader.update {
                    it.copy(
                        loading = false,
                        content = ReaderExtractor.fallbackContent(article),
                        error = "Impossible de charger l'article (${e.message ?: "erreur réseau"}).",
                    )
                }
            }
        }
    }

    fun toggleTranslation() {
        val state = _reader.value
        val article = state.article ?: return
        val content = state.content ?: return
        if (state.translated != null) {
            _reader.update { it.copy(showTranslated = !it.showTranslated) }
            return
        }
        if (state.translating) return
        _reader.update { it.copy(translating = true, translateError = null) }
        viewModelScope.launch {
            runCatching {
                val texts = listOf(content.title) + content.blocks.map { it.text }
                val out = FrenchTranslator.translate(texts, article.lang)
                content.copy(
                    title = out[0],
                    blocks = content.blocks.mapIndexed { i, b -> b.copy(text = out[i + 1]) },
                )
            }.onSuccess { tr ->
                _reader.update { if (it.article?.id == article.id) it.copy(translating = false, translated = tr, showTranslated = true) else it }
            }.onFailure { e ->
                _reader.update {
                    it.copy(translating = false, translateError = "Traduction impossible : ${e.message ?: "erreur"}")
                }
            }
        }
    }

    // ---------------- Favoris ----------------

    fun isFavorite(id: String): Boolean = _favorites.value.any { it.article.id == id }

    fun toggleFavorite(article: Article) {
        if (isFavorite(article.id)) {
            updateFavorites { list -> list.filterNot { it.article.id == article.id } }
        } else {
            val content = _reader.value.takeIf { it.article?.id == article.id }?.content
            updateFavorites { list -> listOf(Favorite(article, content, System.currentTimeMillis())) + list }
        }
    }

    fun removeFavorite(id: String) = updateFavorites { list -> list.filterNot { it.article.id == id } }

    private fun updateFavorites(transform: (List<Favorite>) -> List<Favorite>) {
        _favorites.update(transform)
        favoriteStore.save(_favorites.value)
    }

    // ---------------- Réglages ----------------

    fun updateSettings(transform: (Settings) -> Settings) {
        _settings.update(transform)
        settingsStore.save(_settings.value)
    }

    // ---------------- Sources ----------------

    private fun updateSources(zones: Set<Zone>, transform: (List<Source>) -> List<Source>) {
        _sources.update(transform)
        sourceStore.save(_sources.value)
        // Les fils concernés seront rechargés à la prochaine ouverture.
        _feeds.update { it - zones }
    }

    fun toggleSource(id: String) {
        val s = _sources.value.firstOrNull { it.id == id } ?: return
        updateSources(setOf(s.zone)) { list -> list.map { if (it.id == id) it.copy(enabled = !it.enabled) else it } }
    }

    fun deleteSource(id: String) {
        val s = _sources.value.firstOrNull { it.id == id } ?: return
        updateSources(setOf(s.zone)) { list -> list.filterNot { it.id == id } }
    }

    /** Teste le flux avant de l'ajouter. Renvoie un message d'erreur, ou null si tout va bien. */
    suspend fun addSource(name: String, url: String, zone: Zone, lang: String): String? {
        val cleanUrl = url.trim().let { if (it.startsWith("http")) it else "https://$it" }
        if (_sources.value.any { it.url == cleanUrl && it.zone == zone }) return "Cette source existe déjà dans cette zone."
        val source = Source(
            id = "custom_" + System.currentTimeMillis(),
            name = name.trim().ifBlank { cleanUrl.substringAfter("://").substringBefore('/') },
            url = cleanUrl,
            zone = zone,
            lang = lang,
            custom = true,
        )
        val test = runCatching { repo.fetchSource(source) }
        val count = test.getOrNull()?.size ?: 0
        if (count == 0) {
            return "Aucun article trouvé à cette adresse. Vérifiez qu'il s'agit bien d'un flux RSS ou Atom" +
                (test.exceptionOrNull()?.message?.let { " ($it)" } ?: "") + "."
        }
        updateSources(setOf(zone)) { it + source }
        return null
    }

    fun resetSources() {
        sourceStore.reset()
        _sources.value = sourceStore.load()
        _feeds.value = emptyMap()
    }
}
