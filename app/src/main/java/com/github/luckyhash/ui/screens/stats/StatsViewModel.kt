package com.github.luckyhash.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.domain.MiningRepository
import com.github.luckyhash.ui.utils.EventFlow
import com.github.luckyhash.ui.utils.EventFlowImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel (
    private val miningRepository: MiningRepository,
) : ViewModel(), EventFlow<StatsEvents> by EventFlowImpl() {

    val miningStats = miningRepository.miningStats

    val miningConfig: StateFlow<MiningConfig> = miningRepository.miningConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MiningConfig()
        )

    fun startMining() {
        val config = miningConfig.value

        if (config.runInBackground) {
            viewModelScope.sendEvent(StatsEvents.StartService)
        } else {
            miningRepository.startMining()
        }
    }

    fun stopMining() {
        viewModelScope.sendEvent(StatsEvents.StopService)
        miningRepository.stopMining()
    }

    override fun onCleared() {
        if (!miningConfig.value.runInBackground) {
            miningRepository.stopMining()
        }
        super.onCleared()
    }
}