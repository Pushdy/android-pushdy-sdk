package com.pushdy.core.network

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.gson.JsonElement
import kotlin.collections.HashMap


class PDYRequestSingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: PDYRequestSingleton? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: PDYRequestSingleton(context).also {
                INSTANCE = it
            }
        }
    }
    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }



    var defaultHeaders: HashMap<String, String> = hashMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json"
    )


    fun request(url: String, method: Int, headers:Map<String, String>?, params: JsonElement?, completion:(response: JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest  {
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

        this.addToRequestQueue(request)

        return request
    }

    /**
     * Usage:
     *
     *      val request = PDYRequestSingleton.getInstance(context).get(...)
     *      val request = PDYRequestSingleton.getInstance(context).put(...)
     *      val request = PDYRequestSingleton.getInstance(context).post(...)
     *      val request = PDYRequestSingleton.getInstance(context).delete(...)
     *
     */
    fun get(url: String, headers:Map<String, String>?, params: JsonElement?, completion:(response: JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.GET, headers, params, completion, failure)
    }

    fun post(url: String, headers:Map<String, String>?, params: JsonElement?, completion:(response: JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.POST, headers, params, completion, failure)
    }

    fun put(url: String, headers:Map<String, String>?, params: JsonElement?, completion:(response: JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.PUT, headers, params, completion, failure)
    }

    fun delete(url: String, headers:Map<String, String>?, params: JsonElement?, completion:(response: JsonElement?) -> Unit?, failure:(code:Int, message:String?) -> Unit?) : PDYJsonRequest {
        return this.request(url, Request.Method.DELETE, headers, params, completion, failure)
    }
}
