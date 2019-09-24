package com.pushdy.handlers

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.google.gson.Gson
import com.pushdy.PDYConstant
import com.pushdy.Pushdy

open class PDYLifeCycleHandler : Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    private constructor()

    companion object {
        private var sharedInstance:PDYLifeCycleHandler? = null
        private var isInBackground = false
        var curActivity:Activity? = null

        fun listen(application:Application?) {
            if (sharedInstance == null) {
                sharedInstance = PDYLifeCycleHandler()
            }

            application?.registerActivityLifecycleCallbacks(sharedInstance!!)
            application?.registerComponentCallbacks(sharedInstance!!)
        }

    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        curActivity = activity
        Pushdy.getActivityLifeCycleDelegate()?.onActivityCreated(activity, savedInstanceState)
        val intent = activity?.intent
        processNotificationFromIntent(intent)
    }

    override fun onActivityStarted(activity: Activity?) {
        Pushdy.getActivityLifeCycleDelegate()?.onActivityStarted(activity)
    }

    override fun onActivityPaused(activity: Activity?) {
        Pushdy.getActivityLifeCycleDelegate()?.onActivityPaused(activity)
    }

    override fun onActivityResumed(activity: Activity?) {
        if (isInBackground) {
            isInBackground = false
        }
        Pushdy.getActivityLifeCycleDelegate()?.onActivityResumed(activity)
        val intent = activity?.intent
        processNotificationFromIntent(intent)
    }

    override fun onActivityStopped(activity: Activity?) {
        Pushdy.getActivityLifeCycleDelegate()?.onActivityStopped(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        Pushdy.getActivityLifeCycleDelegate()?.onActivitySaveInstanceState(activity, outState)
    }

    override fun onActivityDestroyed(activity: Activity?) {
        if (curActivity == activity) {
            curActivity = null
        }
        Pushdy.getActivityLifeCycleDelegate()?.onActivityDestroyed(activity)
    }

    override fun onLowMemory() {

    }

    override fun onTrimMemory(level: Int) {
        if(level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE){
            isInBackground = true
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {

    }

    fun processNotificationFromIntent(intent: Intent?) {
        if (intent != null) {
            val notificationStr = intent.getStringExtra("notification")
            val notification = Gson().fromJson(notificationStr, MutableMap::class.java)
            if (notification != null) {
                Pushdy.getDelegate()?.onNotificationOpened(notification as Map<String, Any>, PDYConstant.AppState.BACKGROUND)
                Pushdy.trackOpened(notification as Map<String, Any>)
                intent.removeExtra("notification")
            }
        }
    }
}
