package mobappdev.example.nback_cimpl.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mobappdev.example.nback_cimpl.ui.screens.GameScreen
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.screens.SettingsScreen
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.SettingsViewModel

const val HOME_ROUTE = "home"
const val GAME_ROUTE = "game/{mode}" // Route with mode parameter for game
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

        composable(
            route = GAME_ROUTE,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "Dual"
            GameScreen(
                vm = gameViewModel,
                navController = navController,
                gameMode = mode
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
