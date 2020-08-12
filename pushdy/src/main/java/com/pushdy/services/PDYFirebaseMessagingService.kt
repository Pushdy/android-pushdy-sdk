package com.pushdy.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pushdy.Pushdy
import com.pushdy.core.entities.PDYParam
import com.pushdy.handlers.PDYNotificationHandler
import com.pushdy.storages.PDYLocalData
import com.pushdy.views.PDYNotificationView
import com.google.gson.Gson
import android.util.Base64

open class PDYFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "PDYFCMService"

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "onMessageReceived HAS CALLED with msg type: " + message.messageType)

        // Process received message
        val data:Map<String, Any> = message.data
        val n = message.notification


        if (data != null) {
            // Remember to fallback into data message, instead of notification message
            val title = n?.title ?: data.get("title").toString()
            val body = n?.body ?: data.get("body").toString()
            var image = n?.imageUrl?.toString() ?: ""

            // Fall back to custom media_key
            if (image == "" || image == "null"){
                Log.d(TAG, PDYNotificationView.getCustomMediaKey())
                val media_key = PDYNotificationView.getCustomMediaKey()
                image = data.get(media_key).toString()
                if (image == null || image == "null"){
                    image = data.get("image").toString()
                }
            }

            Log.d(TAG, "onMessageReceived title: $title, body: $body, image: $image")
            Log.d(TAG, "data: $data")
            // Check ready state
            var ready = true
            val delegate = Pushdy.getDelegate()
            if (delegate != null && delegate!!.readyForHandlingNotification()) {
                ready = delegate!!.readyForHandlingNotification()
            }

            val nmsPayload = String(Base64.decode(data.get("_nms_payload")!!.toString(), Base64.NO_WRAP))
            if (ready) { // Process immediately
                PDYNotificationHandler.process(title, body, image, data, nmsPayload)
            }
            else { // Push notification to pending stack
                Pushdy.pushPendingNotification(nmsPayload)
            }
        }
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: $token")
        // Check is new token or not
        val oldToken = PDYLocalData.getDeviceToken()
        var notEqual = false
        if (oldToken != null) {
            notEqual = oldToken != token
        }
        else {
            notEqual = true
        }


        PDYLocalData.setDeviceToken(token)


        if (notEqual) {
            // Push to change stack or edit player immediately
            PDYLocalData.pushToChangedStack(PDYParam.DeviceToken, token)
            Pushdy.editPlayer()
        }
    }
}