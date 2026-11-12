package fr.mesactus.data

enum class Zone(val label: String) {
    FR_NAT("France"),
    FR_REG("Centre-Val de Loire"),
    FR_CITY("Orléans"),
    CA_NAT("Canada"),
    CA_REG("Québec"),
    CA_CITY("Montréal"),
    ES_NAT("Espagne"),
    ES_REG("Andalousie"),
    ES_CITY("Grenade"),
}

/** Les trois onglets de la page, un par pays. */
enum class Country(val title: String, val flag: String) {
    FRANCE("France", "🇫🇷"),
    CANADA("Canada", "🇨🇦"),
    ESPAGNE("Espagne", "🇪🇸"),
}

/**
 * Les rubriques de la page « L'essentiel », dans l'ordre d'affichage.
 * [places] : pour une rubrique locale, les lieux dont l'article doit parler (forme normalisée,
 * sans accents ; un mot suivi d'une espace doit être entier, sinon c'est un début de mot).
 * Une info nationale reprise par France 3 Centre-Val de Loire n'a donc pas sa place dans « Orléans ».
 */
enum class Section(
    val title: String,
    val flag: String,
    val country: Country,
    val zones: List<Zone>,
    val count: Int,
    val places: List<String>? = null,
    val notPlaces: List<String> = emptyList(),
) {
    FRANCE("France", "🇫🇷", Country.FRANCE, listOf(Zone.FR_NAT), 5),
    ORLEANS(
        "Orléans & Loiret", "📍", Country.FRANCE, listOf(Zone.FR_CITY, Zone.FR_REG), 4,
        places = listOf(
            "orleans", "loiret", "olivet", "saran ", "fleury les aubrais", "saint jean de braye",
            "saint jean de la ruelle", "saint jean le blanc", "la chapelle saint mesmin", "saint denis en val",
            "saint pryve", "ingre ", "checy", "semoy", "montargis", "gien ", "pithiviers", "beaugency",
            "meung sur loire", "jargeau", "sully sur loire", "briare", "chateauneuf sur loire", "orleanais",
            "serge grouard", "centre val de loire", "region centre", "val de loire", "francois bonneau",
            "tours ", "tourangeau", "touraine", "indre", "blois", "blesois", "loir et cher", "vendome",
            "romorantin", "sologne", "bourges", "berruyer", "vierzon", "berry ", "chartres", "chartrain",
            "eure et loir", "dreux", "beauce", "chateauroux", "amboise", "chinon",
        ),
        notPlaces = listOf("nouvelle orleans", "new orleans"),
    ),
    CANADA("Canada & Québec", "🇨🇦", Country.CANADA, listOf(Zone.CA_NAT, Zone.CA_REG), 5),
    MONTREAL(
        "Montréal", "📍", Country.CANADA, listOf(Zone.CA_CITY), 4,
        places = listOf(
            "montreal", "mtl ", "laval ", "longueuil", "brossard", "terrebonne", "repentigny", "rive sud",
            "rive nord", "spvm", "stm ", "rem ", "plateau mont royal", "mont royal", "rosemont", "verdun",
            "villeray", "hochelaga", "ahuntsic", "lachine", "lasalle", "outremont", "anjou ", "cote des neiges",
            "notre dame de grace", "pointe aux trembles", "riviere des prairies", "saint leonard",
            "mercier", "westmount", "dorval", "pierrefonds", "ile des soeurs", "vieux port", "jacques cartier",
            "pont champlain", "turcot", "decarie", "jean drapeau", "martinez ferrada", "mcgill", "concordia",
            "uqam", "chum ", "cusm", "sainte justine",
        ),
    ),
    ESPAGNE("Espagne", "🇪🇸", Country.ESPAGNE, listOf(Zone.ES_NAT), 5),
    GRENADE(
        "Grenade & Andalousie", "📍", Country.ESPAGNE, listOf(Zone.ES_CITY, Zone.ES_REG), 4,
        places = listOf(
            "granad", "andalu", "juanma moreno", "junta ", "sevill", "malag", "cordob", "almeria", "jaen ",
            "huelva", "cadiz", "gaditan", "jerez", "marbella", "algeciras", "betis", "motril", "loja ",
            "guadix", "baza ", "armilla", "maracena", "ogijares", "la zubia", "atarfe", "albolote", "huetor",
            "las gabias", "peligros", "alhambra", "albaicin", "sierra nevada", "alpujarr", "costa tropical",
            "almunecar", "salobrena", "carazo", "ugr ",
        ),
    ),
}

data class Source(
    val id: String,
    val name: String,
    val url: String,
    val zone: Zone,
    val lang: String,
)

data class Article(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val imageUrl: String?,
    val published: Long,
    val sourceName: String,
    val zone: Zone,
    val lang: String,
    /** Position dans le flux d'origine (0 = en tête de la une). */
    val rank: Int,
    /** Titre d'origine quand [title] a été traduit en français. */
    val originalTitle: String? = null,
)

/** Un sujet d'actualité : l'article mis en avant et les médias qui en parlent. */
data class Story(val lead: Article, val outlets: List<String>, val score: Double)

enum class BlockType { PARAGRAPH, HEADING }

data class ReaderBlock(val type: BlockType, val text: String)

data class ReaderContent(
    val title: String,
    val imageUrl: String?,
    val blocks: List<ReaderBlock>,
    /** true quand le texte complet n'a pas pu être extrait (on n'a que le résumé RSS). */
    val partial: Boolean = false,
)

enum class ThemeMode(val label: String) { SYSTEM("Selon le système"), LIGHT("Clair"), DARK("Sombre") }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val hiddenSections: Set<Section> = emptySet(),
)
