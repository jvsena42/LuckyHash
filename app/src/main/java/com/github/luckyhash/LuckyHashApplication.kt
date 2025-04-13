package com.github.luckyhash

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.luckyhash.domain.MiningRepository
import com.github.luckyhash.ui.screens.config.ConfigViewModel
import com.github.luckyhash.ui.screens.stats.StatsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class LuckyHashApplication : Application() {

    companion object {
        const val MINING_CHANNEL_ID = "mining_notification_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startKoin {
            androidLogger()
            androidContext(this@LuckyHashApplication)
            modules(
                listOf(
                    viewmodelModule,
                    networkModule,
                    databaseModule,
                    repositoryModule,
                    utilModule
                )
            )
        }
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