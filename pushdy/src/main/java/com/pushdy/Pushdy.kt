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
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pushdy.core.entities.*
import com.pushdy.core.ultilities.PDYDeviceInfo
import com.pushdy.handlers.PDYLifeCycleHandler
import com.pushdy.handlers.PDYNotificationHandler
import com.pushdy.storages.PDYLocalData
import com.pushdy.views.PDYNotificationView
import com.pushdy.views.PDYPushBannerActionInterface
import java.util.*
import kotlin.Exception
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

open class Pushdy {
    interface PushdyDelegate {
        fun readyForHandlingNotification() : Boolean
        fun onNotificationReceived(notification: String, fromState: String)
        fun onNotificationOpened(notification: String, fromState: String)
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
        fun customNotification(title:String, body:String, image: String, data: Map<String, Any>) : Notification?
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
        private var _badge_on_foreground:Boolean = false
        private var _notificationChannel:String? = null
        private var _last_notification_id:String? = null
        private var _pendingNotifications:MutableList<String> = mutableListOf()
        private var _customPushBannerView:View? = null
        private var _activityLifeCycleDelegate:PushdyActivityLifeCycleDelegate? = null
        private const val UPDATE_ATTRIBUTES_INTERVAL:Long = 5000
        private val TAG = "Pushdy"

        /**
         * opened notification ids was not track immediately after user open a notification.
         * It must be saved here and push by batch later by schedule.
         * Duplication items support + preserve the ordering
         */
        private var pendingTrackingOpenedTimer: Timer = Timer("TrackingOpenedTimer", false)
        private var pendingTrackingOpenedTimerTask: TimerTask? = null
        private var pendingTrackingOpenedItems: MutableList<String> = mutableListOf()


        @JvmStatic
        open fun setDeviceID(deviceID:String) {
            var check = false
            if (_deviceID == "unexpecteddeviceid"){
                check = true
            }
            _deviceID = deviceID

            Log.d(TAG, "setDeviceId: _deviceID: $_deviceID")

            if (check){
                // Create player and run Pushdy from now on
                onSession(true)
            }
        }

        @JvmStatic
        open fun setNullDeviceID() {
            _deviceID = "unexpecteddeviceid"
        }

        @JvmStatic
        fun registerActivityLifecycle(_context:Context) {
            if (_context!! is Application) {
                PDYLifeCycleHandler.listen(_context!! as Application)
            }
        }

        @JvmStatic
        fun initWith(context:Context, clientKey:String, delegate: PushdyDelegate?) {
            _clientKey = clientKey
            _delegate = delegate
            _context = context

            initialize()
            observeAttributesChanged()
            restoreDataFromStorage()
        }

        @JvmStatic
        fun initWith(context:Context, clientKey:String, delegate: PushdyDelegate?, smallIcon: Int?) {
            _clientKey = clientKey
            _delegate = delegate
            _context = context
            _smallIcon = smallIcon

            initialize()
            observeAttributesChanged()
            restoreDataFromStorage()
        }

        @JvmStatic
        fun restoreDataFromStorage() {
            restorePendingTrackingOpenedItems()
        }

        @JvmStatic
        fun onNotificationOpened(notificationID: String, notification: String, fromState: String) {
            Log.d(TAG, "onNotificationOpened HAS CALLED")
            if (notificationID == _last_notification_id){
                Log.d(TAG, "onNotificationOpened: Skip because this noti was opened once before: " + notificationID)
                return
            }

            _last_notification_id = notificationID

            try {
                getDelegate()?.onNotificationOpened(notification, fromState)
            } catch (e: Exception) {

            }

            /**
             * Issue: No player ID when JS was not ready, happen at the first time open app, right after app installed.
             */
            trackOpenedLazy(notificationID)
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
                getFirebaseMessagingToken()
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

        // Create player and run Pushdy from now on
        @JvmStatic
        fun onSession(force: Boolean){
            val playerID = PDYLocalData.getPlayerID()
            Log.d(TAG, "onSession: PLAYER ID: $playerID")
            if (playerID == null) {
                createPlayer()
            } else {
                if (force){
                    editPlayer()
                } else if (!updatePlayerIfNeeded()){
                    createNewSession()
                }
                _subscribe()
            }

        }

        private fun initialize() {
            if (_context != null) {
                PDYLocalData.initWith(_context!!)

                if (_context!! is Application) {
                    PDYLifeCycleHandler.listen(_context!! as Application)
                }
            }
            else {
                throw noContextWasSetException()
            }
        }

        private fun getFirebaseMessagingToken() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val exception:Exception = task.exception ?: Exception("[Pushdy] Failed to register remote notification")
                    getDelegate()?.onRemoteNotificationFailedToRegister(exception)
                    return@OnCompleteListener
                }

