package com.github.luckyhash.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockTemplate(
    val version: Int = 536870912,             // Block version
    val previousBlockHash: String = "",       // Hash of the previous block
    val merkleRoot: String = "",              // Merkle root of transactions
    val timestamp: Int = (System.currentTimeMillis() / 1000).toInt(), // Current timestamp
    val bits: String = "",                    // Compact form of the target
    val height: Int = 0,                     // Block height
    val difficulty: Double = 119116256505723.5,                       // Difficulty
    val nonce: Int = 1                       // Nonce
)