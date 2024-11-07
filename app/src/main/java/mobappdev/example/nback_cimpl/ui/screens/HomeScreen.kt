package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mobappdev.example.nback_cimpl.ui.GAME_ROUTE
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * This is the Home screen composable
 *
 * Currently this screen shows the saved highscore
 * It also contains a button which can be used to show that the C-integration works
 * Furthermore it contains two buttons that you can use to start a game
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

@Composable
fun HomeScreen(
    vm: GameViewModel,
    navController: NavController

) {
    val highscore by vm.highscore.collectAsState()  // Highscore is its own StateFlow
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(32.dp),
                text = "High-Score = $highscore",
                style = MaterialTheme.typography.headlineLarge
            )
            Button(onClick = {
                navController.navigate("settings")
            }) {
                Text(text = "Settings")
            }

            Button(
                onClick = {
                    vm.setGameType(GameType.AudioVisual)
                    vm.startGame()
                    navController.navigate(GAME_ROUTE)
                },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Start Game")
            }
            Button(
                onClick = {
                    vm.setGameType(GameType.Visual)
                    vm.startGame()
                    navController.navigate(GAME_ROUTE)
                },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Start Game Visual Only")
            }
            Button(
                onClick = {
                    vm.setGameType(GameType.Audio)
                    vm.startGame()
                    navController.navigate(GAME_ROUTE)
                },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Start Game Audio Only")
            }
        }
    }
}