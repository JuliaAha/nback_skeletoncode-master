//package mobappdev.example.nback_cimpl.ui
//
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme
//
//@Composable
//fun FunProgressBar(progressValue: Int, maximum: Int) {
//    val animatedProgress by animateFloatAsState(
//        targetValue = (progressValue.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
//    )
//
//    NBack_CImplTheme {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Custom Progress Bar using Box for thickness and full width
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(24.dp)  // Adjusted thickness for better visibility
//                    .clip(MaterialTheme.shapes.medium)  // Rounded corners
//                    .background(MaterialTheme.colorScheme.primaryContainer)
//            ) {
//                // Filled part of the progress bar with gradient and animation
//                Box(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .fillMaxWidth(animatedProgress)
//                        .background(
//                            Brush.horizontalGradient(
//                                listOf(Color(0xFFFF8A65), Color(0xFF4CAF50))  // Gradient colors
//                            )
//                        )
//                )
//
//                // Overlay text to show progress percentage
//                Text(
//                    text = "${(animatedProgress * 100).toInt()}%",
//                    color = Color.White,
//                    style = MaterialTheme.typography.bodyMedium,
//                    modifier = Modifier.align(Alignment.Center)
//                )
//            }
//        }
//    }
//}
