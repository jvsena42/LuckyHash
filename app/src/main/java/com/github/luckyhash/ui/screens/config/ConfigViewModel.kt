package com.github.luckyhash.ui.screens.config
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.luckyhash.data.MiningConfig
import com.github.luckyhash.domain.MiningRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ConfigViewModel(
    private val miningRepository: MiningRepository
) : ViewModel() {
    var updateThreadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    // Expose mining config from repository
    val miningConfig: StateFlow<MiningConfig> = miningRepository.miningConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MiningConfig()
        )

    init {
        viewModelScope.launch {
            miningConfig.collect { config ->
                Log.d("ConfigViewModel", "Mining config: $config")

            }
        }
    }

    fun handleAddressChange(newAddress: String) {

        val validatedText = newAddress.filterNot{ it.isWhitespace()} //TODO CHECK ALSO VALID ADDRESS

        viewModelScope.launch {
            miningRepository.saveMiningConfig(miningConfig.value.copy(
                bitcoinAddress = validatedText
            ))
        }

    }

    fun onThreadChange(threadNumber: Int) {
        updateThreadScope.cancel()
        updateThreadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        updateThreadScope.launch {
            delay(1.seconds)
            miningRepository.saveMiningConfig(miningConfig.value.copy(
                threads = threadNumber
            ))
        }

    }
}