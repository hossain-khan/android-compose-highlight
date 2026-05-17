package dev.hossain.highlight.sample.perf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Performance benchmark Activity that renders all code samples in a scrollable list and
 * measures per-block highlighting timing, code size, and overall memory footprint.
 *
 * Launch this from [dev.hossain.highlight.sample.MainActivity] via the ⚡ button in the top bar.
 */
class PerfActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                PerfScreen()
            }
        }
    }
}
