package fr.mesactus.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Une version publiée sur GitHub, plus récente que celle installée. */
data class AppUpdate(val version: String, val apkUrl: String, val notes: String)

/**
 * Mise à jour dans l'app : lit la dernière release GitHub du dépôt, et si elle est plus récente,
 * télécharge l'APK puis ouvre l'installateur d'Android (l'APK est signé avec la même clé, il
 * s'installe par-dessus l'ancienne version sans rien perdre).
 */
object Updater {
    private const val LATEST = "https://api.github.com/repos/clementledo/mes-actus/releases/latest"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Compare « 1.10 » et « 1.9 » chiffre par chiffre. */
    fun isNewer(remote: String, local: String): Boolean {
        val r = remote.trimStart('v', 'V').split('.').map { it.toIntOrNull() ?: 0 }
        val l = local.trimStart('v', 'V').split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun check(currentVersion: String): AppUpdate? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(LATEST).header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: return@withContext null)
            val tag = json.optString("tag_name")
            if (tag.isBlank() || !isNewer(tag, currentVersion)) return@withContext null
            val assets = json.optJSONArray("assets") ?: return@withContext null
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") } ?: return@withContext null
            AppUpdate(tag.trimStart('v', 'V'), apk.getString("browser_download_url"), json.optString("body"))
        }
    }

    /** Télécharge l'APK dans le cache de l'app, en signalant l'avancement (0..1). */
    suspend fun download(context: Context, update: AppUpdate, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "MesActus-${update.version}.apk")
            http.newCall(Request.Builder().url(update.apkUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: throw IOException("Réponse vide")
                val total = body.contentLength().takeIf { it > 0 }
                var read = 0L
                body.byteStream().use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            if (total != null) onProgress(read.toFloat() / total)
                        }
                    }
                }
            }
            file
        }

    /** Android demande une fois l'autorisation « installer des applis inconnues » pour Mes Actus. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
