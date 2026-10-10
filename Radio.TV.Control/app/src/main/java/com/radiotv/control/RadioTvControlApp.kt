package com.radiotv.control

import android.app.Application
import com.radiotv.control.core.AndroidTvRemoteV2Transport
import com.radiotv.control.core.OtherTvRemoteController
import com.radiotv.control.core.AndroidTvVoiceInputController
import com.radiotv.control.core.GyroAirMouseController
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
                single { GyroAirMouseController(androidContext(), get()) }
                single { AndroidTvVoiceInputController(androidContext(), get()) }
                single { OtherTvRemoteController(androidContext()) }
            })
        }
    }
}
