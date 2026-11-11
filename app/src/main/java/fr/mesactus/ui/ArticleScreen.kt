package fr.mesactus.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fr.mesactus.data.BlockType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(vm: MainViewModel, onBack: () -> Unit) {
    val state by vm.reader.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val article = state.article
    val context = LocalContext.current
    val isFav = article != null && favorites.any { it.article.id == article.id }

    fun openInBrowser() {
        article ?: return
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.link))) }
    }

    fun share() {
        article ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, article.title)
            putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.link}")
        }
        context.startActivity(Intent.createChooser(send, "Partager l'article"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article?.sourceName ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                },
                actions = {
                    if (article != null) {
                        IconButton(onClick = { vm.toggleFavorite(article) }) {
                            Icon(
                                if (isFav) Icons.Default.Star else StarOutline,
                                if (isFav) "Retirer des favoris" else "Ajouter aux favoris",
                                tint = if (isFav) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { share() }) { Icon(Icons.Default.Share, "Partager") }
                    }
                },
            )
        },
    ) { padding ->
        if (article == null) return@Scaffold
        val content = if (state.showTranslated && state.translated != null) state.translated else state.content

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val image = content?.imageUrl ?: article.imageUrl
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    content?.title ?: article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    listOf(article.sourceName, fullDate(article.published), article.zone.fullLabel)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (article.lang != "fr" && state.content != null) {
                        FilledTonalButton(onClick = { vm.toggleTranslation() }, enabled = !state.translating) {
                            if (state.translating) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Traduction…")
                            } else {
                                Text(if (state.showTranslated) "Voir l'original" else "Traduire en français")
                            }
                        }
                    }
                    OutlinedButton(onClick = { openInBrowser() }) { Text("Ouvrir sur le site") }
                }
                if (state.translating && state.translated == null) {
                    Text(
                        "La première traduction télécharge le modèle de langue (~30 Mo). Ensuite, elle fonctionne même sans réseau.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.translateError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                when {
                    state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    content != null -> {
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        if (content.partial) {
                            Text(
                                "Le texte complet n'a pas pu être extrait de cette page. Voici le résumé disponible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                content.blocks.forEach { b ->
                                    if (b.type == BlockType.HEADING) {
                                        Text(b.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Text(b.text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp))
                                    }
                                }
                            }
                        }
                        if (content.partial) {
                            Button(onClick = { openInBrowser() }) { Text("Lire l'article sur le site") }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Étoile vide (absente du jeu d'icônes de base). */
private val StarOutline: ImageVector by lazy {
    ImageVector.Builder("StarOutline", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString(
                    "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24z" +
                        "M12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z"
                ).toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black),
        )
    }.build()
}
