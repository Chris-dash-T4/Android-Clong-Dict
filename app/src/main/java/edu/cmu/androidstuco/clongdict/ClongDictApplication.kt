package edu.cmu.androidstuco.clongdict

import android.app.Activity
import android.app.Application
import android.os.Bundle
import edu.cmu.androidstuco.clongdict.util.Toaster

class ClongDictApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Toaster.bindToastContext(this)
        registerActivityLifecycleCallbacks(ToasterActivityCallbacks())
    }

    private class ToasterActivityCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {
            Toaster.bindSnackbarActivity(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            Toaster.clearSnackbarActivity(activity)
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }
}
