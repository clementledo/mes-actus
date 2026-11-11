package fr.mesactus.data

import android.util.Xml
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Lit les flux RSS 2.0 et Atom. */
object RssParser {

    private class Raw {
        var title = ""
        var link = ""
        var guid = ""
        var description = ""
        var content = ""
        var date = ""
        var image: String? = null
        var sourceName: String? = null
        val categories = mutableListOf<String>()
    }

    fun parse(input: InputStream, source: Source): List<Article> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        // Mode tolérant : certains flux contiennent des entités HTML (&nbsp;…) non déclarées.
        runCatching { parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true) }
        parser.setInput(input, null)

        val items = mutableListOf<Raw>()
        try {
            loop(parser, items)
        } catch (e: Exception) {
            // Flux mal formé : on garde ce qui a déjà été lu, sinon on remonte l'erreur.
            if (items.isEmpty()) throw e
        }
        return items.mapIndexedNotNull { i, raw -> toArticle(raw, source, i) }
    }

    private fun loop(parser: XmlPullParser, items: MutableList<Raw>) {
        var current: Raw? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val name = parser.name.lowercase(Locale.ROOT)
                if (name == "item" || name == "entry") {
                    current = Raw()
                } else if (current != null) {
                    readField(parser, name, current)
                }
            } else if (event == XmlPullParser.END_TAG) {
                val name = parser.name.lowercase(Locale.ROOT)
                if ((name == "item" || name == "entry") && current != null) {
                    items += current
                    current = null
                }
            }
            event = parser.next()
        }
    }

    private fun readField(p: XmlPullParser, name: String, r: Raw) {
        when (name) {
            "title" -> r.title = readText(p)
            "link" -> {
                val href = p.getAttributeValue(null, "href")
                val rel = p.getAttributeValue(null, "rel")
                if (href != null) {
                    if (rel == null || rel == "alternate") r.link = href
                } else {
                    val t = readText(p)
                    if (t.isNotBlank()) r.link = t
                }
            }
            "guid", "id" -> r.guid = readText(p)
            "description", "summary" -> r.description = readText(p)
            "content:encoded", "content" -> r.content = readText(p)
            "pubdate", "published", "updated", "dc:date" -> if (r.date.isBlank()) r.date = readText(p)
            "category" -> {
                val term = p.getAttributeValue(null, "term")
                val t = term ?: readText(p)
                if (t.isNotBlank()) r.categories += t
            }
            "source" -> r.sourceName = readText(p).ifBlank { null }
            "enclosure" -> {
                val type = p.getAttributeValue(null, "type") ?: ""
                val url = p.getAttributeValue(null, "url")
                if (url != null && (type.startsWith("image") || type.isEmpty()) && r.image == null) r.image = url
            }
            "media:content", "media:thumbnail" -> {
                val url = p.getAttributeValue(null, "url")
                val medium = p.getAttributeValue(null, "medium") ?: "image"
                val type = p.getAttributeValue(null, "type") ?: "image"
                if (url != null && r.image == null && medium == "image" && type.startsWith("image")) r.image = url
            }
        }
    }

    /** Texte d'un élément, y compris s'il contient du HTML brut non échappé. */
    private fun readText(p: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(p.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return sb.toString().trim()
            }
        }
        return sb.toString().trim()
    }

    private fun toArticle(r: Raw, source: Source, rank: Int): Article? {
        val link = r.link.ifBlank { r.guid.takeIf { it.startsWith("http") } ?: "" }.trim()
        if (link.isEmpty()) return null
        var title = Jsoup.parse(r.title).text().trim()
        if (title.isEmpty()) return null

        var sourceName = source.name
        val viaGoogle = link.contains("news.google.")
        if (viaGoogle) {
            // Titres Google Actualités : « Titre - Média »
            val media = r.sourceName ?: title.substringAfterLast(" - ", "").ifBlank { null }
            if (media != null) {
                title = title.removeSuffix(" - $media").trim()
                sourceName = media
            }
        }

        val html = r.description.ifBlank { r.content }
        val doc = Jsoup.parse(html)
        val image = r.image ?: doc.selectFirst("img[src]")?.attr("src")?.takeIf { it.startsWith("http") }
        var description = doc.text().trim()
        if (viaGoogle) description = "" // le résumé Google ne fait que répéter le titre
        if (description.length > 400) description = description.take(400).substringBeforeLast(' ') + "…"

        return Article(
            id = link,
            title = title,
            link = link,
            description = description,
            imageUrl = image,
            published = parseDate(r.date),
            sourceName = sourceName,
            zone = source.zone,
            lang = source.lang,
            rank = rank,
        )
    }

    private val legacyFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss",
    )

    fun parseDate(s: String): Long {
        val t = s.trim()
        if (t.isEmpty()) return 0L
        runCatching { return ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
        runCatching { return OffsetDateTime.parse(t).toInstant().toEpochMilli() }
        runCatching { return ZonedDateTime.parse(t).toInstant().toEpochMilli() }
        for (f in legacyFormats) {
            runCatching {
                val d = SimpleDateFormat(f, Locale.US).parse(t)
                if (d != null) return d.time
            }
        }
        return 0L
    }
}
