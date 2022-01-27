package com.pushdy.core.entities

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pushdy.core.network.PDYRequestSingleton

open class PDYNotification(val ctx: Context, clientKey : String, deviceID:String?) : PDYEntity(ctx, clientKey, deviceID) {
    override fun router(): String {
        return "/notification"
    }

    /**
     * This is /notification/:id/track for single notification tracking
     * For batch tracking, please use /player/:id/track on PDYPlayer instead
     */
    open fun trackOpened(playerID:String, notificationID:String, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequestSingleton {
        if (this.context != null) {
            val request = PDYRequestSingleton.getInstance(context!!)
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