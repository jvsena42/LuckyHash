package com.github.luckyhash.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.github.luckyhash.data.BlockInfo
import com.github.luckyhash.data.BlockTemplate
import com.github.luckyhash.data.MempoolBlocksResponse
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Random

class MiningRepository(
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) {

    // Preference keys
    private object PreferencesKeys {
        val THREADS = intPreferencesKey("threads")
        val RUN_IN_BACKGROUND = booleanPreferencesKey("run_in_background")
        val DIFFICULTY_TARGET = intPreferencesKey("difficulty_target")
    }

    // Mining statistics
    private val _miningStats = MutableStateFlow(MiningStats())
    val miningStats: StateFlow<MiningStats> = _miningStats.asStateFlow()

    // Mining configuration
    val miningConfig: Flow<MiningConfig> = dataStore.data.map { preferences ->
        MiningConfig(
            threads = preferences[PreferencesKeys.THREADS] ?: 1,
            runInBackground = preferences[PreferencesKeys.RUN_IN_BACKGROUND] ?: true,
            difficultyTarget = preferences[PreferencesKeys.DIFFICULTY_TARGET] ?: 1
        )
    }

    // Coroutine scope for mining
    private val miningScope = CoroutineScope(Dispatchers.Default)
    private var isRunning = false
    private val random = Random()
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
        }

        // Update target difficulty in current stats
        _miningStats.value = _miningStats.value.copy(targetDifficulty = config.difficultyTarget)
    }

    // Start mining
    fun startMining(threads: Int = 1) {
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
                _miningStats.value = _miningStats.value.copy(currentBlock = blockTemplate)

                // Start mining on multiple threads
                repeat(threads) {
                    miningScope.launch {
                        mine(blockTemplate)
                    }
                }
            } catch (e: Exception) {
                // If API fetch fails, use a fallback block template
                val fallbackTemplate = BlockTemplate(
                    previousBlockHash = "000000000000000000037c7c32a34baa4d5f4fd0ca5e8678841d4d219f034749",
                    merkleRoot = "bb37f74134215f0c1b499d42342befed3babe1a0285cd212b50a6f831cc38f75",
                    timestamp = (System.currentTimeMillis() / 1000).toInt(),
                    bits = "386689514"
                )

                _miningStats.value = _miningStats.value.copy(currentBlock = fallbackTemplate)

                // Start mining with fallback template
                repeat(threads) {
                    miningScope.launch {
                        mine(fallbackTemplate)
                    }
                }
            }
        }
    }

    // Stop mining
    fun stopMining() {
        isRunning = false
        _miningStats.value = _miningStats.value.copy(isRunning = false)
    }

    private suspend fun fetchLatestBlockTemplate(): BlockTemplate = withContext(Dispatchers.IO) {
        // Get recent blocks from mempool.space API
        val blocksResponse: MempoolBlocksResponse = client.get("https://mempool.space/api/v1/blocks").body()

        if (blocksResponse.blocks.isEmpty()) {
            throw Exception("No blocks returned from API")
        }

        // Get the latest block details
        val latestBlock = blocksResponse.blocks.first()
        val blockInfo: BlockInfo = client.get("https://mempool.space/api/block/${latestBlock.hash}").body()

        // Create a block template from the latest block
        return@withContext BlockTemplate(
            version = blockInfo.version,
            previousBlockHash = blockInfo.previousblockhash,
            merkleRoot = blockInfo.merkle_root,
            timestamp = blockInfo.timestamp,
            bits = blockInfo.bits.toString(16), // Convert to hex string
            height = blockInfo.height
        )
    }

    private suspend fun mine(blockTemplate: BlockTemplate) {
        var hashCount = 0L
        var attemptCount = 0L
        var bestMatch = 0
        var nonce = 0

        val startTime = System.currentTimeMillis()

        while (isRunning) {
            // Create a block header with the current nonce
            val blockHeader = createBlockHeader(blockTemplate, nonce)

            // Double SHA-256 hash (Bitcoin standard)
            val hash = withContext(Dispatchers.IO) {
                sha256Twice(blockHeader)
            }

            // Check hash result (count leading zeros)
            val leadingZeros = countLeadingZeros(hash)

            // Update best match if found
            if (leadingZeros > bestMatch) {
                bestMatch = leadingZeros
                // If we match our target difficulty, we found a valid share
                if (leadingZeros >= _miningStats.value.targetDifficulty) {
                    val hashHex = hash.joinToString("") { String.format("%02x", it) }
                    println("Found valid share! Nonce: $nonce, Hash: $hashHex")
                }
            }

            hashCount++
            attemptCount++
            nonce++

            // Update statistics every 1000 hashes
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

            // Small delay to prevent CPU overload
            if (hashCount % 100 == 0L) {
                withContext(Dispatchers.IO) {
                    Thread.sleep(1)
                }
            }
        }
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