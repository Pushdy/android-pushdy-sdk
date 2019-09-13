package com.pushdy.utils

import android.app.ActivityManager
import android.content.Context

open class PDYUtil {
    private constructor()

    companion object {
        fun isAppVisible(context: Context, packageName:String) : Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processInfos = activityManager.runningAppProcesses
            var visible = false
            if (processInfos != null) {
                for (processInfo in processInfos) {
                    if (processInfo.processName == packageName
                        && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        visible = true
                        break
                    }
                }
            }
            return visible
        }
    }
}