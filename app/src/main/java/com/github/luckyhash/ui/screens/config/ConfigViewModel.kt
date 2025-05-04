package com.github.luckyhash.ui.screens.config
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.domain.MiningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ConfigViewModel(
    private val miningRepository: MiningRepository
) : ViewModel() {

    // Expose mining config from repository
    val miningConfig: StateFlow<MiningConfig> = miningRepository.miningConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MiningConfig()
        )

    // Save configuration
    fun saveConfig(config: MiningConfig) {
        viewModelScope.launch {
            miningRepository.saveMiningConfig(config)
        }

        viewModelScope.launch(Dispatchers.Default) {
            miningRepository.stopMining()
            delay(1.seconds)
            miningRepository.startMining()
        }
    }
}