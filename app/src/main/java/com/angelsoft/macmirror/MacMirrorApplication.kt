package com.angelsoft.macmirror

import android.app.Application
import com.angelsoft.macmirror.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MacMirrorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MacMirrorApplication)
            modules(appModule)
        }
    }
}
