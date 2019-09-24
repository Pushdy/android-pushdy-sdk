package com.pushdy

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonElement
import com.pushdy.core.entities.PDYAttribute
import com.pushdy.core.entities.PDYNotification
import com.pushdy.core.entities.PDYParam
import com.pushdy.core.entities.PDYPlayer
import com.pushdy.core.ultilities.PDYDeviceInfo
import com.pushdy.handlers.PDYLifeCycleHandler
import com.pushdy.storages.PDYLocalData
import com.pushdy.views.PDYPushBannerActionInterface
import java.util.*
import kotlin.Exception
import kotlin.collections.HashMap

open class Pushdy {
    interface PushdyDelegate {
        fun readyForHandlingNotification() : Boolean
        fun onNotificationReceived(notification: Map<String, Any>, fromState: String)
        fun onNotificationOpened(notification: Map<String, Any>, fromState: String)
        fun onRemoteNotificationRegistered(deviceToken: String)
        fun onRemoteNotificationFailedToRegister(error:Exception)
//        fun onPlayerAdded(playerID:String)
//        fun onPlayerFailedToAdd(error:Exception)
//        fun onBeforeUpdatePlayer()
//        fun onPlayerEdited(playerID:String)
//        fun onPlayerFailedToEdit(playerID:String, error:Exception)
//        fun onNewSessionCreated(playerID:String)
//        fun onNewSessionFailedToCreate(playerID:String, error:Exception)
//        fun onNotificationTracked(notification: Any)
//        fun onNotificationFailedToTrack(notification: Any, error:java.lang.Exception)
//        fun onAttributesReceived(attributes:Any)
//        fun onAttributesFailedToReceive(error:Exception)
        fun customNotification(title:String, body:String, data: Map<String, Any>) : Notification?
    }

