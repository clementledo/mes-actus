package fr.mesactus.data

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Traduction vers le français sur le téléphone (ML Kit).
 * Le modèle de langue (~30 Mo) est téléchargé une seule fois, puis tout fonctionne hors ligne.
 */
object FrenchTranslator {

    suspend fun translate(texts: List<String>, fromLang: String): List<String> {
        val source = TranslateLanguage.fromLanguageTag(fromLang)
            ?: throw IllegalArgumentException("Langue non prise en charge : $fromLang")
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(TranslateLanguage.FRENCH)
            .build()
        val client = Translation.getClient(options)
        try {
            client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            return texts.map { if (it.isBlank()) it else client.translate(it).await() }
        } finally {
            client.close()
        }
    }
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
