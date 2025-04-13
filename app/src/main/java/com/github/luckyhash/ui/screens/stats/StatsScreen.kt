package com.github.luckyhash.ui.screens.stats


import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices.PIXEL_TABLET
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.luckyhash.data.MiningStats
import com.github.luckyhash.ui.theme.LuckyHashTheme
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateToConfig: () -> Unit,
    startService: () -> Unit,
    stopService: () -> Unit,
    viewModel: StatsViewModel = koinViewModel()
) {
    val stats by viewModel.miningStats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                StatsEvents.StartService -> startService()
                StatsEvents.StopService -> stopService()
            }
        }
    }

    StatsScreen(
        stats = stats,
        onNavigateToConfig = onNavigateToConfig,
        startMining = { viewModel.startMining() },
        stopMining = { viewModel.stopMining() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: MiningStats,
    onNavigateToConfig: () -> Unit,
    startMining: () -> Unit,
    stopMining: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bitcoin Lottery Miner") }
            )
        },
        floatingActionButton = {
            Row {
                if (stats.isRunning) {
                    FloatingActionButton(
                        onClick = stopMining
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop Mining")
                    }
                } else {
                    FloatingActionButton(
                        onClick = startMining
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Mining")
                    }
                }

                Spacer(modifier = Modifier.padding(8.dp))

                FloatingActionButton(
                    onClick = onNavigateToConfig
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatsCard(stats)
            ProgressCard(stats)
        }
    }
}

@Composable
fun StatsCard(stats: MiningStats) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsRow("Status:", if (stats.isRunning) "Running" else "Stopped")
            StatsRow("Hash Rate:", "${String.format("%.2f", stats.hashRate)} H/s")
            StatsRow("Total Hashes:", formatLargeNumber(stats.totalHashes))
            StatsRow("Total Attempts:", formatLargeNumber(stats.attemptsCount))
            StatsRow("Best Match:", "${stats.bestMatchBits} bits")
            StatsRow("Target Difficulty:", "${stats.targetDifficulty} bits")
            StatsRow("Running Time:", formatDuration(stats.startTime))
        }
    }
}

@Composable
fun ProgressCard(stats: MiningStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mining Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress indicator showing current best match vs target
            val progress = if (stats.targetDifficulty > 0) {
                stats.bestMatchBits.toFloat() / stats.targetDifficulty.toFloat()
            } else 0f

            Text(
                text = "${stats.bestMatchBits} / ${stats.targetDifficulty} bits matched",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = estimateTimeToWin(stats),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true, device = PIXEL_TABLET, uiMode = Configuration.ORIENTATION_LANDSCAPE)
@Composable
private fun Preview1() {
    LuckyHashTheme {
        StatsScreen(
            stats = MiningStats(
                isRunning = true
            ),
            startMining = {},
            stopMining = {},
            onNavigateToConfig = {}
        )
    }
}

@Preview(showBackground = true, device = PIXEL_TABLET, uiMode = Configuration.ORIENTATION_LANDSCAPE)
@Composable
private fun Preview2() {
    LuckyHashTheme {
        StatsScreen(
            stats = MiningStats(
                isRunning = false
            ),
            startMining = {},
            stopMining = {},
            onNavigateToConfig = {}
        )
    }
}

// Helper functions
fun formatLargeNumber(number: Long): String {
    return when {
        number < 1_000 -> number.toString()
        number < 1_000_000 -> String.format("%.2fK", number / 1_000.0)
        number < 1_000_000_000 -> String.format("%.2fM", number / 1_000_000.0)
        else -> String.format("%.2fB", number / 1_000_000_000.0)
    }
}

fun formatDuration(startTime: Long): String {
    if (startTime == 0L) return "00:00:00"

    val currentTime = System.currentTimeMillis()
    val durationMillis = currentTime - startTime

    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun estimateTimeToWin(stats: MiningStats): String {
    if (stats.hashRate <= 0 || stats.targetDifficulty <= stats.bestMatchBits) {
        return "Time estimate unavailable"
    }

    // Each additional bit doubles the difficulty
    val remainingBits = stats.targetDifficulty - stats.bestMatchBits
    val approxHashes = Math.pow(2.0, remainingBits.toDouble())

    // Estimated time in seconds
    val estimatedTimeSeconds = approxHashes / stats.hashRate

    return when {
        estimatedTimeSeconds < 60 -> "Est. time to win: ${estimatedTimeSeconds.toInt()} seconds"
        estimatedTimeSeconds < 3600 -> "Est. time to win: ${(estimatedTimeSeconds / 60).toInt()} minutes"
        estimatedTimeSeconds < 86400 -> "Est. time to win: ${(estimatedTimeSeconds / 3600).toInt()} hours"
        estimatedTimeSeconds < 31536000 -> "Est. time to win: ${(estimatedTimeSeconds / 86400).toInt()} days"
        else -> "Est. time to win: ${(estimatedTimeSeconds / 31536000).toInt()} years"
    }
}