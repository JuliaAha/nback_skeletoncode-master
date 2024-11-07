package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSettingsSaved: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Slider for Number of Events
        Text(text = "Number of Events: ${settings.numberOfEvents}")
        Slider(
            value = settings.numberOfEvents.toFloat(),
            onValueChange = { viewModel.setNumberOfEvents(it.toInt()) },
            valueRange = 5f..50f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Slider for Event Interval
        Text(text = "Event Interval (ms): ${settings.eventInterval}")
        Slider(
            value = settings.eventInterval.toFloat(),
            onValueChange = { viewModel.setEventInterval(it.toInt()) },
            valueRange = 500f..5000f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Slider for Grid Size
        Text(text = "Grid Size: ${settings.gridSize}x${settings.gridSize}")
        Slider(
            value = settings.gridSize.toFloat(),
            onValueChange = { viewModel.setGridSize(it.toInt()) },
            valueRange = 3f..10f,
            steps = 7, // Allows selection from 3 to 10
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Slider for Audio Letter Count
        Text(text = "Audio Letter Count: ${settings.audioLetterCount}")
        Slider(
            value = settings.audioLetterCount.toFloat(),
            onValueChange = { viewModel.setAudioLetterCount(it.toInt()) },
            valueRange = 1f..20f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(onClick = onSettingsSaved, modifier = Modifier.fillMaxWidth()) {
            Text("Save Settings")
        }
    }
}
