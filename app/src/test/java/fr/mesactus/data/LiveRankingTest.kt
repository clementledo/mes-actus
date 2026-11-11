package fr.mesactus.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.Ignore
import org.junit.Test

/** Test manuel (réseau) : affiche les grands titres que l'app choisirait maintenant. */
class LiveRankingTest {
    private val http = OkHttpClient()

    private fun fetch(s: Source): List<Article> = runCatching {
        val xml = http.newCall(Request.Builder().url(s.url).header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/130.0 Mobile").build())
            .execute().use { it.body!!.string() }
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        doc.select("item").mapIndexed { i, it ->
            var title = Jsoup.parse(it.selectFirst("title")?.text() ?: "").text()
            var src = s.name
            val link = it.selectFirst("link")?.text() ?: ""
            if (link.contains("news.google.")) {
                val m = it.selectFirst("source")?.text()
                if (m != null) { title = title.removeSuffix(" - $m"); src = m }
            }
            val img = it.selectFirst("enclosure, media|content, media|thumbnail")?.attr("url")?.ifBlank { null }
            Article(link, title, link, "", img, RssParser.parseDate(it.selectFirst("pubDate")?.text() ?: ""), src, s.zone, s.lang, i)
        }
    }.getOrDefault(emptyList())

    @Ignore("Test manuel : nécessite le réseau")
    @Test
    fun printBrief() {
        for (section in Section.entries) {
            val arts = DefaultSources.all.filter { it.zone in section.zones }.flatMap { fetch(it) }
            println("===== ${section.title} (${arts.size} articles)")
            Ranking.topStories(arts, section.count).forEachIndexed { i, s ->
                println("${i + 1}. [${s.outlets.size} médias, ${"%.1f".format(s.score)}] ${s.lead.title} — ${s.lead.sourceName}${if (s.lead.imageUrl != null) " 🖼" else ""}")
            }
        }
    }
}
