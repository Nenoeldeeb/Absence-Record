package dev.nenoeldeeb.education.absencerecord.app

import android.app.Application

class Application : Application() {
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(applicationContext)
    }
}