package com.github.luckyhash.data

data class MiningConfig(
    val threads: Int = 1,
    val runInBackground: Boolean = true,
    val difficultyTarget: Int = 1
)