package com.acuant.sampleapp

import android.app.Application
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.LeakCanary.refWatcher
import com.squareup.leakcanary.RefWatcher


/**
 * Created by tapasbehera on 4/18/18.
 */
class AppInstance : Application() {
    private var mDefaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private var refWatcher:RefWatcher? = null
    private val mCaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, ex ->
        mDefaultUncaughtExceptionHandler!!.uncaughtException(thread, ex)
    }
    companion object {
        lateinit var instance: AppInstance
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this.applicationContext)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        refWatcher = LeakCanary.install(this)

        instance = this
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler);

    }

    fun mustDie(obj: Any) {
        if (refWatcher != null) {
            refWatcher!!.watch(obj)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}