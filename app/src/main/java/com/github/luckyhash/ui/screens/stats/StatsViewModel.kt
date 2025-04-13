package com.github.luckyhash.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.domain.MiningRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel (
    private val miningRepository: MiningRepository,
) : ViewModel() {

    // Expose mining stats from repository
    val miningStats = miningRepository.miningStats

    // Expose mining config from repository
    val miningConfig: StateFlow<MiningConfig> = miningRepository.miningConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MiningConfig()
        )

    // Start mining from UI
    fun startMining() {
        val config = miningConfig.value

        if (config.runInBackground) {
            // Start foreground service for background mining
            val intent = Intent(context, MiningService::class.java) //TODO MOVE TO ACTIVITY
            context.startService(intent)
        } else {
            // Mine directly through repository (will stop when app exits)
            miningRepository.startMining(config.threads)
        }
    }

    // Stop mining from UI
    fun stopMining() {
        // Stop the service if it's running
        val intent = Intent(context, MiningService::class.java) //TODO MOVE TO ACTIVITY
        context.stopService(intent)

        // Also stop mining in repository in case it was started directly
        miningRepository.stopMining()
    }

    override fun onCleared() {
        // If not configured to run in background, stop mining when ViewModel is cleared
        if (!miningConfig.value.runInBackground) {
            miningRepository.stopMining()
        }
        super.onCleared()
    }
}