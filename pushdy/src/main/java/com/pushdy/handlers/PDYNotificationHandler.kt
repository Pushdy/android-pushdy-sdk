package com.pushdy.handlers

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.pushdy.PDYConstant
import com.pushdy.Pushdy
import com.pushdy.utils.PDYUtil
import com.pushdy.views.PDYNotificationView
import com.pushdy.views.PDYPushBannerActionInterface

internal class PDYNotificationHandler {
    private constructor()
    companion object {
        fun process(title:String, body:String, data:Map<String, Any>) {
            val context = Pushdy.getContext()
            if (context != null) {
                val visible = PDYUtil.isAppVisible(context!!, Pushdy.getApplicationPackageName())
                val fromState = if (visible) PDYConstant.AppState.ACTIVE else PDYConstant.AppState.BACKGROUND
                Pushdy.getDelegate()?.onNotificationReceived(data, fromState)

                if (PDYUtil.isAppVisible(context!!, Pushdy.getApplicationPackageName())) {
//                    sendBoardcast(context, title, body, data)
                    showInAppNotification(context, title, body, data)
                }
                else {
                    notify(context!!, title, body, data)
                }
            }
        }

        fun notify(context:Context, title: String, body: String, data: Map<String, Any>) {
            val curActivity = PDYLifeCycleHandler.curActivity
            if (curActivity != null) {
                var notification = Pushdy.getDelegate()?.customNotification(title, body, data)
                if (notification == null) {
                    val intent = Intent(context, curActivity.javaClass)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                    val modifiedNotification:MutableMap<String, Any> = mutableMapOf()
                    modifiedNotification.putAll(data)
                    modifiedNotification.put("title", title)
                    modifiedNotification.put("body", body)
                    val notificationStr = Gson().toJson(modifiedNotification, MutableMap::class.java)
                    intent.putExtra("notification", notificationStr)


                    val defaultChannel =
                        Pushdy.getNotificationChannel() ?: "default_notification_channel"
                    val pendingIntent =
                        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val notificationBuilder = NotificationCompat.Builder(context, defaultChannel)
                    val smallIcon = Pushdy.getSmallIcon()
                    if (smallIcon != null) {
                        notificationBuilder.setSmallIcon(smallIcon!!)
                    }
                    notificationBuilder.setContentTitle(title)
                    notificationBuilder.setContentText(body)
                    notificationBuilder.setAutoCancel(true)
                    notificationBuilder.setSound(soundUri)
                    notificationBuilder.setContentIntent(pendingIntent)

                    val result = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        notificationBuilder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                    }
                    notification = notificationBuilder.build()
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(0, notification)
            }
        }

//        fun sendBoardcast(context: Context, title: String, body: String, data: Map<String, String>) {
//            val intent = Intent(PDYConstant.Keys.IN_APP_PUSH_BOARDCAST)
//            intent.putExtra("title", title)
//            intent.putExtra("body", body)
//            val bundle = Bundle()
//            for (entry in data.entries) {
//                bundle.putString(entry.key, entry.value)
//            }
//            intent.putExtra("data", bundle)
//
//            LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(intent)
//        }

        fun showInAppNotification(context: Context, title: String, body:String, data: Map<String, Any>) {
            val curActivity = PDYLifeCycleHandler.curActivity
            if (curActivity != null) {
                // Create in app banner view and add notification
                val bannerView:View = Pushdy.getCustomPushBannerView() ?: PDYNotificationView(context)
                val notification:MutableMap<String, Any> = mutableMapOf()
                notification.putAll(data)
                notification.put("title", title)
                notification.put("body", body)

                if (bannerView is PDYPushBannerActionInterface) {
                    (bannerView as PDYPushBannerActionInterface).show(notification, onTap = {
                        Pushdy.getDelegate()?.onNotificationOpened(notification, PDYConstant.AppState.ACTIVE)
                        Pushdy.trackOpened(notification)
                        null
                    })
                }

                // Add and show banner
                var viewAdded = false
                if (curActivity.actionBar != null && curActivity.actionBar!!.isShowing) {
                    val actionViewResId = Resources.getSystem().getIdentifier(
                        "action_bar_content", "id", Pushdy.getApplicationPackageName())
                    if (actionViewResId > 0) {
                        val actionBarView = curActivity.findViewById<FrameLayout>(actionViewResId)
                        if (actionBarView != null) {
                            curActivity.runOnUiThread {
                                actionBarView.addView(bannerView)
                            }
                            viewAdded = true
                        }
                    }
                }

                if (!viewAdded) {
                    val viewGroup = curActivity?.window?.decorView?.rootView as ViewGroup
                    if (viewGroup != null) {
                        curActivity.runOnUiThread {
                            viewGroup?.addView(bannerView)
                        }
                    }
                    else {
                        val rootView = curActivity.findViewById<FrameLayout>(android.R.id.content)
                        if (rootView != null) {
                            curActivity.runOnUiThread {
                                rootView.addView(bannerView)
                            }

                        }
                    }
                }
            }
        }


    }
}