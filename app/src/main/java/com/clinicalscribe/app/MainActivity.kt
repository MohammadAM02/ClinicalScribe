package com.clinicalscribe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clinicalscribe.app.ui.HomeScreen
import com.clinicalscribe.app.ui.SettingsScreen
import com.clinicalscribe.app.ui.SnippetsScreen
import com.clinicalscribe.app.ui.VocabularyScreen
import com.clinicalscribe.app.ui.theme.ClinicalScribeTheme

object Routes {
    const val HOME = "home"
    const val VOCABULARY = "vocabulary"
    const val SNIPPETS = "snippets"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(applicationContext)

        setContent {
            ClinicalScribeTheme {
                val navController = rememberNavController()
                ClinicalScribeNavHost(navController)
            }
        }
    }
}

@Composable
private fun ClinicalScribeNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenVocabulary = { navController.navigate(Routes.VOCABULARY) },
                onOpenSnippets = { navController.navigate(Routes.SNIPPETS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.VOCABULARY) {
            VocabularyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SNIPPETS) {
            SnippetsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
