package com.github.hashpot.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.hashpot.domain.MiningRepository
import com.github.hashpot.ui.utils.EventFlow
import com.github.hashpot.ui.utils.EventFlowImpl

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