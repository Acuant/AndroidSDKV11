package com.acuant.sampleapp

import android.app.Application
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer

/**
 * Created by tapasbehera on 4/18/18.
 */
class AppInstance : Application() {
    private var mDefaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

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
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler);

    }

    override fun onTerminate() {
        super.onTerminate()
    }
}