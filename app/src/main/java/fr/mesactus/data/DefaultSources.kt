package fr.mesactus.data

import java.net.URLEncoder

object DefaultSources {

    private fun googleSearch(query: String, hl: String, gl: String, ceid: String): String =
        "https://news.google.com/rss/search?q=" + URLEncoder.encode("($query) when:2d", "UTF-8") +
            "&hl=$hl&gl=$gl&ceid=$ceid"

    private val orleansAgglo = listOf(
        "Orléans", "Olivet", "Saran", "\"Fleury-les-Aubrais\"", "\"Saint-Jean-de-Braye\"",
        "\"Saint-Jean-de-la-Ruelle\"", "\"La Chapelle-Saint-Mesmin\"", "\"Saint-Jean-le-Blanc\"", "Loiret",
    ).joinToString(" OR ")

    private val granadaAgglo = listOf(
        "Granada", "Armilla", "Maracena", "Ogíjares", "\"La Zubia\"", "Atarfe", "Albolote",
        "\"Huétor Vega\"", "\"Las Gabias\"", "Peligros",
    ).joinToString(" OR ")

    val all: List<Source> = listOf(
        // ---------- France ----------
        Source("gn_fr", "Google Actus – À la une", "https://news.google.com/rss?hl=fr&gl=FR&ceid=FR:fr", Zone.FR_NAT, "fr"),
        Source("lemonde", "Le Monde", "https://www.lemonde.fr/rss/une.xml", Zone.FR_NAT, "fr"),
        Source("franceinfo", "Franceinfo", "https://www.francetvinfo.fr/titres.rss", Zone.FR_NAT, "fr"),
        Source("liberation", "Libération", "https://www.liberation.fr/arc/outboundfeeds/rss-all/?outputType=xml", Zone.FR_NAT, "fr"),
        Source("20minutes", "20 Minutes", "https://www.20minutes.fr/feeds/rss-une.xml", Zone.FR_NAT, "fr"),

        Source("f3cvl", "France 3 Centre-Val de Loire", "https://france3-regions.francetvinfo.fr/centre-val-de-loire/rss", Zone.FR_REG, "fr"),
        Source("f3loiret", "France 3 Loiret", "https://france3-regions.francetvinfo.fr/centre-val-de-loire/loiret/rss", Zone.FR_CITY, "fr"),
        Source("magcentre", "Magcentre", "https://www.magcentre.fr/feed/", Zone.FR_CITY, "fr"),
        Source("gn_orleans", "Google Actus – Orléans", googleSearch(orleansAgglo, "fr", "FR", "FR:fr"), Zone.FR_CITY, "fr"),

        // ---------- Canada ----------
        Source("gn_ca", "Google Actus – À la une", "https://news.google.com/rss?hl=fr-CA&gl=CA&ceid=CA:fr", Zone.CA_NAT, "fr"),
        Source("rc_une", "Radio-Canada", "https://ici.radio-canada.ca/rss/4159", Zone.CA_NAT, "fr"),
        Source("lapresse", "La Presse", "https://www.lapresse.ca/actualites/rss", Zone.CA_NAT, "fr"),
        Source("ledevoir", "Le Devoir", "https://www.ledevoir.com/rss/manchettes.xml", Zone.CA_NAT, "fr"),
        Source("ledevoir_qc", "Le Devoir", "https://www.ledevoir.com/rss/section/politique/quebec.xml", Zone.CA_REG, "fr"),
        Source("lapresse_pol", "La Presse", "https://www.lapresse.ca/actualites/politique/rss", Zone.CA_REG, "fr"),

        Source("rc_mtl", "Radio-Canada", "https://ici.radio-canada.ca/rss/4201", Zone.CA_CITY, "fr"),
        Source("lapresse_mtl", "La Presse", "https://www.lapresse.ca/actualites/grand-montreal/rss", Zone.CA_CITY, "fr"),
        Source("metro", "Journal Métro", "https://journalmetro.com/feed/", Zone.CA_CITY, "fr"),
        Source("gn_mtl", "Google Actus – Montréal", googleSearch("Montréal", "fr-CA", "CA", "CA:fr"), Zone.CA_CITY, "fr"),

        // ---------- Espagne ----------
        Source("gn_es", "Google Actus – À la une", "https://news.google.com/rss?hl=es&gl=ES&ceid=ES:es", Zone.ES_NAT, "es"),
        Source("elpais", "El País", "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/portada", Zone.ES_NAT, "es"),
        Source("elmundo", "El Mundo", "https://e00-elmundo.uecdn.es/elmundo/rss/portada.xml", Zone.ES_NAT, "es"),
        Source("rtve", "RTVE", "https://api2.rtve.es/rss/temas_noticias.xml", Zone.ES_NAT, "es"),
        Source("20minutos", "20minutos", "https://www.20minutos.es/rss/", Zone.ES_NAT, "es"),

        Source("ep_and", "Europa Press", "https://www.europapress.es/rss/rss.aspx?ch=00279", Zone.ES_REG, "es"),
        Source("ideal", "Ideal", "https://www.ideal.es/rss/2.0/?section=granada", Zone.ES_CITY, "es"),
        Source("indep_gr", "El Independiente de Granada", "https://www.elindependientedegranada.es/rss.xml", Zone.ES_CITY, "es"),
        Source("ahora_gr", "Ahora Granada", "https://www.ahoragranada.com/feed/", Zone.ES_CITY, "es"),
        Source("gn_gr", "Google Actus – Grenade", googleSearch(granadaAgglo, "es", "ES", "ES:es"), Zone.ES_CITY, "es"),
    )
}
