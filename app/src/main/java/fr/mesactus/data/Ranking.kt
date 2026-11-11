package fr.mesactus.data

import java.text.Normalizer
import kotlin.math.exp
import kotlin.math.min

/**
 * Repère les plus grosses infos : les articles sur un même sujet sont regroupés, puis chaque
 * sujet est noté selon le nombre de médias qui en parlent, sa place en tête des unes et sa fraîcheur.
 */
object Ranking {

    private const val MAX_AGE_MS = 36L * 3_600_000

    private val stopWords = setOf(
        // français
        "pour", "dans", "avec", "sans", "sous", "sur", "plus", "moins", "tout", "tous", "toute", "toutes", "cette",
        "comme", "mais", "leur", "leurs", "elle", "elles", "nous", "vous", "sont", "etre", "avoir", "fait", "faire",
        "apres", "avant", "depuis", "entre", "encore", "deux", "trois", "selon", "contre", "vers", "chez", "quand",
        "direct", "video", "photos", "ceux", "celle", "dont", "aussi", "alors", "notre", "votre", "veut", "peut",
        // espagnol
        "para", "sobre", "como", "pero", "desde", "hasta", "este", "esta", "estos", "estas", "entre", "tras", "todo",
        "todos", "segun", "contra", "tiene", "hace", "durante", "cuando", "donde", "porque", "unos", "unas", "ante",
        "directo", "ultima", "hora", "minuto", "otro", "otra", "sido", "puede", "sera",
        // anglais
        "with", "from", "that", "this", "after", "into", "over", "about", "what", "will", "says", "have", "their",
    )

    fun keywords(title: String): Set<String> =
        TextNormalizer.normalize(title).trim().split(" ")
            .filter { it.length >= 4 && it !in stopWords && !it.all(Char::isDigit) }
            .map { it.take(7) } // racine grossière : « grève »/« grèves », « huelga »/« huelgas »
            .toSet()

    fun similar(a: Set<String>, b: Set<String>): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        val common = a.count { it in b }
        return common >= 2 && common.toDouble() / min(a.size, b.size) >= 0.34
    }

    fun topStories(articles: List<Article>, count: Int, now: Long = System.currentTimeMillis()): List<Story> {
        val recent = articles.filter { it.published == 0L || now - it.published in -3_600_000..MAX_AGE_MS }
        val clusters = mutableListOf<MutableList<Pair<Article, Set<String>>>>()
        for (a in recent.sortedBy { it.rank }) {
            val kw = keywords(a.title)
            if (kw.isEmpty()) continue
            val cluster = clusters.firstOrNull { c -> c.any { similar(kw, it.second) } }
            if (cluster != null) cluster += a to kw else clusters += mutableListOf(a to kw)
        }

        return clusters.map { c ->
            val items = c.map { it.first }
            val outlets = items.map { it.sourceName }.distinct()
            // Un sujet en tête de plusieurs unes compte beaucoup ; en 15e position, presque rien.
            val prominence = items.sumOf { 1.0 / (1 + it.rank) }
            val newest = items.maxOf { it.published }
            val ageHours = if (newest == 0L) 12.0 else (now - newest).coerceAtLeast(0) / 3_600_000.0
            val score = (outlets.size + 3 * prominence) * exp(-ageHours / 24)
            // Article mis en avant : un lien direct (lisible dans l'app), avec image, le mieux placé.
            val lead = items.sortedWith(
                compareBy<Article>({ it.link.contains("news.google.") }, { it.imageUrl == null }, { it.rank })
            ).first()
            Story(lead, outlets, score)
        }.sortedByDescending { it.score }.let { pickDiverse(it, count) }
    }

    /** Évite d'afficher trois fois le même événement (« huelga general », « huelga general en directo »…). */
    private fun pickDiverse(sorted: List<Story>, count: Int): List<Story> {
        val picked = mutableListOf<Pair<Story, Set<String>>>()
        for (s in sorted) {
            if (picked.size == count) break
            val kw = keywords(s.lead.title)
            if (picked.none { (_, other) -> kw.count { it in other } >= 2 }) picked += s to kw
        }
        return picked.map { it.first }
    }
}

object TextNormalizer {
    private val nonAlnum = Regex("[^a-z0-9]+")
    private val accents = Regex("\\p{Mn}+")

    /** Minuscules, sans accents, ponctuation remplacée par des espaces, encadré d'espaces. */
    fun normalize(text: String): String {
        val stripped = accents.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")
        val spaced = nonAlnum.replace(stripped, " ").trim()
        return if (spaced.isEmpty()) " " else " $spaced "
    }
}
