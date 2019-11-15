package com.pushdy.core.entities

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pushdy.core.network.PDYRequest

open class PDYNotification(val ctx: Context, clientKey : String, deviceID:String?) : PDYEntity(ctx, clientKey, deviceID) {
    override fun router(): String {
        return "/notification"
    }

    open fun trackOpened(playerID:String, notificationID:String, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequest {
        if (this.context != null) {
            val request = PDYRequest(context!!)
            var newParams: JsonObject?
            var mergedParams = HashMap<String, Any>()
            mergedParams.put("platform", "android")
            if (playerID != null) {
                mergedParams.put("player_id", playerID)
            }
            newParams = Gson().toJsonTree(mergedParams).asJsonObject
            request.put(this.url()+"/"+notificationID+"/track", this.defaultHeaders(), newParams, { response: JsonElement? ->
                completion?.invoke(response)
            }, { code: Int, message: String? ->
                failure?.invoke(code, message)
            })
            return request
        } else {
            throw this.noContextWasSetException()
        }
    }
}