package com.pushdy.handlers

import android.Manifest
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.pushdy.PDYConstant
import com.pushdy.Pushdy
import com.pushdy.utils.PDYUtil
import com.pushdy.views.PDYNotificationView
import com.pushdy.views.PDYPushBannerActionInterface
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

internal class PDYNotificationHandler {
    private constructor()
    companion object {
        fun process(title:String, body:String, image:String, data:Map<String, Any>, jsonData: String) {
            val context = Pushdy.getContext()
            if (context != null) {
                val visible = PDYUtil.isAppVisible(context!!, Pushdy.getApplicationPackageName())
                val fromState = if (visible) PDYConstant.AppState.ACTIVE else PDYConstant.AppState.BACKGROUND

                if (PDYUtil.isAppVisible(context!!, Pushdy.getApplicationPackageName()) && Pushdy.getBadgeOnForeground()) {
//                    sendBoardcast(context, title, body, data)
                    showInAppNotification(context, title, body, image, data, jsonData)
                }
                else {
                    Log.d("PDYNotificationHandler", "process(): prepare to notify")
                    notify(context!!, title, body, image, data, jsonData)
                }

                Log.d("PDYNotificationHandler", "process(): send onNotificationReceived to JS")
                Pushdy.getDelegate()?.onNotificationReceived(jsonData, fromState)
            }
        }

        fun notify(context:Context, title: String, body: String, image:String, data: Map<String, Any>, jsonData: String) {
            val curActivity = PDYLifeCycleHandler.curActivity
            // Log.d("PDYNotificationHandler", "notify().curActivity != null : ${curActivity != null}")


            var intentClass: Class<*>?
            if (curActivity == null) {
                intentClass = PDYUtil.getMainActivityClass(context)
                if (intentClass == null) {
                    Log.e("PDYNotificationHandler", "No activity class found for the notification")
                }
                // Log.d("PDYNotificationHandler", "notify().intentClass from getMainActivityClass: ${intentClass?.canonicalName}")
            } else {
                intentClass = curActivity.javaClass
                // Log.d("PDYNotificationHandler", "notify().intentClass from curActivity: ${intentClass?.canonicalName}")
            }


            if (intentClass != null) {

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                var notification = Pushdy.getDelegate()?.customNotification(title, body, image, data)
                if (notification == null) {
                    val intent = Intent(context, intentClass)

                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.putExtra("pushdy_notification", jsonData)

                    val defaultChannel =
                        Pushdy.getNotificationChannel() ?: "default_notification_channel"
                    val pendingIntent =
                        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val notificationBuilder = NotificationCompat.Builder(context, defaultChannel)
                    val smallIcon = Pushdy.getSmallIcon()
                    notificationBuilder.setSmallIcon(smallIcon!!)
                    notificationBuilder.setContentTitle(title)
                    notificationBuilder.setContentText(body)
                    notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    notificationBuilder.setAutoCancel(true)
                    notificationBuilder.setSound(soundUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel("default_channel", "default_channel", NotificationManager.IMPORTANCE_DEFAULT);
                        notificationManager.createNotificationChannel(channel);
                        notificationBuilder.setChannelId("default_channel");
                    }
                    val multiplier = getImageFactor(context.getResources())
                    val bitmap = getBitmapfromUrl(image)
                    if (bitmap != null) {
                        val bitmap2 = Bitmap.createScaledBitmap(bitmap, (bitmap.getWidth() * multiplier).toInt(), (bitmap.getHeight() * multiplier).toInt(), false)
                        notificationBuilder.setLargeIcon(bitmap2)
                    }
                    notificationBuilder.setContentIntent(pendingIntent)

                    val result = ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        notificationBuilder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                    }
                    notification = notificationBuilder.build()
                } else {
                }

                // Log.d("PDYNotificationHandler", "notificationManager.notify notification.title: $title")
                notificationManager.notify(Date().getTime().toInt(), notification)
            }
        }

        fun getBitmapfromUrl(imageUrl:String) : Bitmap? {
            try {
               val url = URL(imageUrl)
               val connection = url.openConnection() as HttpURLConnection
               connection.setDoInput(true)
               connection.connect()
               val input = connection.getInputStream()
               return BitmapFactory.decodeStream(input)
            } catch (e:Exception) {
               // TODO Auto-generated catch block
               e.printStackTrace()
               return null
            }
        }

        fun getImageFactor(r:Resources) : Float {
            val metrics = r.getDisplayMetrics()
            return metrics.density / 3f 
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

        fun startOriginActivity() {
            val activity = PDYLifeCycleHandler.rootActivity!!

            val context = Pushdy.getContext()!!
            // val activity = getActivity(context)
            // val am = context.getSystemService(Context.ACTIVITY_SERVICE)

            val intent = Intent(context, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("testField", "Bring back MainActivity...")
            }

            startActivity(context, intent, null)
        }

//        // How to get activity from context: https://stackoverflow.com/a/46205896/4984888
//        fun getActivity(context: Context?): Activity? {
//            if (context == null) {
//                return null
//            } else if (context is ContextWrapper) {
//                return context as? Activity ?: getActivity(context.baseContext)
//            }
//
//            return null
//        }

        fun showInAppNotification(context: Context, title: String, body:String, image:String, data: Map<String, Any>, jsonData: String) {
            val curActivity = PDYLifeCycleHandler.curActivity
            if (curActivity != null) {
                // Create in app banner view and add notification
                val bannerView:View = Pushdy.getCustomPushBannerView() ?: PDYNotificationView(context)
                val notification:MutableMap<String, Any> = mutableMapOf()
                notification.putAll(data)
                notification.put("title", title)
                notification.put("body", body)
                notification.put("image", image)

                if (bannerView is PDYPushBannerActionInterface) {
                    (bannerView as PDYPushBannerActionInterface).show(notification, onTap = {
                        Log.d("PDYNotificationHandler", "In App Banner Notification has tapped")

                        // Bring MainActivity (Pushdy is running on) to top
                        startOriginActivity()

                        Pushdy.onNotificationOpened(data[PDYConstant.Keys.NOTIFICATION_ID].toString(), jsonData, PDYConstant.AppState.ACTIVE)
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