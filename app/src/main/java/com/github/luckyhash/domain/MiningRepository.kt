package com.github.luckyhash.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.data.MiningStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Random


// Extension for DataStore

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

        repeat(threads) {
            miningScope.launch {
                mine()
            }
        }
    }

    // Stop mining
    fun stopMining() {
        isRunning = false
        _miningStats.value = _miningStats.value.copy(isRunning = false)
    }

    // Mining logic
    private suspend fun mine() {
        var hashCount = 0L
        var attemptCount = 0L
        var bestMatch = 0

        val startTime = System.currentTimeMillis()

        while (isRunning) {
            // Generate a random 256-bit number (simulate a Bitcoin address)
            val candidateBytes = ByteArray(32)
            withContext(Dispatchers.IO) {
                random.nextBytes(candidateBytes)
            }

            // Hash the candidate
            val hash = withContext(Dispatchers.IO) {
                md.digest(candidateBytes)
            }

            // Count leading zeros in the hash (lottery winning condition)
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

            // Update best match if found
            if (leadingZeros > bestMatch) {
                bestMatch = leadingZeros
            }

            hashCount++
            attemptCount++

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
}