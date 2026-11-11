package fr.mesactus.data

import java.text.Normalizer

/**
 * Thèmes. Le classement se fait par mots-clés (français, espagnol, anglais) sur le titre,
 * le résumé et les catégories RSS. Un mot-clé est un début de mot (« politi » trouve
 * « politique », « política »…) ; un espace final impose le mot exact (« pp »).
 */
enum class Category(val label: String, keywords: List<String>) {
    POLITIQUE(
        "Politique", listOf(
            "politi", "gouvern", "ministr", "election", "electoral", "parlement", "assemblee nationale",
            "senat", "depute", "macron", "legault", "carney", "poilievre", "sanchez", "feijoo", "gobierno",
            "eleccion", "partido popular", "partido socialista", "congreso", "diputad", "alcald", "maire ",
            "mairie", "conseil municipal", "ayuntamiento", "junta de andalucia", "moreno bonilla", "psoe",
            "vox ", "pp ", "caq ", "referendum", "referendo", "parliament", "prime minister",
        )
    ),
    ECONOMIE(
        "Économie", listOf(
            "econom", "entreprise", "emploi", "chomage", "inflation", "bourse", "banque", "impot", "fiscal",
            "budget", "salaire", "commerce", "industri", "usine", "pib ", "croissance", "empresa", "empleo",
            "paro ", "desempleo", "banco", "impuesto", "sueldo", "salari", "comercio", "arancel", "tarif",
            "crecimiento", "immobilier", "business", "investiss", "inversion", "exportat", "faillite",
        )
    ),
    SPORT(
        "Sport", listOf(
            "sport", "deport", "football", "futbol", "foot ", "hockey", "canadien de montreal", "ligue",
            "liga ", "laliga", "rugby", "tennis", "tenis", "basket", "baloncesto", "cyclis", "ciclis",
            "joueur", "jugador", "entraineur", "entrenador", "olympi", "champion", "coupe du monde", "copa ",
            "nhl", "lnh", "alouettes", "real madrid", "granada cf", "stade", "estadio", "athlet", "atlet",
            "formule 1", "formula 1", "f1 ", "motogp", "golf", "boxe", "natation", "medaill", "medalla",
            "mondial", "mundial", "match", "partido de", "gol ", "victoire", "derrota", "defaite",
        )
    ),
    CULTURE(
        "Culture", listOf(
            "cultur", "musee", "museo", "exposition", "exposicion", "festival", "cinema", "cine ", "film",
            "pelicul", "concert", "concierto", "musique", "musica", "theatre", "teatro", "livre", "libro",
            "litter", "literat", "artist", "chanteu", "cantante", "serie", "televis", "patrimoine",
            "patrimonio", "alhambra", "spectacle", "espectaculo", "danse", "danza", "flamenco", "opera",
            "roman ", "novela", "album", "ecrivain", "escritor",
        )
    ),
    FAITS_DIVERS(
        "Faits divers", listOf(
            "faits divers", "suceso", "accident", "incendie", "incendio", "meurtre", "homicid", "asesin",
            "tue ", "mort ", "muerto", "muere", "fallec", "deces", "decede", "police", "policia", "gendarm",
            "suspect", "sospech", "vol ", "robo", "agress", "arrest", "detenid", "detenu", "interpel",
            "enquete", "investigacion", "tribunal", "juicio", "proces ", "condamn", "conden", "prison",
            "carcel", "drogue", "droga", "collision", "choque", "disparu", "desaparec", "violence",
            "violencia", "crime", "criminal", "fusillade", "tiroteo", "coup de couteau", "apunal",
            "spvm", "guardia civil",
        )
    ),
    SOCIETE(
        "Société", listOf(
            "societe", "sociedad", "logement", "vivienda", "alquiler", "loyer", "education", "educacion",
            "ecole", "escuela", "colegio", "universit", "greve", "huelga", "manifest", "syndic", "sindicat",
            "immigr", "inmigr", "retraite", "jubilac", "pension", "famille", "familia", "transport",
            "metro ", "tramway", "tranvia", "autobus", "stm ", "trafic", "trafico", "circulation",
            "carretera", "femme", "mujer", "egalite", "igualdad", "religion", "eglise", "iglesia", "social",
            "eleves", "alumnos", "enseignant", "profesor",
        )
    ),
    SCIENCES(
        "Sciences & tech", listOf(
            "scien", "cienc", "recherche", "investigador", "technolog", "tecnolog", "numerique", "digital",
            "internet", "intelligence artificielle", "inteligencia artificial", "ia ", "espace ", "espacial",
            "nasa", "satellit", "informati", "smartphone", "startup", "cyber", "robot", "astronom",
            "physique", "fisica", "chimie", "quimica", "decouverte", "descubr",
        )
    ),
    SANTE(
        "Santé", listOf(
            "sante", "salud", "hopital", "hospital", "medec", "medic", "malad", "enfermedad", "virus",
            "vaccin", "vacun", "cancer", "epidem", "grippe", "gripe", "covid", "soins", "urgences",
            "urgencias", "sanit", "chu ", "patient", "pacient", "infirmi", "enfermer", "psycholog",
            "sante mentale", "salud mental",
        )
    ),
    ENVIRONNEMENT(
        "Environnement & météo", listOf(
            "environ", "medio ambiente", "climat", "clima", "meteo", "tempete", "tormenta", "borrasca",
            "orage", "pluie", "lluvia", "neige", "nieve", "canicule", "ola de calor", "secheresse",
            "sequia", "inondation", "inundac", "dana ", "pollution", "contamin", "ecolog", "energie",
            "energia", "renouvel", "renovable", "nucleaire", "nuclear", "biodivers", "foret", "bosque",
            "agricult", "vigilance", "alerte meteo", "aviso amarillo", "aviso naranja", "aviso rojo",
            "carbone", "carbono", "recyclage", "reciclaje", "verglas", "tornade", "weather", "climate",
        )
    );

    // Un espace final dans le mot-clé (« pp ») est conservé pour imposer le mot exact.
    private val patterns: List<String> = keywords.map { kw ->
        val core = TextNormalizer.normalize(kw).trim()
        if (kw.endsWith(" ")) " $core " else " $core"
    }

    fun matches(normalizedText: String): Boolean = patterns.any { normalizedText.contains(it) }

    companion object {
        fun classify(title: String, description: String, rssCategories: List<String>): Set<Category> {
            val text = TextNormalizer.normalize(title + " " + description + " " + rssCategories.joinToString(" "))
            return entries.filter { it.matches(text) }.toSet()
        }
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
