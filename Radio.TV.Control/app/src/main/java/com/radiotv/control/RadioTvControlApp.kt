package com.radiotv.control

import android.app.Application
import com.radiotv.control.core.AndroidTvRemoteV2Transport
import com.radiotv.control.core.BluetoothHidController
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class RadioTvControlApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RadioTvControlApp)
            modules(module {
                single { AndroidTvRemoteV2Transport(androidContext()) }
                single { BluetoothHidController(androidContext()) }
            })
        }
    }
}
