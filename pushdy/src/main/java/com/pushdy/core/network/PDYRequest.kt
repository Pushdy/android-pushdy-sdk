package com.pushdy.core.network

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.JsonElement
import kotlin.collections.HashMap


class PDYRequest {
    var defaultTimeout: Int = 30 * 1000
    var timeout = defaultTimeout

    var context: Context? = null
    var queue:RequestQueue? = null

    constructor(context: Context) {
        this.context = context
        if (this.context != null) {
            this.queue = Volley.newRequestQueue(this.context)
        }
    }


    var defaultHeaders: HashMap<String, String> = hashMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json"
    )

    fun request(url: String, method: Int, headers:Map<String, String>?, params:JsonElement?, completion:(response:JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest  {
        val request = object : PDYJsonRequest(method, url, params,
            Response.Listener { response ->
                completion(response)
            },
            Response.ErrorListener { error ->
                if (error.networkResponse != null) {
                    failure(error.networkResponse.statusCode, error.networkResponse.toString())
                }
                else {
                    failure(-1, error.localizedMessage)
                }

            }
        ) {
//            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                var headerParams: HashMap<String, String>?
                if (super.getHeaders() != null) {
                    headerParams = HashMap(super.getHeaders())
                }
                else {
                    headerParams = HashMap()
                }

                headerParams.putAll(defaultHeaders)

                if (headers != null) {
                    headerParams.putAll(headers)
                }
                return headerParams
            }
        }

        this.queue?.add(request)
        return request
    }

    fun get(url: String, headers:Map<String, String>?, params:JsonElement?, completion:(response:JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.GET, headers, params, completion, failure)
    }

    fun post(url: String, headers:Map<String, String>?, params:JsonElement?, completion:(response:JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.POST, headers, params, completion, failure)
    }

    fun put(url: String, headers:Map<String, String>?, params:JsonElement?, completion:(response:JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.PUT, headers, params, completion, failure)
    }

    fun delete(url: String, headers:Map<String, String>?, params:JsonElement?, completion:(response:JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.DELETE, headers, params, completion, failure)
    }
}