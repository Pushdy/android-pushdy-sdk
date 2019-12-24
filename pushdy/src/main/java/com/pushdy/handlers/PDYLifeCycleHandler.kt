package com.pushdy.handlers

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.pushdy.PDYConstant
import com.pushdy.Pushdy
import com.pushdy.core.entities.PDYParam
import org.json.JSONObject
import java.util.Base64

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
        Log.d("PDYLifeCycleHandler", "onActivityCreated: "+activity?.localClassName)
        curActivity = activity
        Pushdy.getActivityLifeCycleDelegate()?.onActivityCreated(activity, savedInstanceState)
        val ready = Pushdy.getDelegate()?.readyForHandlingNotification() ?: true
        val intent = activity?.intent
        if (ready) {
            Log.d("PDYLifeCycleHandler", "onActivityCreated: call processNotificationFromIntent")
            processNotificationFromIntent(intent)
        }
        else {
            val notificationStr = intent?.getStringExtra("pushdy_notification")
            if (notificationStr != null) {
                Log.d("PDYLifeCycleHandler", "onActivityCreated: push to pending notification: "+notificationStr)
                Pushdy.pushPendingNotification(notificationStr)
                intent?.removeExtra("pushdy_notification")
            }
            else {
                Log.d("PDYLifeCycleHandler", "onActivityCreated: no notification in intent")
            }
        }
    }

    override fun onActivityStarted(activity: Activity?) {
        Pushdy.getActivityLifeCycleDelegate()?.onActivityStarted(activity)
    }

    override fun onActivityPaused(activity: Activity?) {
        Pushdy.getActivityLifeCycleDelegate()?.onActivityPaused(activity)
    }

    override fun onActivityResumed(activity: Activity?) {
        Log.d("PDYLifeCycleHandler", "onActivityResumed: "+activity?.localClassName)
        curActivity = activity
        if (isInBackground) {
            isInBackground = false
        }
        Pushdy.getActivityLifeCycleDelegate()?.onActivityResumed(activity)
        val intent = activity?.intent
        val ready = Pushdy.getDelegate()?.readyForHandlingNotification() ?: true
        if (ready) {
            Log.d("PDYLifeCycleHandler", "onActivityResumed: call processNotificationFromIntent")
            processNotificationFromIntent(intent)
        }
        else {
            val notificationStr = intent?.getStringExtra("pushdy_notification")
            if (notificationStr != null) {
                Log.d("PDYLifeCycleHandler", "onActivityResumed: push to pending notification: "+notificationStr)
                Pushdy.pushPendingNotification(notificationStr)
                intent?.removeExtra("pushdy_notification")
            }
            else {
                Log.d("PDYLifeCycleHandler", "onActivityResumed: no notification in intent")
            }
        }
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

    fun bundleToMap(extras: Bundle) : MutableMap<String, Any> {
        val map:MutableMap<String, Any> = mutableMapOf()
        val ks = extras.keySet();
        val iterator = ks.iterator();
        while (iterator.hasNext()) {
            var key = iterator.next();
            try {
                map.put(key, extras.getString(key));
            } catch (e: java.lang.Exception){
            }
        }
        return map;
    }

    fun processNotificationFromIntent(intent: Intent?) {
        if (intent != null) {
            var notificationStr = intent.getStringExtra("pushdy_notification")
            var fromState = PDYConstant.AppState.ACTIVE
            if (notificationStr == null || notificationStr == ""){
                val notificationStrEnscript = intent.getStringExtra("_nms_payload")
                if (notificationStrEnscript != null){
                    notificationStr = String(Base64.getDecoder().decode(notificationStrEnscript), Charsets.UTF_8)
                    Log.d("PDYLifeCycleHandler", "processNotificationFromIntent: "+notificationStr)
                    fromState = PDYConstant.AppState.BACKGROUND
                }
            }
            if (notificationStr != null && notificationStr != "") {
                val notification = JSONObject(notificationStr)
                // Remove corresponding notification
                intent.removeExtra("pushdy_notification")
                val notificationID = notification.getJSONObject("data").getString(PDYConstant.Keys.NOTIFICATION_ID)
                if (notificationID != null) {
                    Pushdy.onNotificationOpened(notificationID, notificationStr, fromState)
                    Pushdy.removePendingNotification(notificationID)
                }
            }
        }
    }
}
