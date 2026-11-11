package fr.mesactus.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fr.mesactus.data.BlockType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(vm: MainViewModel, onBack: () -> Unit) {
    val state by vm.reader.collectAsStateWithLifecycle()
    val article = state.article
    val context = LocalContext.current

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                },
                actions = {
                    if (article != null) IconButton(onClick = { share() }) { Icon(Icons.Default.Share, "Partager") }
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    article.zone.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    content?.title ?: article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    listOf(article.sourceName, fullDate(article.published)).filter { it.isNotBlank() }.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.outlets.size >= 2) {
                    Text(
                        "Aussi à la une de : " + state.outlets.filter { it != article.sourceName }.take(6).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val image = content?.imageUrl ?: article.imageUrl
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(18.dp)),
                )
            } else {
                Spacer(Modifier.height(18.dp))
            }

            Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (article.lang != "fr" && state.content != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    if (state.translating && state.translated == null) {
                        Text(
                            "La première traduction télécharge le modèle de langue (~30 Mo), ensuite elle est instantanée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.translateError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }

                when {
                    state.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    content != null -> {
                        if (content.partial) {
                            Text(
                                "Ce média ne permet pas d'afficher l'article ici.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                content.blocks.forEach { b ->
                                    if (b.type == BlockType.HEADING) {
                                        Text(b.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                    } else {
                                        Text(b.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        if (content.partial) {
                            Button(onClick = { openInBrowser() }, modifier = Modifier.fillMaxWidth()) { Text("Lire sur ${article.sourceName}") }
                        } else {
                            OutlinedButton(onClick = { openInBrowser() }, modifier = Modifier.fillMaxWidth()) { Text("Voir sur le site") }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp).background(MaterialTheme.colorScheme.background))
            }
        }
    }
}
