package fr.mesactus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.mesactus.data.Article
import fr.mesactus.data.ListStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun timeAgo(ms: Long): String {
    if (ms <= 0L) return ""
    val diff = System.currentTimeMillis() - ms
    val min = diff / 60_000
    return when {
        min < 1 -> "à l'instant"
        min < 60 -> "il y a $min min"
        min < 24 * 60 -> "il y a ${min / 60} h"
        min < 7 * 24 * 60 -> "il y a ${min / (24 * 60)} j"
        else -> SimpleDateFormat("d MMM yyyy", Locale.FRANCE).format(Date(ms))
    }
}

fun fullDate(ms: Long): String =
    if (ms <= 0L) "" else SimpleDateFormat("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRANCE).format(Date(ms))

@Composable
private fun MetaLine(article: Article, showZone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (article.lang != "fr") {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    article.lang.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        val parts = listOfNotNull(
            article.sourceName,
            timeAgo(article.published).ifBlank { null },
            if (showZone) article.zone.fullLabel else null,
        )
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArticleItem(article: Article, style: ListStyle, onClick: () -> Unit, showZone: Boolean = false) {
    when (style) {
        ListStyle.CARDS -> ElevatedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            if (article.imageUrl != null) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaLine(article, showZone)
                Text(article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (article.description.isNotBlank()) {
                    Text(
                        article.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        ListStyle.COMPACT -> Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetaLine(article, showZone)
                    Text(
                        article.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (article.imageUrl != null) {
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }

        ListStyle.TITLES -> Column {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(article.title, style = MaterialTheme.typography.bodyLarge)
                MetaLine(article, showZone)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}
