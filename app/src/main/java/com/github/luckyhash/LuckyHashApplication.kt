package com.github.luckyhash

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.github.luckyhash.domain.MiningRepository

class LuckyHashApplication: Application() {

    override fun onCreate() {
        super.onCreate()
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
}

val networkModule = module {
    single<ConnectivityManager> {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}

val databaseModule = module {

}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mining_preferences")

val repositoryModule = module {
    single {
        MiningRepository(androidContext().dataStore)
    }
}

val viewmodelModule = module {

}

val utilModule = module {

}