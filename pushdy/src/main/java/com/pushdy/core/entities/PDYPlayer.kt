package com.pushdy.core.entities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pushdy.core.network.PDYRequest

open class PDYPlayer(val ctx: Context, clientKey : String, deviceID:String?) : PDYEntity(ctx, clientKey, deviceID) {
    override fun router(): String {
        return "/player"
    }

    open fun add(params:HashMap<String, Any>?, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequest {
        if (this.context != null) {
            val request = PDYRequest(context!!)
            val gson = Gson()
            var newParams:JsonObject?
            if (params != null) {
                var mergedParams = HashMap<String, Any>(this.defaultParams())
                mergedParams.putAll(params!!)
                newParams = gson.toJsonTree(mergedParams).asJsonObject
            }
            else {
                newParams = gson.toJsonTree(this.defaultParams()).asJsonObject
            }

            Log.d("PDYPlayer", "add params: "+params.toString())
            Log.d("PDYPlayer", "add newParams: "+newParams.toString())

            request.post(this.url(), this.defaultHeaders(), newParams, { response: JsonElement? ->
                completion?.invoke(response)
            }, { code:Int, message:String? ->
                failure?.invoke(code, message)
            })
            return request
        }
        else {
            throw this.noContextWasSetException()
        }
    }

    open fun edit(playerID:String, params:HashMap<String, Any>, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequest {
        if (this.context != null) {
            val request = PDYRequest(context!!)
            val gson = Gson()
            var newParams:JsonObject?
            if (params != null) {
                var mergedParams = HashMap<String, Any>(this.defaultParams())
                mergedParams.putAll(params!!)
                newParams = gson.toJsonTree(mergedParams).asJsonObject
            }
            else {
                newParams = gson.toJsonTree(this.defaultParams()).asJsonObject
            }
            Log.d("PDYPlayer", "edit params: "+params.toString())
            Log.d("PDYPlayer", "edit newParams: "+newParams.toString())
            request.put(this.url()+"/"+playerID, this.defaultHeaders(), newParams, { response:JsonElement? ->
                completion?.invoke(response)
            }, { code:Int, message:String? ->
                failure?.invoke(code, message)
            })
            return request
        }
        else {
            throw this.noContextWasSetException()
        }
    }

    open fun newSession(playerID: String, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequest {
        if (this.context != null) {
            val request = PDYRequest(context!!)
            request.post(this.url()+"/"+playerID+"/on_session", this.defaultHeaders(), null, { response:JsonElement? ->
                completion?.invoke(response)
            }, { code:Int, message:String? ->
                failure?.invoke(code, message)
            })
            return request
        }
        else {
            throw this.noContextWasSetException()
        }
    }

    open fun trackOpened(playerID:String, notificationIDs: List<String>, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequest {
        if (this.context != null) {
            if (notificationIDs.isEmpty()) {
                throw Exception("PDYNotification: notificationIDs list is empty")
            }
            if (playerID.isBlank()) {
                throw Exception("PDYNotification: playerID isBlank")
            }

            val request = PDYRequest(context!!)
            var newParams: JsonObject?
            var mergedParams = HashMap<String, Any>()
            mergedParams.put("platform", "android")
            mergedParams.put("notifications", notificationIDs)
            newParams = Gson().toJsonTree(mergedParams).asJsonObject

            val url = "${this.url()}/$playerID/track"
            request.put(url, this.defaultHeaders(), newParams, { response: JsonElement? ->
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