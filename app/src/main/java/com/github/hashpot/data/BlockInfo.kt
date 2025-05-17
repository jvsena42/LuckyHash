package com.github.hashpot.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockInfo(
    val id: String,
    val height: String,
    val version: String,
    val timestamp: Int,
    val bits: Int,
    val nonce: String,
    val merkle_root: String,
    val previousblockhash: String
)