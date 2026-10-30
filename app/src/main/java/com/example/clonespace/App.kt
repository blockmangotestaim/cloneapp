package com.example.clonespace

import android.app.Application
import android.content.Context

class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        EngineBridge.attach(base)
    }

    override fun onCreate() {
        super.onCreate()
        EngineBridge.create()
    }
}