                val token = task.result;
                if (token != null) {
                    Log.d(TAG, "getFirebaseMessagingToken token: ${token!!}")
                    val lastToken = PDYLocalData.getDeviceToken()
                    PDYLocalData.setDeviceToken(token!!)
                    getDelegate()?.onRemoteNotificationRegistered(token!!)
                    val playerID = PDYLocalData.getPlayerID()
                    if (playerID == null) {
                        createPlayer()
                    }
                    else {
                        if (lastToken != token){
                            editPlayer()
                        }
                    }
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

        private fun updatePlayerIfNeeded(): Boolean {
            if (!_creatingPlayer && !_editingPlayer) {
                var shouldUpdate = false
                if (PDYLocalData.attributesHasChanged()) {
                    shouldUpdate = true
                }

                if (shouldUpdate) {
                    if (PDYLocalData.isFetchedAttributes()) {
                        editPlayer()
                        return true
                    }
                }
            }
            return false
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
        fun setBadgeOnForeground(badge_on_foreground:Boolean) {
            _badge_on_foreground = badge_on_foreground
        }

        @JvmStatic
        fun getBadgeOnForeground() : Boolean {
            return _badge_on_foreground
        }

        @JvmStatic
        fun setSmallIcon(icon:Int) {
            _smallIcon = icon
        }

        @JvmStatic
        fun getSmallIcon() : Int? {
            return _smallIcon ?: R.drawable.ic_notification
        }

        private fun setPlayerID(playerID: String) {
            PDYLocalData.setPlayerID(playerID)
        }

        @JvmStatic
        fun getDeviceToken() : String? {
            return PDYLocalData.getDeviceToken()
        }

        @JvmStatic
        fun getPendingEvents(count: Int): MutableList<java.util.HashMap<String, Any>>? {
            return PDYLocalData.getPendingTrackEvents(count)
        }

        @JvmStatic
        fun setPendingEvents(list: MutableList<HashMap<String, Any>>) {
            PDYLocalData.setPendingTrackEvents(list)
        }

        @JvmStatic
        fun removePendingEvents(count: Int) {
            PDYLocalData.removePendingTrackEvents(count)
        }

        @JvmStatic
        fun subscribe() {
            return _subscribe()
        }

        @JvmStatic
        fun getAllBanners(): JsonArray? {
            return PDYLocalData.getBanners()
        }

        @JvmStatic
        fun trackBanner(bannerId: String, type: String) {
            var playerID = PDYLocalData.getPlayerID()
            var applicationId = PDYLocalData.getApplicationId() ?: "pushdy";

            // FIXME: remove this line; for testing only
//            applicationId = "pushdy";
//            playerID = "96655d2e-ce02-3ec7-a0f6-273e5458fe67"


            if (playerID != null) {
                val player = PDYPlayer(_context!!, _clientKey!!, playerID)

                // update banner tracking object
                val bannerTrackingData = PDYLocalData.getBannerObject(bannerId) ?: JsonObject()
                // due to server add the amount of each type of event by add the amount of this data.
                // So we need a variable to store the amount of each type of event.
                // Then reset the amount of each type of event to 0 when send to server.
                val bannerTrackingDataReset = PDYLocalData.getBannerObject("track_$bannerId") ?: JsonObject()



                // {"close": 1, "click": 1, "imp": 1, "loaded": 1, "last_close_ts": 1234567890, "last_click_ts": 1234567890, "last_imp_ts": 1234567890, "last_loaded_ts": 1234567890}

                when (type) {
                    "impression" -> {
                        // increase impression count
                        val impressionCount = bannerTrackingData.get("imp")?.asInt ?: 0
                        bannerTrackingData.addProperty("imp", impressionCount + 1)
                        // update last_impression_ts
                        bannerTrackingData.addProperty(
                            "last_imp_ts",
                            System.currentTimeMillis() / 1000
                        )

                        bannerTrackingDataReset.addProperty("imp", impressionCount + 1)
                        bannerTrackingDataReset.addProperty(
                            "last_imp_ts",
                            System.currentTimeMillis() / 1000
                        )
                    }
                    "click" -> {
                        // increase click count
                        val clickCount = bannerTrackingData.get("click")?.asInt ?: 0
                        bannerTrackingData.addProperty("click", clickCount + 1)
                        // update last_click_ts
                        bannerTrackingData.addProperty("last_click_ts", System.currentTimeMillis() / 1000)

                        bannerTrackingDataReset.addProperty("click", clickCount + 1)
                        bannerTrackingDataReset.addProperty(
                            "last_click_ts",
                            System.currentTimeMillis() / 1000
                        )
                    }
                    "close" -> {
                        // increase close count
                        val closeCount = bannerTrackingData.get("close")?.asInt ?: 0
                        bannerTrackingData.addProperty("close", closeCount + 1)
                        // update last_close_ts
                        bannerTrackingData.addProperty("last_close_ts", System.currentTimeMillis() / 1000)

                        bannerTrackingDataReset.addProperty("close", closeCount + 1)
                        bannerTrackingDataReset.addProperty(
                            "last_close_ts",
                            System.currentTimeMillis() / 1000
                        )
                    }
                    "loaded" -> {
                        // increase loaded count
                        val loadedCount = bannerTrackingData.get("loaded")?.asInt ?: 0
                        bannerTrackingData.addProperty("loaded", loadedCount + 1)
                        // update last_loaded_ts
                        bannerTrackingData.addProperty("last_loaded_ts", System.currentTimeMillis() / 1000)

                        bannerTrackingDataReset.addProperty("loaded", loadedCount + 1)
                        bannerTrackingDataReset.addProperty(
                            "last_loaded_ts",
                            System.currentTimeMillis() / 1000
                        )
                    }
                }

                // update banner tracking object
                PDYLocalData.setBannerObject(bannerId, bannerTrackingData)
                PDYLocalData.setBannerObject("track_$bannerId", bannerTrackingDataReset)

                Log.d(TAG, "trackBanner bannerTrackingData: ${bannerTrackingData}")
                var data = JsonObject()
                // convert to {
                //    "imp": {
                //        "b": {
                //            "a506b0ce-1b8b-440f-b415-1acc3ade855d": 3
                //        }
                //    }
                //}

                // imp
                data.add("imp", JsonObject())
                data.get("imp")?.asJsonObject?.add("b", JsonObject())
                data.get("imp")?.asJsonObject?.get("b")?.asJsonObject?.addProperty(bannerId, bannerTrackingDataReset.get("imp")?.asInt ?: 0)

                // close
                data.add("close", JsonObject())
                data.get("close")?.asJsonObject?.add("b", JsonObject())
                data.get("close")?.asJsonObject?.get("b")?.asJsonObject?.addProperty(bannerId, bannerTrackingDataReset.get("close")?.asInt ?: 0)

                // click
                data.add("click", JsonObject())
                data.get("click")?.asJsonObject?.add("b", JsonObject())
                data.get("click")?.asJsonObject?.get("b")?.asJsonObject?.addProperty(bannerId, bannerTrackingDataReset.get("click")?.asInt ?: 0)

                // loaded
                data.add("loaded", JsonObject())
                data.get("loaded")?.asJsonObject?.add("b", JsonObject())
                data.get("loaded")?.asJsonObject?.get("b")?.asJsonObject?.addProperty(bannerId, bannerTrackingDataReset.get("loaded")?.asInt ?: 0)


                Log.d(TAG, "trackBanner data: ${data}")

                player.trackBanner(applicationId,playerID, data, { response ->
                    Log.d(TAG, "trackBanner successfully ${response}")
                    // clear banner tracking object reset
                    PDYLocalData.setBannerObject("track_$bannerId", JsonObject())
                    null
                }, { code, message ->
                    Log.d(TAG, "trackBanner error: ${code}, message:${message}")
                    null
                })
            }
        }

        @JvmStatic
        fun getBannerData(bannerId: String): JsonObject? {
            return PDYLocalData.getBannerObject(bannerId) ?: JsonObject()
        }

        private fun add(bannerId: String, i: Int) {

        }

        @JvmStatic
        fun setApplicationId(applicationId: String) {
            PDYLocalData.setApplicationId(applicationId)
        }

        @JvmStatic
        fun trackEvent(eventName: String, params: HashMap<String, Any>, immediate: Boolean = false, completion: ((response: JsonElement?) -> Unit)? = null, failure: ((code:Int, message:String?) -> Unit)? = null) {
            val playerID = this.getPlayerID();
            if (playerID != null) {
                val event = PDYEvent(this._context!!, this._clientKey!!, playerID!!);

                event.trackEvent(eventName, params, immediate, { response: JsonElement? ->
                    completion?.invoke(response)
                }, { code:Int, message:String? ->
                    failure?.invoke(code, message)
                })
            }
        }

        @JvmStatic
        fun pushPendingEvents(completion: ((response: JsonElement?) -> Unit)? = null, failure: ((code:Int, message:String?) -> Unit)? = null) {
            val playerID = this.getPlayerID();
            if (playerID != null) {
                val event = PDYEvent(this._context!!, this._clientKey!!, playerID!!);

                event.pushPendingEvents( { response: JsonElement? ->
                    completion?.invoke(response)
                }, { code:Int, message:String? ->
                    failure?.invoke(code, message)
                })
            }
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
                    Log.d(TAG, "createNewSession error: ${code}, message:${message}")
                    null
                })
            }
        }

