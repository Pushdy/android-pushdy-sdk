package com.pushdy.core.entities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
//import com.pushdy.core.network.PDYRequest
import com.pushdy.core.network.PDYRequestSingleton


open class PDYPlayer(val ctx: Context, clientKey : String, deviceID:String?) : PDYEntity(ctx, clientKey, deviceID) {
    override fun router(): String {
        return "/player"
    }

    open fun add(params:HashMap<String, Any>?, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequestSingleton {
        if (this.context != null) {
            val request = PDYRequestSingleton.getInstance(context!!)
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

    open fun edit(playerID:String, params:HashMap<String, Any>, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequestSingleton {
        if (this.context != null) {
            // val request = PDYRequest(context!!)
            val request = PDYRequestSingleton.getInstance(context!!)

            /*
            This test code is used to reproduce the error:
            Throwing OutOfMemoryError "pthread_create (1040KB stack) failed: Try again"
            1. Use PDYRequest to reproduce the exception
            2. Do some action that edit player again and again
            3. There're some chance to catch above exception
            4. Use PDYRequestSingleton to avoid exception

             */
//            if (System.currentTimeMillis() % 10 > 3) {
//                Log.d("PDYPlayer", "Start PDYRequest Stress test")
//                for (i in 1..100) {
//                    // var r = PDYRequest(context!!)
//                    var r = PDYRequestSingleton.getInstance(context!!)
//                }
//            }


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
            Log.d("PDYPlayer", "edit params: " + params.toString())
            Log.d("PDYPlayer", "edit newParams: " + newParams.toString())
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

    open fun newSession(playerID: String, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequestSingleton {
        if (this.context != null) {
            val request = PDYRequestSingleton.getInstance(context!!)
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

    open fun trackOpened(playerID:String, notificationIDs: List<String>, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequestSingleton {
        if (this.context != null) {
            if (notificationIDs.isEmpty()) {
                throw Exception("PDYNotification: notificationIDs list is empty")
            }
            if (playerID.isBlank()) {
                throw Exception("PDYNotification: playerID isBlank")
            }

            val request = PDYRequestSingleton.getInstance(context!!)
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