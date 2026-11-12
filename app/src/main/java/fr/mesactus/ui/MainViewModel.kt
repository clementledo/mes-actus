package fr.mesactus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mesactus.data.Article
import fr.mesactus.data.DefaultSources
import fr.mesactus.data.FrenchTranslator
import fr.mesactus.data.Ranking
import fr.mesactus.data.ReaderContent
import fr.mesactus.data.ReaderExtractor
import fr.mesactus.data.Repository
import fr.mesactus.data.Section
import fr.mesactus.data.Settings
import fr.mesactus.data.SettingsStore
import fr.mesactus.data.Story
import fr.mesactus.BuildConfig
import fr.mesactus.data.AppUpdate
import fr.mesactus.data.Updater
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SectionState(
    val loading: Boolean = true,
    val stories: List<Story> = emptyList(),
)

data class ReaderState(
    val article: Article? = null,
    val outlets: List<String> = emptyList(),
    val loading: Boolean = false,
    val content: ReaderContent? = null,
    val error: String? = null,
    val translated: ReaderContent? = null,
    val translating: Boolean = false,
    val showTranslated: Boolean = false,
    val translateError: String? = null,
)

data class UpdateState(
    val available: AppUpdate? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository()
    private val settingsStore = SettingsStore(app)

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _brief = MutableStateFlow<Map<Section, SectionState>>(emptyMap())
    val brief: StateFlow<Map<Section, SectionState>> = _brief.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _updatedAt = MutableStateFlow(0L)
    val updatedAt: StateFlow<Long> = _updatedAt.asStateFlow()

    private val _reader = MutableStateFlow(ReaderState())
    val reader: StateFlow<ReaderState> = _reader.asStateFlow()

    private val _update = MutableStateFlow(UpdateState())
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    private var readerJob: Job? = null
    private var refreshJob: Job? = null

    init {
        refresh()
        checkUpdate()
    }

    // ---------------- Mise à jour de l'app ----------------

    fun checkUpdate() {
        if (_update.value.downloading) return
        viewModelScope.launch {
            val found = runCatching { Updater.check(BuildConfig.VERSION_NAME) }.getOrNull()
            _update.update { it.copy(available = found) }
        }
    }

    fun installUpdate() {
        val app = getApplication<Application>()
        val upd = _update.value.available ?: return
        if (_update.value.downloading) return
        if (!Updater.canInstall(app)) {
            Updater.openInstallPermission(app)
            return
        }
        _update.update { it.copy(downloading = true, progress = 0f, error = null) }
        viewModelScope.launch {
            runCatching {
                Updater.download(app, upd) { p -> _update.update { it.copy(progress = p) } }
            }.onSuccess { file ->
                _update.update { it.copy(downloading = false, progress = 1f) }
                Updater.install(app, file)
            }.onFailure { e ->
                _update.update { it.copy(downloading = false, error = "Téléchargement impossible (${e.message ?: "réseau"})") }
            }
        }
    }

    /** Recharge si les infos ont plus de 20 minutes (au retour dans l'app). */
    fun refreshIfStale() {
        checkUpdate()
        if (System.currentTimeMillis() - _updatedAt.value > 20 * 60_000) refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _refreshing.value = true
            val sections = Section.entries.filter { it !in _settings.value.hiddenSections }
            val jobs = sections.map { section ->
                launch {
                    _brief.update { it + (section to (it[section] ?: SectionState()).copy(loading = true)) }
                    val sources = DefaultSources.all.filter { it.zone in section.zones }
                    val result = repo.fetchZone(sources)
                    val stories = Ranking.topStories(result.articles, section.count)
                    _brief.update { it + (section to SectionState(loading = false, stories = stories)) }
                    // Ajoute un visuel aux grands titres qui n'en ont pas (image de la page de l'article).
                    if (stories.any { it.lead.imageUrl == null }) {
                        val withImages = stories.map { st ->
                            async {
                                if (st.lead.imageUrl != null) st
                                else withTimeoutOrNull(8_000) { repo.fetchImage(st.lead.link) }
                                    ?.let { st.copy(lead = st.lead.copy(imageUrl = it)) } ?: st
                            }
                        }.awaitAll()
                        _brief.update { it + (section to SectionState(loading = false, stories = withImages)) }
                    }
                }
            }
            jobs.forEach { it.join() }
            _updatedAt.value = System.currentTimeMillis()
            _refreshing.value = false
        }
    }

    // ---------------- Lecture ----------------

    fun openStory(story: Story) {
        val article = story.lead
        readerJob?.cancel()
        _reader.value = ReaderState(article = article, outlets = story.outlets, loading = true)
        readerJob = viewModelScope.launch {
            runCatching { repo.fetchReader(article) }
                .onSuccess { c -> _reader.update { it.copy(loading = false, content = c) } }
                .onFailure { e ->
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
                _reader.update {
                    if (it.article?.id == article.id) it.copy(translating = false, translated = tr, showTranslated = true) else it
                }
            }.onFailure { e ->
                _reader.update { it.copy(translating = false, translateError = "Traduction impossible : ${e.message ?: "erreur"}") }
            }
        }
    }

    // ---------------- Réglages ----------------

    fun updateSettings(transform: (Settings) -> Settings) {
        val before = _settings.value
        _settings.update(transform)
        settingsStore.save(_settings.value)
        // Une rubrique réactivée doit être chargée.
        if ((before.hiddenSections - _settings.value.hiddenSections).isNotEmpty()) {
            refreshJob?.cancel()
            refresh()
        }
    }
}
