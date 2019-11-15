package com.pushdy.core.entities

import android.content.Context
import com.pushdy.core.PDYConfig
import com.pushdy.core.ultilities.PDYDeviceInfo
import com.pushdy.core.ultilities.PDYStorage
import kotlin.collections.HashMap

open class PDYEntity {
    var clientKey:String?
    var deviceID:String?
    var context:Context?

    constructor(context: Context, clientKey : String, deviceID:String?) {
        this.context = context
        this.clientKey = clientKey
        this.deviceID = deviceID
    }

    open fun url() : String {
        return this.baseUrl()+this.router()
    }

    open fun baseUrl() : String {
/*        var urlStr = ""
        context?.let { ctx -> {
            val bundle =  ctx.packageManager.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA).metaData
            urlStr = bundle.getString("BASE_URL", null)
        } }*/
        return PDYConfig.BASE_URL
    }

    open fun router() : String {
        return ""
    }

    open fun defaultHeaders() : HashMap<String, String>? {
        var headersParams:HashMap<String, String>? = null
        this.clientKey?.let {
            headersParams = hashMapOf(
                PDYParam.ClientKey to this.clientKey!!
            )
        }
        return headersParams
    }

    open fun defaultParams() : HashMap<String, Any>? {
        var params:HashMap<String, Any>? = null
        this.context?.let { ctx ->
            params = hashMapOf(
                PDYParam.AppVersion to PDYDeviceInfo.appVersion(ctx) as Any,
                PDYParam.DeviceModel to PDYDeviceInfo.deviceModel() as Any,
                PDYParam.DeviceType to PDYDeviceInfo.deviceType() as Any,
                PDYParam.DeviceOS to PDYDeviceInfo.systemVersion() as Any,
                PDYParam.Platform to PDYDeviceInfo.platform() as Any,
                PDYParam.DeviceID to (this.deviceID ?: PDYDeviceInfo.deviceID(ctx)) as Any,
                PDYParam.Language to PDYDeviceInfo.language() as Any,
                PDYParam.Country to PDYDeviceInfo.country() as Any
            )
        }

        return params
    }

    fun noContextWasSetException() : Exception {
        return Exception("No context was set!")
    }

}