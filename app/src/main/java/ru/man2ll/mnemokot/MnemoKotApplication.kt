package ru.man2ll.mnemokot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MnemoKotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}
