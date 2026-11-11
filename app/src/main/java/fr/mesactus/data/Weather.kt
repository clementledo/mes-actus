package fr.mesactus.data

data class Weather(
    val temperature: Double,
    val code: Int,
    val wind: Double,
    val max: Double,
    val min: Double,
    val isDay: Boolean,
)

/** Codes météo WMO utilisés par Open-Meteo. */
fun weatherLabel(code: Int, isDay: Boolean = true): Pair<String, String> = when (code) {
    0 -> (if (isDay) "☀️" else "🌙") to "Ciel dégagé"
    1 -> (if (isDay) "🌤️" else "🌙") to "Plutôt dégagé"
    2 -> "⛅" to "Partiellement nuageux"
    3 -> "☁️" to "Couvert"
    45, 48 -> "🌫️" to "Brouillard"
    51, 53, 55 -> "🌦️" to "Bruine"
    56, 57 -> "🌧️" to "Bruine verglaçante"
    61, 63 -> "🌧️" to "Pluie"
    65 -> "🌧️" to "Forte pluie"
    66, 67 -> "🌧️" to "Pluie verglaçante"
    71, 73 -> "🌨️" to "Neige"
    75 -> "❄️" to "Forte neige"
    77 -> "🌨️" to "Grains de neige"
    80, 81 -> "🌦️" to "Averses"
    82 -> "⛈️" to "Fortes averses"
    85, 86 -> "🌨️" to "Averses de neige"
    95 -> "⛈️" to "Orage"
    96, 99 -> "⛈️" to "Orage avec grêle"
    else -> "🌡️" to "—"
}
