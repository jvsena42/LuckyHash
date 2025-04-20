package com.github.luckyhash.ui.screens.config

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.PIXEL_TABLET
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.ui.components.BackgroundCard
import com.github.luckyhash.ui.components.PerformanceCard
import com.github.luckyhash.ui.theme.LuckyHashTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    restartService: () -> Unit,
    viewModel: ConfigViewModel = koinViewModel()
) {
    val config by viewModel.miningConfig.collectAsState()

    ConfigScreen(
        onNavigateBack = onNavigateBack,
        onSaveConfig = { newConfig ->
            viewModel.saveConfig(newConfig)
            onNavigateBack()
            restartService()
        },
        config = config,
        onThreadsUpdate = { threads -> viewModel.saveConfig(config.copy(threads = threads)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    config: MiningConfig,
    onNavigateBack: () -> Unit,
    onThreadsUpdate: (Int) -> Unit,
    onSaveConfig: (MiningConfig) -> Unit
) {

    var threads by remember { mutableIntStateOf(config.threads) }
    var runInBackground by remember { mutableStateOf(config.runInBackground) }
    var difficultyTarget by remember { mutableIntStateOf(config.difficultyTarget) }

    if (threads != config.threads) threads = config.threads
    if (runInBackground != config.runInBackground) runInBackground = config.runInBackground
    if (difficultyTarget != config.difficultyTarget) difficultyTarget = config.difficultyTarget

    // Thread slider value
    var threadSliderValue by remember { mutableFloatStateOf(threads.toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mining Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PerformanceCard(
                threads = threads,
                threadSliderValue = threadSliderValue,
                onThreadSliderChange = { value ->
                    threadSliderValue = value
                    threads = value.roundToInt()
                    onThreadsUpdate(value.roundToInt())
                }
            )

            BackgroundCard(
                runInBackground = runInBackground,
                onRunInBackgroundChange = { value ->
                    runInBackground = value
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val newConfig = MiningConfig(
                        threads = threads,
                        runInBackground = runInBackground,
                        difficultyTarget = difficultyTarget
                    )
                    onSaveConfig(newConfig)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Preview(showBackground = true, device = PIXEL_TABLET, uiMode = Configuration.ORIENTATION_LANDSCAPE)
@Composable
private fun Preview() {
    LuckyHashTheme {
        ConfigScreen(
            config = MiningConfig(),
            onSaveConfig = {},
            onNavigateBack = {},
            onThreadsUpdate = {}
        )
    }
}