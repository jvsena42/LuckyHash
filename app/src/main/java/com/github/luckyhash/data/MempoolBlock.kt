package com.github.luckyhash.data

import kotlinx.serialization.Serializable

@Serializable
data class MempoolBlock(
    val id: String,
    val height: Int,
    val version: Int,
    val timestamp: Long,
    val bits: Long,
    val nonce: Long,
    val difficulty: Double,
    val merkle_root: String,
    val tx_count: Int,
    val size: Int,
    val weight: Int,
    val previousblockhash: String,
    val mediantime: Long,
    val stale: Boolean,
)