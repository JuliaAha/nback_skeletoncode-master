package mobappdev.example.nback_cimpl.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import mobappdev.example.nback_cimpl.ui.screens.GameScreen
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.screens.SettingsScreen
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.SettingsViewModel

const val HOME_ROUTE = "home"
const val GAME_ROUTE = "game"
const val SETTINGS_ROUTE = "settings"

@Composable
fun NavigationGraph(
    gameViewModel: GameViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(
                vm = gameViewModel,
                navController = navController
            )
        }
        composable(GAME_ROUTE) {
            GameScreen(
                vm = gameViewModel,
                navController = navController
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onSettingsSaved = {
                    navController.popBackStack()
                }
            )
        }
    }
}
