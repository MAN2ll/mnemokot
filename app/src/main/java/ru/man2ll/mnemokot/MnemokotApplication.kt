package ru.man2ll.mnemokot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MnemokotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Здесь в будущем можно инициализировать логику WorkManager уведомлений
    }
}
