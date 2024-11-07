package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.HOME_ROUTE
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import androidx.navigation.NavController
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType

@Composable
fun GameScreen(
    vm: GameViewModel,
    navController: NavController? = null,
    gameMode: String
) {
    val gameState by vm.gameState.collectAsState()
    val gameType = gameState.gameType
    val score by vm.score.collectAsState()
    val currentIndex by vm.currentIndex.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val gridSize = gameState.gridSize
    val activatedPositions by vm.activatedPositions.collectAsState() // Collects the latest value as State

    DisposableEffect(Unit) {
        onDispose {
            vm.stopGame()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    vm.stopGame()
                    navController?.navigate(HOME_ROUTE)
                }) {
                    Text("Back")
                }
                Text(text = "Score: $score", style = MaterialTheme.typography.headlineMedium)
            }
        },
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "N-Back Game",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                text = "Current Event Number: ${currentIndex + 1}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                    items(gridSize * gridSize) { index ->
                        if (gameType != GameType.Audio) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)  // Ensures each tile is square
                                .background(
                                    color = when {
                                        index in activatedPositions -> Color.Green
                                        else -> Color.LightGray
                                    }
                                )
                        )
                        }
                        else{
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f)  // Ensures each tile is square
                                    .background(
                                        color = Color.LightGray

                                    )
                            )
                        }
                    }
            }

            if(gameType != GameType.Visual){
                Text(
                    text = "Current Letter: ${gameState.currentLetter}",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if(gameType != GameType.Visual) Button(onClick = { vm.checkAudioMatch() }) { Text("Check Audio") }
                if(gameType != GameType.Audio) Button(onClick = { vm.checkPlaceMatch() }) { Text("Check Place") }
            }

            if (gameState.feedback.isNotBlank()) {
                Text(
                    text = gameState.feedback,
                    color = if (gameState.feedback.contains("Correct")) Color.Green else Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

