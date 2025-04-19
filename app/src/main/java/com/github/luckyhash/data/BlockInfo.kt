package com.github.luckyhash.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockInfo(
    val id: String,
    val height: Int,
    val version: Int,
    val timestamp: Int,
    val bits: Int,
    val nonce: Int,
    val merkle_root: String,
    val previousblockhash: String
)