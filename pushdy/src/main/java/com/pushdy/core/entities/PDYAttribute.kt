package com.pushdy.core.entities

import android.content.Context
import com.google.gson.JsonElement
import com.pushdy.core.network.PDYRequestSingleton

open class PDYAttribute(val ctx: Context, clientKey : String, deviceID:String?) : PDYEntity(ctx, clientKey, deviceID) {
    override fun router(): String {
        return "/attribute"
    }

    open fun get(completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequestSingleton {
        if (this.context != null) {
            val request = PDYRequestSingleton.getInstance(context!!)
            request.get(this.url(), this.defaultHeaders(), null, { response:JsonElement? ->
                completion?.invoke(response)
            }) { code: Int, message: String? ->
                failure?.invoke(code, message)
            }
            return request
        }
        else {
            throw this.noContextWasSetException()
        }
    }
}