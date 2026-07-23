package com.iyes.dacpressuremanager

import android.app.Application

class DacApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }
}