    interface PushdyActivityLifeCycleDelegate {
        fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?)
        fun onActivityStarted(activity: Activity?)
        fun onActivityResumed(activity: Activity?)
        fun onActivityPaused(activity: Activity?)
        fun onActivityStopped(activity: Activity?)
        fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?)
        fun onActivityDestroyed(activity: Activity?)
    }

    companion object {
        private var _deviceID:String? = null
        private var _clientKey:String? = null
        private var _delegate:PushdyDelegate? = null
        private var _context:Context? = null
        private var _creatingPlayer:Boolean = false
        private var _editingPlayer:Boolean = false
        private var _fetchingAttributes:Boolean = false
        private var _smallIcon:Int? = null
        private var _notificationChannel:String? = null
        private var _pendingNotifications:MutableList<Map<String, Any>> = mutableListOf()
        private var _customPushBannerView:View? = null
        private var _activityLifeCycleDelegate:PushdyActivityLifeCycleDelegate? = null
        private const val UPDATE_ATTRIBUTES_INTERVAL:Long = 5000
        private val TAG = "Pushdy"

        @JvmStatic
        open fun setDeviceID(deviceID:String) {
            _deviceID = deviceID
        }

        @JvmStatic
        fun initWith(context:Context, clientKey:String, delegate: PushdyDelegate?) {
            _clientKey = clientKey
            _delegate = delegate
            _context = context

            initialize()
            observeAttributesChanged()
        }

        @JvmStatic
        fun getDelegate() : PushdyDelegate? {
            return _delegate
        }

        @JvmStatic
        fun getActivityLifeCycleDelegate() : PushdyActivityLifeCycleDelegate? {
            return _activityLifeCycleDelegate
        }

        @JvmStatic
        fun setActivityLifeCycleDelegate(delegate:PushdyActivityLifeCycleDelegate?)  {
            _activityLifeCycleDelegate = delegate
        }

        @JvmStatic
        fun isNotificationEnabled() : Boolean {
            var enabled = false
            if (_context != null) {
                enabled = NotificationManagerCompat.from(_context!!).areNotificationsEnabled()
            }
            else {
                throw noContextWasSetException()
            }
            return enabled
        }

        @JvmStatic
        fun registerForRemoteNotification() {
            if (_context != null) {
                FirebaseMessaging.getInstance().isAutoInitEnabled = true
                initializeFirebaseInstanceID()
            }
            else {
                throw noContextWasSetException()
            }
        }

        fun getContext() : Context? {
            return _context
        }

        fun getApplicationPackageName() : String {
            if (_context != null) {
                return _context!!.applicationContext.packageName
            }
            else {
                throw noContextWasSetException()
            }
        }

        private fun initialize() {
            if (_context != null) {
                val deviceID = PDYDeviceInfo.deviceID(_context!!)
                Log.d(TAG, "DEVICE ID: $deviceID")
                PDYLocalData.initWith(_context!!)

                if (_context!! is Application) {
                    PDYLifeCycleHandler.listen(_context!! as Application)
                }

                val playerID = PDYLocalData.getPlayerID()
                if (playerID != null) {
                    createNewSession()
                }
                else {
                    createPlayer()
                }
            }
            else {
                throw noContextWasSetException()
            }
        }

        private fun initializeFirebaseInstanceID() {
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val exception:Exception = task.exception ?: Exception("[Pushdy] Failed to register remote notification")
                        getDelegate()?.onRemoteNotificationFailedToRegister(exception)
                        return@OnCompleteListener
                    }

                    // Get new Instance ID token
                    val token = task.result?.token
                    if (token != null) {
                        Log.d(TAG, "initializeFirebaseInstanceID token: ${token!!}")
                        PDYLocalData.setDeviceToken(token!!)
                        getDelegate()?.onRemoteNotificationRegistered(token!!)
                    }
                })
        }

        private fun observeAttributesChanged() {
            val timer = Timer()
            timer.scheduleAtFixedRate(object :TimerTask() {
                override fun run() {
                    updatePlayerIfNeeded()
                }
            }, 0, UPDATE_ATTRIBUTES_INTERVAL)
        }

        private fun updatePlayerIfNeeded() {
            if (!_creatingPlayer && !_editingPlayer) {
                var shouldUpdate = false
                if (PDYLocalData.attributesHasChanged()) {
                    shouldUpdate = true
                }

                if (shouldUpdate) {
                    if (PDYLocalData.isFetchedAttributes()) {
                        editPlayer()
                    }
                    else {
                        getAttributes({ response:JsonElement? ->
                            editPlayer()
                        }, { code:Int, message:String? ->
                            editPlayer()
                        })
                    }
                }
                else {
                    getAttributes({ response:JsonElement? ->
                        // Do no thing
                    }, { code:Int, message:String? ->
                        // Do no thing
                    })
                }
            }
        }

        @JvmStatic
        fun getDeviceID() : String? {
            return PDYLocalData.getDeviceID()
        }

        @JvmStatic
        fun getPlayerID() : String? {
            return PDYLocalData.getPlayerID()
        }

        @JvmStatic
        fun getDeviceToken() : String? {
            return PDYLocalData.getDeviceToken()
        }

        /**
         * Pushdy request
         */

        internal fun createNewSession() {
            val playerID = PDYLocalData.getPlayerID()
            if (playerID != null) {
                newSession(playerID!!, {response ->
                    Log.d(TAG, "createNewSession successfully")
                    null
                }, { code, message ->
                    Log.d(TAG, "createNewSession error: ${code}, messag:${message}")
                    null
                })
            }
        }

        internal fun createPlayer() {
            var hasTokenBefore = false
            val deviceToken = PDYLocalData.getDeviceToken()
            if (deviceToken != null) {
                hasTokenBefore = true
            }
            try {
                _creatingPlayer = true
                addPlayer(null, { response ->
                    _creatingPlayer = false
                    null
                }, { code:Int, message:String? ->
                    _creatingPlayer = false
                    null
                })
            }
            catch (exception:Exception) {
                _creatingPlayer = false
            }
        }

        internal fun addPlayer(params:HashMap<String, Any>?, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
            if (_context != null) {
                if (_clientKey != null) {
                    val player = PDYPlayer(_context!!, _clientKey!!, _deviceID)
                    player.add(params, completion, failure)
                } else {
                    throw noClientKeyException()
                }
            }
            else {
                throw noContextWasSetException()
            }
        }


        internal fun editPlayer() {
            val playerID = PDYLocalData.getPlayerID()
            if (playerID != null && !_editingPlayer) {
                val params = HashMap<String, Any>()
                val deviceToken = PDYLocalData.getDeviceToken()
                if (deviceToken != null) {
                    params.put(PDYParam.DeviceToken, deviceToken)
                }

                val changedAttributes = PDYLocalData.getChangedStack()
                if (changedAttributes != null) {
                    params.putAll(changedAttributes)
                }

                _editingPlayer = true
                editPlayer(playerID, params, { response ->
                    _editingPlayer = false
                    Log.d(TAG, "editPlayer successfully")
                    PDYLocalData.setLocalAttribValuesAfterSubmitted()
                    PDYLocalData.clearChangedStack()
                    null
                }, { code, message ->
                    _editingPlayer = false
                    Log.d(TAG, "editPlayer error: ${code}, messag:${message}")
                    null
                })
            }
        }

        internal fun trackOpened(notification: Map<String, Any>) {
            if (notification.containsKey(PDYConstant.Keys.NOTIFICATION_ID)) {
                val notificationID = notification[PDYConstant.Keys.NOTIFICATION_ID] as? String
                if (notificationID != null) {
                    trackOpened(notificationID, { response ->
                        Log.d(TAG, "trackOpened {$notificationID} successfully")
                        null
                    }, { code, message ->
                        Log.d(TAG, "trackOpened error: ${code}, messag:${message}")
                        null
                    })
                }
            }
        }


        internal fun editPlayer(playerID:String, params: HashMap<String, Any>, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
            if (_context != null) {
                if (_clientKey != null) {
                    val player = PDYPlayer(_context!!, _clientKey!!, _deviceID)
                    player.edit(playerID, params, completion, failure)
                } else {
                    throw noClientKeyException()
                }
            }
            else {
                throw noContextWasSetException()
            }
        }

        internal fun newSession(playerID: String, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
            if (_context != null) {
                if (_clientKey != null) {
                    val player = PDYPlayer(_context!!, _clientKey!!, _deviceID)
                    player.newSession(playerID, completion, failure)
                } else {
                    throw noClientKeyException()
                }
            }
            else {
                throw noContextWasSetException()
            }
        }

        internal fun trackOpened(notificationID: String, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
            if (_context != null) {
                if (_clientKey != null) {
                    val notification = PDYNotification(_context!!, _clientKey!!, _deviceID)
                    notification.trackOpened(notificationID, completion, failure)
                } else {
                    throw noClientKeyException()
                }
            }
            else {
                throw noContextWasSetException()
            }
        }

        internal fun getAttributes(completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
            if (_context != null) {
                if (_clientKey != null) {
                    _fetchingAttributes = true
                    val attribute = PDYAttribute(_context!!, _clientKey!!, _deviceID)
                    attribute.get({ response:JsonElement? ->
                        _fetchingAttributes = false
                        if (response != null) {
                            val jsonObject = response!!.asJsonObject
                            if (jsonObject.has("success")) {
                                val success = jsonObject.get("success").asBoolean
                                if (success && jsonObject.has("data")) {
                                    val attributes = jsonObject.getAsJsonArray("data")
                                    PDYLocalData.setAttributesSchema(attributes)
                                    PDYLocalData.setFetchedAttributes(true)
                                }
                            }
                        }
                        completion?.invoke(response)
                    }, { code:Int, message:String? ->
                        _fetchingAttributes = false
                        failure?.invoke(code, message)
                    })
                } else {
                    throw noClientKeyException()
                }
            }
            else {
                throw noContextWasSetException()
            }
        }


        /**
         * Exception && error handling
         */
        private fun noContextWasSetException() : Exception {
            return Exception("[Pushdy] No context was set!")
        }

        private fun noClientKeyException() : Exception {
            return Exception("[Pushdy] No client key was set!")
        }

        private fun valueTypeNotSupport() : Exception {
            return Exception("[Pushdy] value's type not supported")
        }


        /**
         * Custom notification
         */
        fun setSmallIcon(icon:Int) {
            _smallIcon = icon
        }

        fun getSmallIcon() : Int? {
            return _smallIcon
        }

        fun setNotificationChannel(channel:String) {
            _notificationChannel = channel
        }

        fun getNotificationChannel() : String? {
            return _notificationChannel
        }

        /**
         * Pending notifications
         */
        @JvmStatic
        fun getPendingNotification() : Map<String, Any>? {
            if (_pendingNotifications.size > 0) {
                return _pendingNotifications[_pendingNotifications.size-1]
            }
            return null
        }

        @JvmStatic
        fun getPendingNotifications() : List<Map<String, Any>> {
            return _pendingNotifications
        }

        @JvmStatic
        fun popPendingNotification() {
            val size = _pendingNotifications.size
            if (size > 0) {
                _pendingNotifications.removeAt(size-1)
            }
        }

        @JvmStatic
        fun removePendingNotification(notificationID: String) {
            var index = -1

            for (i in 0 until _pendingNotifications.size) {
                val item = _pendingNotifications[i]
                val idStr = item[PDYConstant.Keys.NOTIFICATION_ID]
                if (idStr == notificationID) {
                    index = i
                    break
                }

            }
            if (index >= 0 && index < _pendingNotifications.size) {
                _pendingNotifications.removeAt(index)
            }
        }

        @JvmStatic
        fun pushPendingNotification(notification: Map<String, Any>) {
            _pendingNotifications.add(notification)
        }

        @JvmStatic
        fun removePendingNotificationAt(index:Int) {
            val size = _pendingNotifications.size
            if (size > 0 && index >= 0 && index < size) {
                _pendingNotifications.removeAt(index)
            }
        }

        @JvmStatic
        fun clearPendingNotifications() {
            _pendingNotifications.clear()
        }

        /**
         * Set & push attribute
         */
        @JvmStatic
        fun setAttribute(name:String, value:Any) {
            setAttribute(name, value, false)
        }

        @JvmStatic
        fun setAttribute(name:String, value:Any, commitImmediately:Boolean) {
            val changed = PDYLocalData.isAttributeChanged(name, value)
            if (!changed) { return }

            var typeStr = ""
            if (value is Array<*>) {
                typeStr = PDYConstant.AttributeType.kArray
            }
            else if (value is String) {
                typeStr = PDYConstant.AttributeType.kString
            }
            else if (value is Boolean) {
                typeStr = PDYConstant.AttributeType.kBoolean
            }
            else if (value is Int || value is Float || value is Double || value is Long) {
                typeStr = PDYConstant.AttributeType.kNumber
            }

            if (PDYConstant.AttributeType.types().contains(typeStr)) {
                val currentValue = PDYLocalData.getAttributeValue(name)
                if (currentValue != null) {
                    PDYLocalData.setPrevAttributeValue(name, currentValue)
                }
                PDYLocalData.setAttributeValue(name, value)
                PDYLocalData.pushToChangedStack(name, value)

                if (commitImmediately) {
                    editPlayer()
                }
            }
            else {
                throw valueTypeNotSupport()
            }
        }

        @JvmStatic
        fun pushAttribute(name:String, value:Any) {
            pushAttribute(name, value, false)
        }

        @JvmStatic
        fun pushAttribute(name:String, value:Any, commitImmediately:Boolean) {
            var currentValues:Array<Any>?
            val values = PDYLocalData.getAttributeValue(name)
            if (values is Array<*>) {
                PDYLocalData.setPrevAttributeValue(name, values)
                var mutalbleValues = mutableListOf<Any>(values as Array<Any>)
                mutalbleValues.add(value)
                PDYLocalData.setAttributeValue(name, mutalbleValues)
                currentValues = mutalbleValues.toTypedArray()
            }
            else {
                currentValues = arrayOf(value)
                PDYLocalData.setAttributeValue(name, arrayOf(value))
            }

            if (currentValues != null) {
                PDYLocalData.pushToChangedStack(name, currentValues!!)
            }

            if (commitImmediately) {
                editPlayer()
            }
        }

        /**
         * In App Push Banner
         */
        fun setPushBannerAutoDismiss(autoDismiss:Boolean) {
            PDYLocalData.setPushBannerAutoDismiss(autoDismiss)
        }

        fun isPushBannerAutoDismiss() : Boolean {
            val autoDismiss = PDYLocalData.isPushBannerAutoDismiss()
            return autoDismiss
        }

        fun getPushBannerDismissDuration() : Float {
            val duration = PDYLocalData.getPushBannerDismissDuration()
            return duration
        }

        fun setPushBannerDismissDuration(duration:Float) {
            PDYLocalData.setPushBannerDismissDuration(duration)
        }

        internal fun getCustomPushBannerView() : View? {
            return _customPushBannerView
        }

        fun setCustomPushBanner(customView:View) {
            if (customView is PDYPushBannerActionInterface) {
                _customPushBannerView = customView
            }
            else {
                throw Exception("[Pushdy] Your custom view must implement PDYPushBannerActionInterface interface")
            }
        }

    }

}