package fr.mesactus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ZoneResult(val articles: List<Article>, val failedSources: List<String>)

class Repository {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun request(url: String) = Request.Builder()
        .url(url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0 Mobile Safari/537.36",
        )
        .header("Accept-Language", "fr-FR,fr;q=0.9,es;q=0.8,en;q=0.7")
        .build()

    suspend fun fetchSource(source: Source): List<Article> = withContext(Dispatchers.IO) {
        http.newCall(request(source.url)).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("Réponse vide")
            RssParser.parse(body.byteStream(), source)
        }
    }

    suspend fun fetchZone(sources: List<Source>): ZoneResult = coroutineScope {
        val results = sources.map { s -> async { s to runCatching { fetchSource(s) } } }.awaitAll()
        val failed = results.filter { it.second.isFailure || it.second.getOrNull().isNullOrEmpty() }.map { it.first.name }
        val seenTitles = HashSet<String>()
        val articles = results
            .flatMap { it.second.getOrDefault(emptyList()) }
            .sortedByDescending { it.published }
            .distinctBy { it.id }
            .filter { seenTitles.add(TextNormalizer.normalize(it.title)) }
            .take(200)
        ZoneResult(articles, failed)
    }

    /** Télécharge la page et en extrait le texte (mode lecture). */
    suspend fun fetchReader(article: Article): ReaderContent = withContext(Dispatchers.IO) {
        if (article.link.contains("news.google.")) {
            // Les liens Google Actualités passent par une redirection en JavaScript.
            return@withContext ReaderExtractor.fallbackContent(article)
        }
        http.newCall(request(article.link)).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val html = resp.body?.string() ?: throw IOException("Réponse vide")
            ReaderExtractor.extract(html, resp.request.url.toString(), article)
        }
    }

    suspend fun fetchWeather(country: Country): Weather = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${country.lat}&longitude=${country.lon}" +
            "&current=temperature_2m,weather_code,wind_speed_10m,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        http.newCall(request(url)).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val json = JSONObject(resp.body!!.string())
            val cur = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            Weather(
                temperature = cur.getDouble("temperature_2m"),
                code = cur.getInt("weather_code"),
                wind = cur.getDouble("wind_speed_10m"),
                max = daily.getJSONArray("temperature_2m_max").getDouble(0),
                min = daily.getJSONArray("temperature_2m_min").getDouble(0),
                isDay = cur.optInt("is_day", 1) == 1,
            )
        }
    }
}
