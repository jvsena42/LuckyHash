package com.github.hashpot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.hashpot.domain.MiningRepository
import com.github.hashpot.domain.MiningService
import com.github.hashpot.domain.MiningService.Companion.MINING_CHANNEL_ID
import com.github.hashpot.domain.MiningStopReceiver
import com.github.hashpot.ui.screens.config.ConfigViewModel
import com.github.hashpot.ui.screens.stats.StatsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class HashpotApplication : Application() {

    private lateinit var miningStopReceiver: MiningStopReceiver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startKoin {
            androidLogger()
            androidContext(this@HashpotApplication)
            modules(
                listOf(
                    viewmodelModule,
                    networkModule,
                    databaseModule,
                    repositoryModule,
                    utilModule
                )
            )

            miningStopReceiver = MiningStopReceiver()
            val intentFilter = IntentFilter(
                MiningService.ACTION_STOP_SERVICE_AND_APP
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(miningStopReceiver, intentFilter, RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(miningStopReceiver, intentFilter)
            }
        }

    }

    override fun onTerminate() {

        // Unregister the MiningStopReceiver
        try {
            unregisterReceiver(miningStopReceiver)
        } catch (e: Exception) {
            // Receiver might already be unregistered
        }

        super.onTerminate()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bitcoin Lottery Mining"
            val descriptionText = "Channel for Bitcoin Lottery Mining Service"
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(MINING_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

private val networkModule = module {
    single<ConnectivityManager> {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}

private val databaseModule = module {

}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mining_preferences")

private val repositoryModule = module {
    single {
        MiningRepository(androidContext().dataStore)
    }
}

private val viewmodelModule = module {
    viewModel<ConfigViewModel> { ConfigViewModel(miningRepository = get()) }
    viewModel<StatsViewModel> { StatsViewModel(miningRepository = get()) }
}

private val utilModule = module {

}