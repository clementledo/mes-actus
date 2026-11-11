package fr.mesactus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingTest {
    private val now = 1_800_000_000_000L

    private fun art(title: String, source: String, rank: Int, hoursAgo: Double = 1.0, link: String = "https://$source/${title.hashCode()}") =
        Article(link, title, link, "", null, now - (hoursAgo * 3_600_000).toLong(), source, Zone.FR_NAT, "fr", rank)

    @Test
    fun similarTitlesAreGrouped() {
        assertTrue(Ranking.similar(Ranking.keywords("Grève générale : l'Espagne paralysée"), Ranking.keywords("L'Espagne paralysée par une grève générale historique")))
        assertFalse(Ranking.similar(Ranking.keywords("Grève générale en Espagne"), Ranking.keywords("Le Canadien bat les Bruins à Boston")))
    }

    @Test
    fun storyCoveredByManyOutletsComesFirst() {
        val articles = listOf(
            art("Un chat sauve son maître à Lyon", "Le Monde", 0),
            art("Attentat de Trappes : le suspect mis en examen", "Franceinfo", 1),
            art("Trappes : le suspect de l'attentat mis en examen pour assassinat", "Le Monde", 2),
            art("Attentat raciste de Trappes, le suspect mis en examen", "Libération", 0),
            art("Météo : un week-end ensoleillé", "20 Minutes", 3),
        )
        val top = Ranking.topStories(articles, 3, now)
        assertTrue(top.first().lead.title.contains("Trappes"))
        assertEquals(3, top.first().outlets.size)
    }

    @Test
    fun sameEventAppearsOnce() {
        val articles = listOf(
            art("Huelga general 11N en directo: miles de personas en las calles", "El País", 0),
            art("Los indicadores para medir el seguimiento de la huelga general", "20minutos", 0),
            art("Acuerdo entre Rusia y Ucrania para un intercambio de prisioneros", "El Mundo", 1),
        )
        val top = Ranking.topStories(articles, 3, now)
        assertEquals(2, top.size)
    }

    @Test
    fun oldNewsIsDropped() {
        val top = Ranking.topStories(listOf(art("Vieille nouvelle importante", "Le Monde", 0, hoursAgo = 72.0)), 3, now)
        assertTrue(top.isEmpty())
    }

    @Test
    fun directLinkIsPreferredOverGoogle() {
        val g = art("Grève générale en Espagne historique", "El País", 0, link = "https://news.google.com/rss/articles/x")
        val d = art("Grève générale historique en Espagne", "RTVE", 4)
        val top = Ranking.topStories(listOf(g, d), 1, now)
        assertEquals("RTVE", top.first().lead.sourceName)
    }
}
