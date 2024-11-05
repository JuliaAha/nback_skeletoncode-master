package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import androidx.navigation.NavController

@Composable
fun GameScreen(
    vm: GameViewModel,
    navController: NavController? = null
) {
    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { navController?.navigate("home") }) {
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

            // 3x3 Grid
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(80.dp)
                                    .background(
                                        color = if (index == gameState.currentPosition) Color.Green else Color.Gray
                                    )
                            )
                        }
                    }
                }
            }

            // Display the current letter
            Text(
                text = "Current Letter: ${gameState.currentLetter}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Check buttons and feedback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = { vm.checkAudioMatch() }) { Text("Check Audio") }
                Button(onClick = { vm.checkPlaceMatch() }) { Text("Check Place") }
            }

            // Feedback message
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
