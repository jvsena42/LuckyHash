package com.github.luckyhash.domain

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.luckyhash.data.BlockInfo
import com.github.luckyhash.data.BlockTemplate
import com.github.luckyhash.data.MempoolBlock
import com.github.luckyhash.data.MempoolTransaction
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.data.MiningStats
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.math.roundToInt

class MiningRepository(
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) {

    // Preference keys
    private object PreferencesKeys {
        val THREADS = intPreferencesKey("threads")
        val RUN_IN_BACKGROUND = booleanPreferencesKey("run_in_background")
        val DIFFICULTY_TARGET = doublePreferencesKey("difficulty_target")
        val BTC_ADDRESS = stringPreferencesKey("btc_address")
    }

    companion object {
        const val TAG = "MiningRepository"
        const val BLOCK_REWARD_HALVING_INTERVAL = 210000
        const val FALLBACK_BTC_ADDRESS = "bc1qn5n9shs0q6d0l9k60rfy27xkj07wmf0ltkccqv"
        const val INITIAL_BLOCK_REWARD = 50 * 100_000_000L // 50 BTC in satoshis
        const val MAX_BLOCK_SIZE_BYTES = 1_000_000 // Simplified block size limit
    }

    // Mining statistics
    private val _miningStats = MutableStateFlow(MiningStats())
    val miningStats: StateFlow<MiningStats> = _miningStats.asStateFlow()

    // Mining configuration
    val miningConfig: Flow<MiningConfig> = dataStore.data.map { preferences ->
        MiningConfig(
            threads = preferences[PreferencesKeys.THREADS] ?: 1,
            runInBackground = preferences[PreferencesKeys.RUN_IN_BACKGROUND] ?: true,
            difficultyTarget = preferences[PreferencesKeys.DIFFICULTY_TARGET] ?: 119116256505723.5,
            bitcoinAddress = preferences[PreferencesKeys.BTC_ADDRESS].orEmpty().ifBlank { FALLBACK_BTC_ADDRESS }
        )
    }

    // Coroutine scope for mining
    private val miningScope = CoroutineScope(Dispatchers.Default)
    private var isRunning = false
    private val md = MessageDigest.getInstance("SHA-256")

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    // Save mining configuration
    suspend fun saveMiningConfig(config: MiningConfig) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THREADS] = config.threads
            preferences[PreferencesKeys.RUN_IN_BACKGROUND] = config.runInBackground
            preferences[PreferencesKeys.DIFFICULTY_TARGET] = config.difficultyTarget
            preferences[PreferencesKeys.BTC_ADDRESS] = config.bitcoinAddress
        }

        // Update target difficulty in current stats
        _miningStats.value = _miningStats.value.copy(targetDifficulty = config.difficultyTarget)
    }

    // Start mining
    fun startMining(threads: Int = 1) {
        Log.d(TAG, "startMining: threads: $threads")
        if (isRunning) return

        isRunning = true
        val startTime = System.currentTimeMillis()

        _miningStats.value = MiningStats(
            isRunning = true,
            startTime = startTime,
            targetDifficulty = _miningStats.value.targetDifficulty
        )

        // Fetch recent block data first
        miningScope.launch {
            try {
                val blockTemplate = fetchLatestBlockTemplate()
                val mempoolTransactions = fetchMempoolTransactions()

                val selectedTransactions = selectTransactionsForBlock(mempoolTransactions)

                val totalFees = selectedTransactions.sumOf { it.fee ?: 0 }

                val coinbaseTx = createCoinbaseTransaction(
                    blockHeight = blockTemplate.height,
                    btcAddress = miningConfig.first().bitcoinAddress,
                    fees = totalFees
                )

                // Combine all transactions (coinbase first)
                val allTransactions = listOf(coinbaseTx) + selectedTransactions

                // Build Merkle root
                val merkleRoot = buildMerkleTree(allTransactions)

                // Update block template with new merkle root
                val updatedTemplate = blockTemplate.copy(merkleRoot = merkleRoot)

                _miningStats.value = _miningStats.value.copy(
                    currentBlock = updatedTemplate,
                    transactionsInBlock = allTransactions.size,
                    totalFees = totalFees
                )

                // Start mining on multiple threads
                repeat(threads) {
                    miningScope.launch {
                        mine(updatedTemplate, allTransactions)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in mining setup: ${e.message}", e)
                stopMining()
            }
        }
    }

    // Stop mining
    fun stopMining() {
        isRunning = false
        _miningStats.value = _miningStats.value.copy(isRunning = false)
    }

    private suspend fun fetchMempoolTransactions(): List<MempoolTransaction> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching mempool transactions")
            val txIds: List<String> = client.get("https://mempool.space/api/mempool/txids").body()

            // Limit to top 100 transactions for simplicity
            val limitedTxIds = txIds.take(100)

            // Fetch details for each transaction
            limitedTxIds.mapNotNull { txId ->
                try {
                    client.get("https://mempool.space/api/tx/$txId").body<MempoolTransaction>()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch transaction $txId: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch mempool transactions: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchLatestBlockTemplate(): BlockTemplate = withContext(Dispatchers.IO) {
        Log.d(TAG, "fetchLatestBlockTemplate: ")
        // Get recent blocks from mempool.space API
        val blocksResponse: List<MempoolBlock> = client.get("https://mempool.space/api/v1/blocks").body()

        if (blocksResponse.isEmpty()) {
            throw Exception("No blocks returned from API")
        }

        // Get the latest block details
        val latestBlock = blocksResponse.first()
        val blockInfo: BlockInfo = client.get("https://mempool.space/api/block/${latestBlock.id}").body()

        // Create a block template from the latest block
        return@withContext BlockTemplate(
            version = blockInfo.version.toInt(),
            previousBlockHash = blockInfo.previousblockhash,
            merkleRoot = blockInfo.merkle_root,
            timestamp = blockInfo.timestamp,
            bits = blockInfo.bits.toString(16), // Convert to hex string
            height = blockInfo.height.toInt(),
            difficulty = latestBlock.difficulty
        )
    }

    private fun selectTransactionsForBlock(transactions: List<MempoolTransaction>): List<MempoolTransaction> {
        // Sort by fee rate (higher first)
        val sorted = transactions.sortedByDescending { it.fee?.toDouble()?.div((it.size ?: 1)) }

        // Select transactions until we reach block size limit
        var currentSize = 0
        val selected = mutableListOf<MempoolTransaction>()

        for (tx in sorted) {
            if (currentSize + (tx.size ?: 0) <= MAX_BLOCK_SIZE_BYTES) {
                selected.add(tx)
                currentSize += tx.size ?: 0
            } else {
                break
            }
        }

        return selected
    }

    private fun createCoinbaseTransaction(blockHeight: Int, btcAddress: String, fees: Long): MempoolTransaction {
        // Calculate block reward (halving every 210,000 blocks)
        val halvings = blockHeight / BLOCK_REWARD_HALVING_INTERVAL
        var blockReward = INITIAL_BLOCK_REWARD
        repeat(halvings) { blockReward /= 2 }

        val totalReward = blockReward + fees

        // Create a simplified coinbase transaction
        // TODO In a real implementation, you would need to:
        // 1. Create proper scriptSig with block height and extra nonce
        // 2. Create proper P2WPKH scriptPubKey for the recipient address
        return MempoolTransaction(
            txid = "coinbase_${System.currentTimeMillis()}",
            fee = 0,
            size = 100, // Approximate size for coinbase tx
            inputs = emptyList(), // Coinbase has no inputs
            outputs = listOf(
                MempoolTransaction.Output(
                    value = totalReward,
                    scriptPubKey = createP2wpkhScript(btcAddress)
                )
            )
        )
    }

    private fun createP2wpkhScript(address: String): String {
        // TODO In a real implementation, you would:
        // 1. Decode the bech32 address
        // 2. Create the proper witness program
        // TODO For now, return a placeholder
        return "0014${"a".repeat(40)}" // 0x00 (version) + 0x14 (20 bytes) + pubkey hash
    }

    // Build Merkle tree from transactions
    private fun buildMerkleTree(transactions: List<MempoolTransaction>): String {
        if (transactions.isEmpty()) return "0".repeat(64)

        // Start with transaction hashes
        var hashes = transactions.map { it.txid }

        // If odd number, duplicate last hash
        if (hashes.size % 2 != 0) {
            hashes = hashes + hashes.last()
        }

        // Build tree layers
        while (hashes.size > 1) {
            val newLevel = mutableListOf<String>()
            for (i in hashes.indices step 2) {
                val combined = hashes[i] + hashes[i + 1]
                val hash = sha256Twice(hexStringToByteArray(combined)).joinToString("") { "%02x".format(it) }
                newLevel.add(hash)
            }
            hashes = newLevel
            // If odd number, duplicate last hash
            if (hashes.size % 2 != 0 && hashes.size > 1) {
                hashes = hashes + hashes.last()
            }
        }

        return hashes.first()
    }

    private suspend fun mine(blockTemplate: BlockTemplate, transactions: List<MempoolTransaction>) {
        var hashCount = 0L
        var attemptCount = 0L
        var bestMatch = 0
        var nonce = 0

        val startTime = System.currentTimeMillis()
        val targetDifficulty = _miningStats.value.targetDifficulty
        val target = BigInteger.ONE.shiftLeft(256 - targetDifficulty.toInt())

        while (isRunning) {
            // Create a block header with the current nonce
            val blockHeader = createBlockHeader(blockTemplate, nonce)

            // Double SHA-256 hash (Bitcoin standard)
            val hash = withContext(Dispatchers.Default) {
                sha256Twice(blockHeader)
            }

            // Convert hash to BigInteger for difficulty comparison
            val hashInt = BigInteger(1, hash)

            // Check if hash meets target difficulty
            if (hashInt < target) {
                val hashHex = hash.joinToString("") { String.format("%02x", it) }
                Log.i(TAG, "Block found! Nonce: $nonce, Hash: $hashHex")

                // Update stats
                _miningStats.value = _miningStats.value.copy(
                    blocksFound = _miningStats.value.blocksFound + 1,
                    lastBlockHash = hashHex
                )

                // Conceptually broadcast the block
                broadcastBlock(blockTemplate.copy(nonce = nonce), transactions)

                // Start mining new block
                startMining()
                return
            }

            // Count leading zeros for statistics
            val leadingZeros = countLeadingZeros(hash)
            if (leadingZeros > bestMatch) {
                bestMatch = leadingZeros
            }

            hashCount++
            attemptCount++
            nonce++

            // Update statistics periodically
            if (hashCount % 1000 == 0L) {
                val currentTime = System.currentTimeMillis()
                val elapsedTimeSeconds = (currentTime - startTime) / 1000.0
                val hashRate = hashCount / elapsedTimeSeconds

                _miningStats.value = _miningStats.value.copy(
                    hashRate = hashRate,
                    totalHashes = _miningStats.value.totalHashes + hashCount,
                    attemptsCount = _miningStats.value.attemptsCount + attemptCount,
                    bestMatchBits = maxOf(_miningStats.value.bestMatchBits, bestMatch)
                )

                hashCount = 0
                attemptCount = 0
            }
        }
    }

    // Conceptual block broadcasting
    private fun broadcastBlock(blockTemplate: BlockTemplate, transactions: List<MempoolTransaction>) {
        //TODO IMPLEMENT
        Log.i(TAG, "Conceptual block broadcast:")
        Log.i(TAG, "Block hash: ${blockTemplate.previousBlockHash}")
        Log.i(TAG, "Transactions: ${transactions.size}")
        Log.i(TAG, "Would now send this block to Bitcoin network peers")

        // In a real implementation, you would:
        // 1. Serialize the complete block
        // 2. Connect to Bitcoin nodes
        // 3. Send the block using the 'block' message type
        // 4. Handle propagation and validation responses
    }

    private fun createBlockHeader(template: BlockTemplate, nonce: Int): ByteArray {
        // Bitcoin block header structure:
        // - Version (4 bytes, little endian)
        // - Previous block hash (32 bytes, little endian)
        // - Merkle root (32 bytes, little endian)
        // - Timestamp (4 bytes, little endian)
        // - Bits/Target (4 bytes, little endian)
        // - Nonce (4 bytes, little endian)

        val buffer = ByteArray(80) // Bitcoin block header is always 80 bytes

        // Version (4 bytes, little endian)
        writeInt32LE(buffer, 0, template.version)

        // Previous block hash (32 bytes, byte-reversed)
        val prevHash = hexStringToByteArray(template.previousBlockHash)
        System.arraycopy(reverseBytes(prevHash), 0, buffer, 4, 32)

        // Merkle root (32 bytes, byte-reversed)
        val merkleRoot = hexStringToByteArray(template.merkleRoot)
        System.arraycopy(reverseBytes(merkleRoot), 0, buffer, 36, 32)

        // Timestamp (4 bytes, little endian)
        writeInt32LE(buffer, 68, template.timestamp)

        // Bits/Target (4 bytes, little endian)
        val bits = if (template.bits.startsWith("0x")) {
            Integer.parseInt(template.bits.substring(2), 16)
        } else if (template.bits.all { it.isDigit() }) {
            template.bits.toInt()
        } else {
            Integer.parseInt(template.bits, 16)
        }
        writeInt32LE(buffer, 72, bits)

        // Nonce (4 bytes, little endian)
        writeInt32LE(buffer, 76, nonce)

        return buffer
    }

    // Double SHA-256 hash (Bitcoin standard)
    private fun sha256Twice(data: ByteArray): ByteArray {
        val firstHash = md.digest(data)
        return md.digest(firstHash)
    }

    // Count leading zeros in hash (similar to Bitcoin difficulty)
    private fun countLeadingZeros(hash: ByteArray): Int {
        var leadingZeros = 0
        for (byte in hash) {
            if (byte == 0.toByte()) {
                leadingZeros += 8
            } else {
                val zeros = Integer.numberOfLeadingZeros(byte.toInt() and 0xFF) - 24
                leadingZeros += zeros
                break
            }
        }
        return leadingZeros
    }

    // Helper functions for Bitcoin block header construction
    private fun writeInt32LE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun hexStringToByteArray(hexString: String): ByteArray {
        val cleanHex = hexString.replace("0x", "").replace(" ", "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) +
                    Character.digit(cleanHex[i + 1], 16)).toByte()
            i += 2
        }

        return data
    }

    private fun reverseBytes(data: ByteArray): ByteArray {
        val reversed = data.copyOf()
        for (i in 0 until data.size / 2) {
            val temp = reversed[i]
            reversed[i] = reversed[data.size - i - 1]
            reversed[data.size - i - 1] = temp
        }
        return reversed
    }

    // Calculate Bitcoin difficulty from bits
    private fun bitsToTarget(bits: String): BigInteger {
        val bitsValue = Integer.parseInt(bits, 16)
        val exponent = bitsValue shr 24
        val mantissa = bitsValue and 0x007FFFFF

        var target = BigInteger.valueOf(mantissa.toLong())

        // Apply the exponent (shift left by 8 * (exponent - 3))
        if (exponent > 3) {
            target = target.shiftLeft(8 * (exponent - 3))
        } else {
            target = target.shiftRight(8 * (3 - exponent))
        }

        return target
    }
}