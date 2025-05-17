package com.github.hashpot.ui.screens.stats

sealed interface StatsEvents {
    data object StartService: StatsEvents
    data object StopService: StatsEvents
}