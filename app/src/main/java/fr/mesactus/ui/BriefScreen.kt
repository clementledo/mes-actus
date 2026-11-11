package fr.mesactus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fr.mesactus.data.Section
import fr.mesactus.data.Story
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun timeAgo(ms: Long): String {
    if (ms <= 0L) return ""
    val min = (System.currentTimeMillis() - ms) / 60_000
    return when {
        min < 1 -> "à l'instant"
        min < 60 -> "il y a $min min"
        min < 24 * 60 -> "il y a ${min / 60} h"
        else -> SimpleDateFormat("d MMM", Locale.FRANCE).format(Date(ms))
    }
}

fun fullDate(ms: Long): String =
    if (ms <= 0L) "" else SimpleDateFormat("EEEE d MMMM 'à' HH:mm", Locale.FRANCE).format(Date(ms))

private fun storyMeta(story: Story): String {
    val parts = mutableListOf(story.lead.sourceName)
    timeAgo(story.lead.published).takeIf { it.isNotEmpty() }?.let { parts += it }
    if (story.outlets.size >= 2) parts += "${story.outlets.size} médias"
    return parts.joinToString("  ·  ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefScreen(vm: MainViewModel, onOpen: (Story) -> Unit, onSettings: () -> Unit) {
    val brief by vm.brief.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val updatedAt by vm.updatedAt.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        vm.refreshIfStale()
        onPauseOrDispose { }
    }

    val sections = Section.entries.filter { it !in settings.hiddenSections }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PullToRefreshBox(
            isRefreshing = refreshing && updatedAt > 0,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item(key = "header") { Header(updatedAt, refreshing, onSettings) }
                sections.forEach { section ->
                    item(key = "s_" + section.name) {
                        SectionBlock(section, brief[section] ?: SectionState(), onOpen)
                    }
                }
                item(key = "footer") {
                    Text(
                        "Tirez vers le bas pour actualiser",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(updatedAt: Long, refreshing: Boolean, onSettings: () -> Unit) {
    val today = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date())
        .replaceFirstChar { it.titlecase(Locale.FRANCE) }
    Column(
        Modifier
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "MES ACTUS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Réglages", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(today, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
        Text(
            when {
                refreshing && updatedAt == 0L -> "Chargement des grands titres…"
                refreshing -> "Actualisation…"
                else -> "L'essentiel · mis à jour ${timeAgo(updatedAt)}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionBlock(section: Section, state: SectionState, onOpen: (Story) -> Unit) {
    Column(Modifier.padding(top = 28.dp)) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(section.flag, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(section.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Box(
            Modifier
                .padding(start = 20.dp, top = 8.dp, bottom = 14.dp)
                .width(36.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)),
        )

        when {
            state.stories.isEmpty() && state.loading -> Placeholder()
            state.stories.isEmpty() -> Text(
                "Rien à afficher pour le moment. Vérifiez votre connexion puis tirez vers le bas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            else -> {
                HeroStory(state.stories.first(), onOpen)
                state.stories.drop(1).forEachIndexed { i, story ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    StoryRow(i + 2, story, onOpen)
                }
            }
        }
    }
}

@Composable
private fun HeroStory(story: Story, onOpen: (Story) -> Unit) {
    val a = story.lead
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpen(story) },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (a.imageUrl != null) 4f / 3.4f else 16f / 8f)) {
            if (a.imageUrl != null) {
                AsyncImage(
                    model = a.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.35f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.85f),
                            )
                        ),
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "À LA UNE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Text(
                    a.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(storyMeta(story), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun StoryRow(number: Int, story: Story, onOpen: (Story) -> Unit) {
    val a = story.lead
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(story) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            number.toString(),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
            modifier = Modifier.width(30.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                a.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(storyMeta(story), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (a.imageUrl != null) {
            Spacer(Modifier.width(14.dp))
            AsyncImage(
                model = a.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun Placeholder() {
    val c = MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(c, RoundedCornerShape(20.dp)))
        repeat(2) {
            Box(Modifier.padding(horizontal = 4.dp).fillMaxWidth(0.9f).height(18.dp).background(c, RoundedCornerShape(6.dp)))
            Box(Modifier.padding(horizontal = 4.dp).fillMaxWidth(0.6f).height(18.dp).background(c, RoundedCornerShape(6.dp)))
        }
    }
}
