package com.acuant.sampleapp

import android.app.Application


/**
 * Created by tapasbehera on 4/18/18.
 */
@Suppress("unused")
class AppInstance : Application() {
    private var mDefaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
//    private var refWatcher:RefWatcher? = null
    private val mCaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, ex ->
        mDefaultUncaughtExceptionHandler!!.uncaughtException(thread, ex)
    }
    companion object {
        lateinit var instance: AppInstance
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler)

    }
}