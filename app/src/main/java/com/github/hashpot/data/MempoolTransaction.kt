package com.github.hashpot.data

import kotlinx.serialization.Serializable

@Serializable
data class MempoolTransaction(
    val txid: String,                  // Transaction ID (hash)
    val fee: Long?,                     // Transaction fee in satoshis
    val size: Int?,                     // Transaction size in bytes
    val weight: Int? = null,            // Transaction weight (for segwit)
    val inputs: List<Input> = emptyList(),  // List of transaction inputs
    val outputs: List<Output> = emptyList() // List of transaction outputs
) {
    @Serializable
    data class Input(
        val txid: String? = null,       // Previous transaction ID
        val vout: Int? = null,          // Output index in previous transaction
        val scriptSig: String? = null,  // Input script
        val witness: List<String>? = null, // Witness data for segwit
        val sequence: Long? = null      // Sequence number
    )

    @Serializable
    data class Output(
        val value: Long,                // Output value in satoshis
        val scriptPubKey: String,       // Output script
        val address: String? = null    // Optional address (if script can be decoded)
    )

    // Additional properties that might be useful
    val locktime: Long? = null         // Locktime
    val version: Int? = null           // Transaction version
    val status: Status? = null          // Transaction status in mempool

    @Serializable
    data class Status(
        val confirmed: Boolean,         // Whether transaction is confirmed
        val blockHeight: Int? = null,   // Block height if confirmed
        val blockHash: String? = null,  // Block hash if confirmed
        val blockTime: Long? = null     // Block timestamp if confirmed
    )
}