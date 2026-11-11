package fr.mesactus.data

enum class Country(
    val label: String,
    val flag: String,
    val city: String,
    val lat: Double,
    val lon: Double,
    val timeZone: String,
    val defaultLang: String,
) {
    FRANCE("France", "🇫🇷", "Orléans", 47.9029, 1.9093, "Europe/Paris", "fr"),
    CANADA("Canada", "🇨🇦", "Montréal", 45.5019, -73.5674, "America/Toronto", "fr"),
    SPAIN("Espagne", "🇪🇸", "Grenade", 37.1773, -3.5986, "Europe/Madrid", "es"),
}

enum class Level { NATIONAL, REGION, CITY }

enum class Zone(val country: Country, val level: Level, val label: String) {
    FR_NAT(Country.FRANCE, Level.NATIONAL, "National"),
    FR_REG(Country.FRANCE, Level.REGION, "Centre-Val de Loire"),
    FR_CITY(Country.FRANCE, Level.CITY, "Orléans & métropole"),
    CA_NAT(Country.CANADA, Level.NATIONAL, "National"),
    CA_REG(Country.CANADA, Level.REGION, "Québec"),
    CA_CITY(Country.CANADA, Level.CITY, "Île de Montréal"),
    ES_NAT(Country.SPAIN, Level.NATIONAL, "National"),
    ES_REG(Country.SPAIN, Level.REGION, "Andalousie"),
    ES_CITY(Country.SPAIN, Level.CITY, "Grenade & agglo");

    val fullLabel: String get() = if (level == Level.NATIONAL) "${country.label} – National" else label

    companion object {
        fun of(country: Country): List<Zone> = entries.filter { it.country == country }
    }
}

data class Source(
    val id: String,
    val name: String,
    val url: String,
    val zone: Zone,
    val lang: String,
    val enabled: Boolean = true,
    val custom: Boolean = false,
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
    val categories: Set<Category>,
)

enum class BlockType { PARAGRAPH, HEADING }

data class ReaderBlock(val type: BlockType, val text: String)

data class ReaderContent(
    val title: String,
    val imageUrl: String?,
    val blocks: List<ReaderBlock>,
    /** true quand le texte complet n'a pas pu être extrait (on n'a que le résumé RSS). */
    val partial: Boolean = false,
)

data class Favorite(val article: Article, val content: ReaderContent?, val savedAt: Long)

enum class ThemeMode(val label: String) { SYSTEM("Selon le système"), LIGHT("Clair"), DARK("Sombre") }

enum class ListStyle(val label: String) { CARDS("Cartes avec image"), COMPACT("Liste compacte"), TITLES("Titres seuls") }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val listStyle: ListStyle = ListStyle.COMPACT,
    val hiddenZones: Set<Zone> = emptySet(),
)
