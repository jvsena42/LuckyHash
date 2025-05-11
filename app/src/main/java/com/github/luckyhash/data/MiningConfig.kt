package com.github.luckyhash.data

data class MiningConfig(
    val threads: Int = 1,
    val bitcoinAddress: String = ""
)