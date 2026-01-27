package cn.com.omnimind.omnibot

import android.app.Application
import android.content.Context

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set

        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
