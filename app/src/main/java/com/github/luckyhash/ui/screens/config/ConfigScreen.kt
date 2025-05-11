package com.github.luckyhash.ui.screens.config

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.ui.components.PerformanceCard
import com.github.luckyhash.ui.theme.LuckyHashTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigViewModel = koinViewModel()
) {
    val config by viewModel.miningConfig.collectAsStateWithLifecycle()
    val availableThreads by remember { mutableIntStateOf(viewModel.getAvailableProcessors()) }

    ConfigScreen(
        onNavigateBack = onNavigateBack,
        config = config,
        availableThreads = availableThreads,
        onThreadsUpdate = { threads -> viewModel.onThreadChange(threads) },
        onAddressChanged = { newAddress ->
            viewModel.handleAddressChange(newAddress)
            onNavigateBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(
    config: MiningConfig,
    availableThreads: Int,
    onNavigateBack: () -> Unit,
    onThreadsUpdate: (Int) -> Unit,
    onAddressChanged: (String) -> Unit,
) {
    var threads by remember { mutableIntStateOf(config.threads) }
    var bitcoinAddress by remember { mutableStateOf(config.bitcoinAddress) }
    var threadSliderValue by remember { mutableFloatStateOf(threads.toFloat()) }

    LaunchedEffect(config) {
        threads = config.threads
        bitcoinAddress = config.bitcoinAddress
        threadSliderValue = config.threads.toFloat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mining Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                },
                valueRange = 1f..availableThreads.toFloat(),
                steps = availableThreads - 1
            )

            TextField(
                value = bitcoinAddress,
                onValueChange = { newText -> bitcoinAddress = newText.filterNot { it.isWhitespace() } },
                label = { Text("Set your Bitcoin address") },
                placeholder = { Text("bc1q...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onAddressChanged(bitcoinAddress)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Address")
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
            availableThreads = 8,
            onNavigateBack = {},
            onThreadsUpdate = {},
            onAddressChanged = {}
        )
    }
}