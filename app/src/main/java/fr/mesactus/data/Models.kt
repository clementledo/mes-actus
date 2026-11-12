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

/** Les rubriques de la page « L'essentiel », dans l'ordre d'affichage. */
enum class Section(val title: String, val flag: String, val zones: List<Zone>, val count: Int) {
    FRANCE("France", "🇫🇷", listOf(Zone.FR_NAT), 3),
    ORLEANS("Orléans & Loiret", "📍", listOf(Zone.FR_CITY, Zone.FR_REG), 2),
    CANADA("Canada & Québec", "🇨🇦", listOf(Zone.CA_NAT, Zone.CA_REG), 3),
    MONTREAL("Montréal", "📍", listOf(Zone.CA_CITY), 2),
    ESPAGNE("Espagne", "🇪🇸", listOf(Zone.ES_NAT), 3),
    GRENADE("Grenade & Andalousie", "📍", listOf(Zone.ES_CITY, Zone.ES_REG), 2),
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
