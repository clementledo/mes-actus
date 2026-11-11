package fr.mesactus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.mesactus.data.Country
import fr.mesactus.data.Level
import fr.mesactus.data.ListStyle
import fr.mesactus.data.ThemeMode
import fr.mesactus.data.Zone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onSources: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Zones affichées")
            Text(
                "Le fil national est toujours affiché. Activez les zones précises qui vous intéressent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Country.entries.forEach { c ->
                Text(
                    "${c.flag}  ${c.label}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                )
                Zone.of(c).filter { it.level != Level.NATIONAL }.forEach { z ->
                    val shown = z !in settings.hiddenZones
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.updateSettings { s ->
                                    s.copy(hiddenZones = if (shown) s.hiddenZones + z else s.hiddenZones - z)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(z.label, Modifier.weight(1f))
                        Switch(checked = shown, onCheckedChange = null)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle("Affichage des articles")
            ListStyle.entries.forEach { st ->
                RadioRow(st.label, settings.listStyle == st) { vm.updateSettings { it.copy(listStyle = st) } }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle("Thème")
            ThemeMode.entries.forEach { t ->
                RadioRow(t.label, settings.theme == t) { vm.updateSettings { it.copy(theme = t) } }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSources)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Gérer les sources", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${sources.count { it.enabled }} actives sur ${sources.size} · ajouter un flux RSS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                "Mes Actus 1.0 · actualités via flux RSS publics et Google Actualités · météo Open-Meteo · " +
                    "traduction ML Kit sur l'appareil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp))
    }
}
