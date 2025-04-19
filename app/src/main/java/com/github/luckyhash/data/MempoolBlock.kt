package com.github.luckyhash.data

import kotlinx.serialization.Serializable

@Serializable
data class MempoolBlock(
    val height: Int,
    val hash: String
)