        internal fun _subscribe() {

            var playerID = PDYLocalData.getPlayerID()
            Log.d(TAG, "subscribe: PLAYER ID: $playerID")
            var applicationId = PDYLocalData.getApplicationId() ?: "pushdy";

            // FIXME: remove this line; for testing only
//            applicationId = "pushdy";
//            playerID = "96655d2e-ce02-3ec7-a0f6-273e5458fe67"


            if (playerID != null) {
                val player = PDYPlayer(_context!!, _clientKey!!, playerID)
                player.subscribe(applicationId, playerID, { response ->
                    Log.d(TAG, "subscribe successfully ${response}")
                    val banners = response?.asJsonObject?.get("banners")?.asJsonArray;
                    if (banners != null) {
                        Log.d(TAG, "subscribe banners: ${banners}")
                        PDYLocalData.setBanners(banners)
                    }
                    null
                }, { code, message ->
                    Log.d(TAG, "subscribe error: ${code}, message:${message}")
                    null
                })
            }
        }

        internal fun createPlayer() {
            if (_deviceID == "unexpecteddeviceid"){
                Log.d(TAG, "Skip create player because of _deviceID: $_deviceID")
                return
            }

            Log.d(TAG, "attempt to createPlayer with device ID: $_deviceID")
            var hasTokenBefore = false
            val deviceToken = PDYLocalData.getDeviceToken()
            if (deviceToken != null) {
                hasTokenBefore = true
            }

            val params = HashMap<String, Any>()
            if (deviceToken != null) {
                Log.d("Pushdy", "createPlayer deviceToken: "+deviceToken)
                params.put(PDYParam.DeviceToken, deviceToken)
            }

            try {
                _creatingPlayer = true
                addPlayer(params, { response ->
                    _creatingPlayer = false
                    val jsonObj = response as JsonObject
                    if (jsonObj != null && jsonObj.has("success") && jsonObj.get("success").asBoolean == true) {
                        if (jsonObj.has("id")) {
                            Log.d("Pushdy", "create player success ")
                            setPlayerID(jsonObj.get("id").asString)
                            if (PDYLocalData.attributesHasChanged()) {
                                editPlayer()
                            }
                        }
                        else {
                            Log.d("Pushdy", "create player error: jsonObj does not containing field `id`")
                        }
                    }
                    else {
                        Log.d("Pushdy", "create player error: jsonObj is not success")
                    }

                    var shouldEditPlayer = false
                    if (getDeviceToken() != null && hasTokenBefore == false) {
                        shouldEditPlayer = true
                    }

                    if (PDYLocalData.isFetchedAttributes()) {
                        if (shouldEditPlayer) {
                            editPlayer()
                        }
                    }
                    else {
                        //getAttributes({ response:JsonElement? ->
                        //    if (shouldEditPlayer) {
                        //        editPlayer()
                        //    }
                        //}, { code:Int, message:String? ->
                        //    if (shouldEditPlayer) {
                        //        editPlayer()
                        //    }
                        //})
                    }

                    null
                }, { code:Int, message:String? ->
                    _creatingPlayer = false
                    Log.d("Pushdy", "create player error ")
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

        internal fun trackOpened(playerID: String, notificationID: String) {
            if (notificationID != null) {
                trackOpened(playerID, notificationID, { response ->
                    Log.d(TAG, "trackOpened {$notificationID} successfully")
                    null
                }, { code, message ->
                    Log.d(TAG, "trackOpened error: ${code}, messag:${message}")
                    null
                })
            }
        }

        /**
         * http://redmine.mobiletech.vn/issues/6077
         * - [x] Save to trackOpenQueue
         * - [x] Flushing queue and send all pending trackOpenItems after random (1-125) seconds
         * - [x] Persist this queue to localStorage in case of user kill app before queue was flushed
         * - [x] Can send by batch if queue has multiple items
         *
         * - [x] Restore this queue when app open / app open by notification => Avoid overriten
         * - [x] In case of schedule is running => Cancel the prev schedule if you fire new schedule
         *
         * Known issues:
         * - Open app by clicking a notification cause pendingTrackingOpenedItems has only 1 notiId, lost all ids in storage
         */
        internal fun trackOpenedLazy(notificationID: String) {
            Log.d(TAG, "trackOpenedLazy save notiId={$notificationID} to tracking queue pendingTrackingOpenedItems(before): " + pendingTrackingOpenedItems.joinToString(","))

            // Save to queue + localStorage
            pendingTrackingOpenedItems.add(notificationID)
            PDYLocalData.setPendingTrackOpenNotiIds(pendingTrackingOpenedItems)
            // Delay flushing queue
            // Empty queue on success
            // NOTE: You must do this in non-blocking mode to ensure program will continue to run without any dependant on this code
            val delayInMs:Long = (1L..125L).random() * 1000
            // val delayInMs:Long = 8L * 1000
            trackOpenedWithRetry(delayInMs)
        }

        internal fun trackOpenedWithRetry(delayInMs: Long) {
            val verbose = false
            if (verbose) Log.d(TAG, "trackOpenedWithRetry: delayInMs: $delayInMs")

            // Delay flushing queue
            // Empty queue on success
            // NOTE: You must do this in non-blocking mode to ensure program will continue to run without any dependant on this code
            // Tested in background: This Timer still run when app is in background, not sure for Xiaomi 3
            // Tested in closed state then open by push:
            pendingTrackingOpenedTimerTask?.cancel()   // You need to test the case: Cancel existing task if another task was schedule, to avoid duplication tracking
            pendingTrackingOpenedTimerTask = object : TimerTask() {
                override fun run() {
                    if (verbose) Log.d(TAG, "trackOpenedWithRetry: Process tracking queue after delay ${delayInMs}s | Ids=${pendingTrackingOpenedItems.joinToString(",")}")

                    val playerID: String? = PDYLocalData.getPlayerID()
                    if (playerID.isNullOrBlank()) {
                        // retry after 10 seconds
                        trackOpenedWithRetry(10000)
                        if (verbose) Log.d(TAG, "trackOpenedWithRetry: playerID empty, trying to retry after ${10}s")
                    } else {
                        // NOTE: If api request was failed, we don't intend to fire again, ignore it
                        if (pendingTrackingOpenedItems.size < 1) {
                            if (verbose) Log.d(TAG, "trackOpenedWithRetry: Skip because pendingTrackingOpenedItems empty")
                        } else {
                            trackOpenedList(playerID, pendingTrackingOpenedItems, { response ->
                                if (verbose) Log.d(TAG, "trackOpenedWithRetry: {$pendingTrackingOpenedItems} successfully")
                                // Empty queue on success
                                pendingTrackingOpenedItems = mutableListOf()
                                PDYLocalData.setPendingTrackOpenNotiIds(pendingTrackingOpenedItems)
                                // End
                                null
                            }, { code, message ->
                                if (verbose) Log.e(TAG, "trackOpenedWithRetry: error: ${code}, message:${message}")
                                null
                            })
                        }
                    }
                }
            }

            pendingTrackingOpenedTimer.schedule(pendingTrackingOpenedTimerTask, delayInMs)
        }


        @JvmStatic
        fun restorePendingTrackingOpenedItems() {
            var items: List<String>? = PDYLocalData.getPendingTrackOpenNotiIds()
            if (items != null) {
                Log.d(TAG, "restorePendingTrackingOpenedItems: Restored items: " + items.joinToString(","))
                pendingTrackingOpenedItems.addAll(items)
            } else {
                Log.d(TAG, "restorePendingTrackingOpenedItems: No pending tracking open")
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

        internal fun trackOpened(playerID: String, notificationID: String, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
            if (_context != null) {
                if (_clientKey != null) {
                    val notification = PDYNotification(_context!!, _clientKey!!, _deviceID)
                    notification.trackOpened(playerID, notificationID, completion, failure)
                } else {
                    throw noClientKeyException()
                }
            }
            else {
                throw noContextWasSetException()
            }
        }

        internal fun trackOpenedList(playerID: String, notificationIDs: List<String>, completion:((response: JsonElement?) -> Unit?)?, failure:((code:Int, message:String?) -> Unit?)?) {
            if (_context != null) {
                if (_clientKey != null) {
                    val player = PDYPlayer(_context!!, _clientKey!!, _deviceID)
                    try {
                        player.trackOpened(playerID, notificationIDs, completion, failure)
                    } catch (e: Exception) {
                        Log.e(TAG, "trackOpenedList: error: " + e.message)
                    }
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

        @JvmStatic
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
        fun getPendingNotification() : String? {
            if (_pendingNotifications.size > 0) {
                return _pendingNotifications[_pendingNotifications.size-1]
            }
            return null
        }

        @JvmStatic
        fun getPendingNotifications() : List<String> {
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
                if (notificationID in item){
                    index = i
                    break
                }
            }
            if (index >= 0 && index < _pendingNotifications.size) {
                _pendingNotifications.removeAt(index)
            }
        }

        @JvmStatic
        fun pushPendingNotification(notification: String) {
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
                // log name and value.
                Log.d(TAG, "setAttribute: name: $name, value: $value")
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
        @JvmStatic
        fun setPushBannerAutoDismiss(autoDismiss:Boolean) {
            PDYLocalData.setPushBannerAutoDismiss(autoDismiss)
        }

        @JvmStatic
        fun isPushBannerAutoDismiss() : Boolean {
            val autoDismiss = PDYLocalData.isPushBannerAutoDismiss()
            return autoDismiss
        }

        @JvmStatic
        fun getPushBannerDismissDuration() : Float {
            val duration = PDYLocalData.getPushBannerDismissDuration()
            return duration
        }

        @JvmStatic
        fun setPushBannerDismissDuration(duration:Float) {
            PDYLocalData.setPushBannerDismissDuration(duration)
        }

        internal fun getCustomPushBannerView() : View? {
            return _customPushBannerView
        }

        @JvmStatic
        fun setCustomPushBanner(customView:View) {
            if (customView is PDYPushBannerActionInterface) {
                _customPushBannerView = customView
            }
            else {
                throw Exception("[Pushdy] Your custom view must implement PDYPushBannerActionInterface interface")
            }
        }

        @JvmStatic
        fun useSDKHandler(enabled: Boolean) {
            PDYNotificationHandler.useSDKHandler(enabled)
        }

        @JvmStatic
        fun handleCustomInAppBannerPressed(notificationId: String) {
            PDYNotificationHandler.handleCustomInAppBannerPressed(notificationId)
        }

    }

}