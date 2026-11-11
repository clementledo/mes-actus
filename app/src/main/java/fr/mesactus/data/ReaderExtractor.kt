package fr.mesactus.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Mode lecture : extrait le texte principal d'une page d'article. */
object ReaderExtractor {

    private const val NOISE =
        "script, style, noscript, nav, header, footer, aside, form, iframe, button, svg, select, " +
            "[aria-hidden=true], [role=navigation], [role=complementary], " +
            ".share, .sharing, .social, .related, .newsletter, .comments, .comment, .advert, .ad, " +
            ".ads, .pub, .paywall, .breadcrumb, .tags, .author-bio, .read-more, .outbrain, .taboola"

    private val junk = listOf(
        "cookie", "abonnez-vous", "suscríbete", "inscrivez-vous", "newsletter", "javascript",
        "lire aussi", "lea también", "à lire aussi", "publicité", "publicidad",
    )

    fun extract(html: String, baseUrl: String, fallback: Article): ReaderContent {
        val doc = Jsoup.parse(html, baseUrl)
        val ogImage = doc.selectFirst("meta[property=og:image]")?.absUrl("content")?.ifBlank { null }
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?.ifBlank { null }

        doc.select(NOISE).remove()

        val root = doc.select("article").maxByOrNull { textLength(it) }
            ?.takeIf { it.select("p").size >= 3 }
            ?: bestCandidate(doc)

        val seen = HashSet<String>()
        val blocks = mutableListOf<ReaderBlock>()
        for (el in root.select("p, h2, h3")) {
            val text = el.text().trim()
            if (text.isEmpty() || !seen.add(text)) continue
            val isHeading = el.tagName() != "p"
            if (!isHeading && text.length < 30) continue
            val lower = text.lowercase()
            if (text.length < 160 && junk.any { lower.contains(it) }) continue
            blocks += ReaderBlock(if (isHeading) BlockType.HEADING else BlockType.PARAGRAPH, text)
        }
        // Un intertitre en dernier, sans paragraphe après, ne sert à rien.
        while (blocks.isNotEmpty() && blocks.last().type == BlockType.HEADING) blocks.removeAt(blocks.lastIndex)

        val paragraphs = blocks.count { it.type == BlockType.PARAGRAPH }
        return if (paragraphs < 2) {
            fallbackContent(fallback, ogImage)
        } else {
            ReaderContent(
                title = fallback.title.ifBlank { ogTitle ?: "" },
                imageUrl = ogImage ?: fallback.imageUrl,
                blocks = blocks,
            )
        }
    }

    fun fallbackContent(article: Article, image: String? = null): ReaderContent = ReaderContent(
        title = article.title,
        imageUrl = image ?: article.imageUrl,
        blocks = if (article.description.isNotBlank()) listOf(ReaderBlock(BlockType.PARAGRAPH, article.description)) else emptyList(),
        partial = true,
    )

    private fun textLength(e: Element): Int = e.select("p").sumOf { it.text().length }

    private fun bestCandidate(doc: Document): Element {
        var best: Element = doc.body()
        var bestScore = 0
        for (el in doc.select("main, section, div")) {
            val score = el.children().filter { it.tagName() == "p" }.sumOf { it.text().length }
            if (score > bestScore) {
                bestScore = score
                best = el
            }
        }
        return best
    }
}
