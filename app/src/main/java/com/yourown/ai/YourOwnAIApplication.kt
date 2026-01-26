package com.yourown.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YourOwnAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
