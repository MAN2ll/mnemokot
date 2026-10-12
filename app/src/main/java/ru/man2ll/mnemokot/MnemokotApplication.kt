package ru.man2ll.mnemokot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MnemokotApplication : Application() {
    
    // СТАТИЧЕСКИЙ БЛОК: Загружается самым первым при старте процесса ОС Android
    init {
        System.loadLibrary("sqlcipher")
    }

    override fun onCreate() {
        super.onCreate()
    }
}
