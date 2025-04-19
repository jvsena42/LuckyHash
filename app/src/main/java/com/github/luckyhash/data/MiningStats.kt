package com.github.luckyhash.data

data class MiningStats(
    val hashRate: Double = 0.0,
    val totalHashes: Long = 0L,
    val attemptsCount: Long = 0L,
    val bestMatchBits: Int = 0,
    val isRunning: Boolean = false,
    val startTime: Long = 0L,
    val targetDifficulty: Int = 1,
    val currentBlock: BlockTemplate? = null
)