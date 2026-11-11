package fr.mesactus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.mesactus.ui.ArticleScreen
import fr.mesactus.ui.HomeScreen
import fr.mesactus.ui.MainViewModel
import fr.mesactus.ui.SearchScreen
import fr.mesactus.ui.SettingsScreen
import fr.mesactus.ui.SourcesScreen
import fr.mesactus.ui.theme.MesActusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            MesActusTheme(settings.theme) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            vm = vm,
                            onOpenArticle = { vm.openArticle(it); nav.navigate("article") },
                            onSearch = { nav.navigate("search") },
                            onSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("article") { ArticleScreen(vm = vm, onBack = { nav.popBackStack() }) }
                    composable("search") {
                        SearchScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onOpenArticle = { vm.openArticle(it); nav.navigate("article") },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(vm = vm, onBack = { nav.popBackStack() }, onSources = { nav.navigate("sources") })
                    }
                    composable("sources") { SourcesScreen(vm = vm, onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}
