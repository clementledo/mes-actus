package fr.mesactus.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Enregistrement local (fichiers JSON) des sources et des favoris, et des réglages. */
class SourceStore(context: Context) {
    private val file = File(context.filesDir, "sources.json")

    fun load(): List<Source> {
        if (!file.exists()) return DefaultSources.all
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Source(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    url = o.getString("url"),
                    zone = Zone.valueOf(o.getString("zone")),
                    lang = o.optString("lang", "fr"),
                    enabled = o.optBoolean("enabled", true),
                    custom = o.optBoolean("custom", false),
                )
            }
        }.getOrDefault(DefaultSources.all)
    }

    fun save(sources: List<Source>) {
        val arr = JSONArray()
        sources.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id).put("name", s.name).put("url", s.url).put("zone", s.zone.name)
                    .put("lang", s.lang).put("enabled", s.enabled).put("custom", s.custom)
            )
        }
        file.writeText(arr.toString())
    }

    fun reset() {
        file.delete()
    }
}

class FavoriteStore(context: Context) {
    private val file = File(context.filesDir, "favorites.json")

    fun load(): List<Favorite> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i -> runCatching { favoriteFrom(arr.getJSONObject(i)) }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    fun save(favorites: List<Favorite>) {
        val arr = JSONArray()
        favorites.forEach { arr.put(favoriteTo(it)) }
        file.writeText(arr.toString())
    }

    private fun favoriteTo(f: Favorite): JSONObject = JSONObject()
        .put("article", articleTo(f.article))
        .put("savedAt", f.savedAt)
        .apply { f.content?.let { put("content", contentTo(it)) } }

    private fun favoriteFrom(o: JSONObject) = Favorite(
        article = articleFrom(o.getJSONObject("article")),
        content = o.optJSONObject("content")?.let { contentFrom(it) },
        savedAt = o.optLong("savedAt"),
    )

    private fun articleTo(a: Article): JSONObject = JSONObject()
        .put("id", a.id).put("title", a.title).put("link", a.link).put("description", a.description)
        .put("imageUrl", a.imageUrl ?: "").put("published", a.published).put("sourceName", a.sourceName)
        .put("zone", a.zone.name).put("lang", a.lang)
        .put("categories", JSONArray(a.categories.map { it.name }))

    private fun articleFrom(o: JSONObject): Article {
        val cats = o.optJSONArray("categories")
        return Article(
            id = o.getString("id"),
            title = o.getString("title"),
            link = o.getString("link"),
            description = o.optString("description"),
            imageUrl = o.optString("imageUrl").ifBlank { null },
            published = o.optLong("published"),
            sourceName = o.optString("sourceName"),
            zone = Zone.valueOf(o.getString("zone")),
            lang = o.optString("lang", "fr"),
            categories = if (cats == null) emptySet() else
                (0 until cats.length()).mapNotNull { runCatching { Category.valueOf(cats.getString(it)) }.getOrNull() }.toSet(),
        )
    }

    private fun contentTo(c: ReaderContent): JSONObject = JSONObject()
        .put("title", c.title).put("imageUrl", c.imageUrl ?: "").put("partial", c.partial)
        .put("blocks", JSONArray().apply {
            c.blocks.forEach { put(JSONObject().put("type", it.type.name).put("text", it.text)) }
        })

    private fun contentFrom(o: JSONObject): ReaderContent {
        val arr = o.getJSONArray("blocks")
        return ReaderContent(
            title = o.optString("title"),
            imageUrl = o.optString("imageUrl").ifBlank { null },
            partial = o.optBoolean("partial"),
            blocks = (0 until arr.length()).map {
                val b = arr.getJSONObject(it)
                ReaderBlock(BlockType.valueOf(b.getString("type")), b.getString("text"))
            },
        )
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): Settings = Settings(
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }.getOrDefault(ThemeMode.SYSTEM),
        listStyle = runCatching { ListStyle.valueOf(prefs.getString("listStyle", null)!!) }.getOrDefault(ListStyle.COMPACT),
        hiddenZones = prefs.getStringSet("hiddenZones", emptySet())!!
            .mapNotNull { runCatching { Zone.valueOf(it) }.getOrNull() }.toSet(),
    )

    fun save(s: Settings) {
        prefs.edit()
            .putString("theme", s.theme.name)
            .putString("listStyle", s.listStyle.name)
            .putStringSet("hiddenZones", s.hiddenZones.map { it.name }.toSet())
            .apply()
    }
}
