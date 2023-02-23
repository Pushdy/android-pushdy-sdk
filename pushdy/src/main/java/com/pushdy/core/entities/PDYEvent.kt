package com.pushdy.core.entities

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pushdy.core.network.PDYRequestSingleton
import com.pushdy.storages.PDYLocalData


open class PDYEvent(val ctx: Context, clientKey : String, playerID: String) : PDYEntity(ctx, clientKey, playerID) {
    val DEBUG = false
    val TAG: String = "PDYEvent"
    var playerID: String = playerID
    override fun router(): String {
        return "/track"
    }



    /**
     * Pushdy Event Tracking.
     * track Event with params with queueing or immediate
     * @param eventName: String -> event name
     * @param params: HashMap<String, Any> -> event params
     * @param immediate: Boolean = false (default) -> push to server immediately
     */
    open fun trackEvent(eventName: String, params: HashMap<String, Any>, immediate: Boolean, completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
        // log
        if (DEBUG) Log.d(TAG, "trackEvent: $eventName, $params, $immediate")

        if (this.context != null) {
            val request = PDYRequestSingleton.getInstance(context!!)
            var event = HashMap<String, Any>()

            event["event"] = eventName
            event["properties"] = params
            event["created_at"] = System.currentTimeMillis() /1000
            event["player_id"] = this.playerID
            val pendingEvents = PDYLocalData.getPendingTrackEvents(999)
            pendingEvents.add(event);
            if (immediate) {
                pushPendingEvents(completion, failure)
            } else {
                PDYLocalData.initWith(this.context!!)
                PDYLocalData.setPendingTrackEvents(pendingEvents)
            }
        } else {
            if (DEBUG) Log.d(TAG, "trackEvent: no context was set");
        }
    }


    /**
     * push all pending events to server: Can up to 50 events per request
     * @param completion: ((response:JsonElement?) -> Unit?)? -> completion callback
     * @param failure: ((code:Int, message:String?) -> Unit?)? -> failure callback
     * @return PDYRequestSingleton
     * @see PDYRequestSingleton
     *
     */
    open fun pushPendingEvents(completion:((response:JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) : PDYRequestSingleton? {
        if (DEBUG) Log.d(TAG, "pushPendingEvents requested")
        if (this.context != null) {
            PDYLocalData.initWith(this.context!!)
            val applicationId = PDYLocalData.getApplicationId()
            val events = PDYLocalData.getPendingTrackEvents(50)

            if (events.size == 0) {
                if (DEBUG) Log.d(TAG, "pushPendingEvents --> no pending events")
                return null
            }

            if (applicationId != null) {
                val request = PDYRequestSingleton.getInstance(context!!)
                var newParams: JsonObject?
                var mergedParams = HashMap<String, Any>()
                mergedParams["platform"] = "android"
                mergedParams["events"] = Gson().toJsonTree(events)
                mergedParams["application_id"] = applicationId
                newParams = Gson().toJsonTree(mergedParams).asJsonObject
                if (DEBUG) Log.d(
                    TAG,
                    "pushPendingEvents --> newParams: $newParams url = ${this.url()}"
                )
                request.post(
                    this.url(),
                    this.defaultHeaders(),
                    newParams,
                    { response: JsonElement? ->
                        if (response != null) {
                            if (DEBUG) Log.d(TAG, "pushPendingEvents --> response: $response")
                            PDYLocalData.removePendingTrackEvents(50)
                        }
                        completion?.invoke(response)
                    },
                    { code: Int, message: String? ->
                        if (DEBUG) Log.d(
                            TAG,
                            "pushPendingEvents --> onFailure --> code: ${code} message: ${message}"
                        )
                        failure?.invoke(code, message)
                    })
                return request
            } else {
                if (DEBUG) Log.e(TAG, "pushPendingEvents --> applicationId is null, please set it first!")
                return null
            }
        } else {
            if (DEBUG) Log.e(TAG, "pushPendingEvents --> context is null, please set it first!")
            return null;
        }
    }
}