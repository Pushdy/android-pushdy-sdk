package com.pushdy.example

import android.app.Application
import android.app.Notification
import com.pushdy.Pushdy

class ExampleApplication : Application(), Pushdy.PushdyDelegate {
    override fun readyForHandlingNotification(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNotificationReceived(notification: Map<String, Any>, fromState: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNotificationOpened(notification: Map<String, Any>, fromState: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRemoteNotificationRegistered(deviceToken: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRemoteNotificationFailedToRegister(error: Exception) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun customNotification(
        title: String,
        body: String,
        data: Map<String, Any>
    ): Notification? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate() {
        super.onCreate()
        val clientKey = ""
        Pushdy.initWith(this, clientKey, this)
    }


}