package com.github.luckyhash

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

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


val repositoryModule = module {

}

val viewmodelModule = module {

}

val utilModule = module {

}