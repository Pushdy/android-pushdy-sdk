package com.pushdy.core.ultilities

import android.content.Context
import android.os.Build
import java.util.*

open class PDYDeviceInfo {
    companion object {
        fun deviceID(context: Context) : String {
            return PDYSystemUtil.getUUID(context)
        }

        fun deviceType() : String {
            return "android"
        }

        fun platform()  : String {
            return "android"
        }

        fun appVersion(context:Context) : String {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return info.versionName ?: "0.0.1"
        }

        fun systemVersion() : String {
            return Build.VERSION.RELEASE
        }

        fun deviceModel() : String {
            val manufacture:String = Build.MANUFACTURER
            val model:String = Build.MODEL
            if (model.startsWith(manufacture)) {
                return model
            }
            return "$manufacture $model"
        }

        fun language() : String {
            return Locale.getDefault().language
        }

        fun country() : String {
            return Locale.getDefault().country
        }
    }
}