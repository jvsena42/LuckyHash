package com.github.luckyhash.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.luckyhash.domain.MiningRepository
import com.github.luckyhash.ui.utils.EventFlow
import com.github.luckyhash.ui.utils.EventFlowImpl

class StatsViewModel (
    private val miningRepository: MiningRepository,
) : ViewModel(), EventFlow<StatsEvents> by EventFlowImpl() {

    val miningStats = miningRepository.miningStats

    fun startMining() {
        viewModelScope.sendEvent(StatsEvents.StartService)
    }

    fun stopMining() {
        viewModelScope.sendEvent(StatsEvents.StopService)
        miningRepository.stopMining()
    }
}