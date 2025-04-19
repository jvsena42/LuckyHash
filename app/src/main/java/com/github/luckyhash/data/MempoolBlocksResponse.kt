package com.github.luckyhash.data

import kotlinx.serialization.Serializable

@Serializable
data class MempoolBlocksResponse(
    val blocks: List<MempoolBlock>
)