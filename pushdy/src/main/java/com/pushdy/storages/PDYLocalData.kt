package com.pushdy.storages

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pushdy.PDYConstant
import com.pushdy.core.entities.PDYParam
import com.pushdy.core.ultilities.PDYStorage

open class PDYLocalData {
    companion object {
        val ATTTRIBUTE_PREFIX = "PUSHDY_ATTR_"
        val PREV_ATTTRIBUTE_PREFIX = "PUSHDY_PREV_ATTR_"
        val CHANGED_ATTRIBUTES_STACK = "PUSHDY_CHANGED_ATTRIBUTES_STACK"
        val ATTRIBUTES_SCHEMA = "PUSHDY_ATTRIBUTES_SCHEMA"
        val PREV_ATTRIBUTES_SCHEMA = "PUSHDY_PREV_ATTRIBUTES_SCHEMA"

        var _context:Context? = null

        fun initWith(ctx:Context) {
            _context = ctx
            PDYStorage.setString(_context!!, ATTRIBUTES_SCHEMA, """[{"name":"device_name","type":"string","label":"Device Name","default":false},{"name":"network_carrier","type":"string","label":"Network Carrier","default":false},{"name":"registered_at","type":"number","label":"Registered Date","default":false},{"name":"pv_schedule","type":"number","label":"Số lần xem mục Lịch đấu","default":false},{"name":"pv_highlight_video","type":"number","label":"Số lần xem mục Video highlight","default":false},{"name":"device_type","type":"string","label":"Device Type","default":true},{"name":"device_id","type":"string","label":"Device Id","default":true},{"name":"device_token","type":"string","label":"Device Token","default":true},{"name":"device_os","type":"string","label":"Device Os","default":true},{"name":"device_model","type":"string","label":"Device Model","default":true},{"name":"app_version","type":"string","label":"App Version","default":true},{"name":"mobile_number","type":"string","label":"Mobile Number","default":true},{"name":"language","type":"string","label":"Language","default":true},{"name":"country","type":"string","label":"Country","default":true},{"name":"name","type":"string","label":"Name","default":true},{"name":"gender","type":"string","label":"Gender","default":true},{"name":"custom_user_id","type":"string","label":"Custom User_id","default":true},{"name":"utm_source","type":"array","label":"Utm Source","default":true},{"name":"utm_campaign","type":"array","label":"Utm Campaign","default":true},{"name":"utm_medium","type":"array","label":"Utm Medium","default":true},{"name":"utm_term","type":"array","label":"Utm Term","default":true},{"name":"utm_content","type":"array","label":"Utm Content","default":true}]""")
        }

        private fun noContextWasSetException() : Exception {
            return Exception("[Pushdy >> PDYLocalData] No context was set!")
        }

        fun isFirstTimeOpenApp() : Boolean {
            if (_context != null) {
                var firstTime: Boolean? = PDYStorage.getBool(_context!!, "PUSHDY_FIRST_TIME_OPEN_APP")
                return firstTime ?: false
            }
            throw noContextWasSetException()
        }

        fun setFirstTimeOpenApp(context: Context, firstTime:Boolean) {
            if (_context != null) {
                PDYStorage.setBool(_context!!, "PUSHDY_FIRST_TIME_OPEN_APP", firstTime)
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun getPlayerID() : String? {
            if (_context != null) {
                val playerID = PDYStorage.getString(_context!!, "PUSHDY_PLAYER_ID")
                return playerID ?: null
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setPlayerID(playerID:String) {
            if (_context != null) {
                PDYStorage.setString(_context!!, "PUSHDY_PLAYER_ID", playerID)
            }
            else {
                throw noContextWasSetException()
            }
        }


        @JvmStatic
        fun getDeviceID() : String? {
            if (_context != null) {
                val value = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.DeviceID)
                return value ?: null
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setDeviceID(deviceID:String) {
            if (_context != null) {
                val prevDeviceID = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.DeviceID)
                if (prevDeviceID != null) {
                    PDYStorage.setString(_context!!, PREV_ATTTRIBUTE_PREFIX+PDYParam.DeviceID, deviceID)
                }
                PDYStorage.setString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.DeviceID, deviceID)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun getDeviceToken() : String? {
            if (_context != null) {
                val value = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.DeviceToken)
                return value ?: null
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setDeviceToken(deviceToken:String) {
            if (_context != null) {
                val prevValue = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.DeviceToken)
                if (prevValue != null) {
                    PDYStorage.setString(_context!!, PREV_ATTTRIBUTE_PREFIX+PDYParam.DeviceToken, prevValue)
                }
                PDYStorage.setString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.DeviceToken, deviceToken)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun isPushBannerAutoDismiss() : Boolean {
            if (_context != null) {
                val value = PDYStorage.getBool(_context!!, "PUSHDY_PUSH_BANNER_AUTO_DISMISS")
                return value ?: true
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setPushBannerAutoDismiss(auto:Boolean) {
            if (_context != null) {
                PDYStorage.setBool(_context!!, "PUSHDY_PUSH_BANNER_AUTO_DISMISS", auto)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun getPushBannerDismissDuration() : Float {
            if (_context != null) {
                val value = PDYStorage.getFloat(_context!!, "PUSHDY_PUSH_BANNER_DISMISS_DURATION")
                return value ?: 5.0f
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setPushBannerDismissDuration(duration:Float) {
            if (_context != null) {
                PDYStorage.setFloat(_context!!, "PUSHDY_PUSH_BANNER_DISMISS_DURATION", duration)
            }
            else {
                throw noContextWasSetException()
            }
        }


        @JvmStatic
        fun getAppVersion() : String? {
            if (_context != null) {
                val value = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.AppVersion)
                return value ?: "0.0.1"
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setAppVersion(version:String) {
            if (_context != null) {
                val prevValue = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.AppVersion)
                if (prevValue != null) {
                    PDYStorage.setString(_context!!, PREV_ATTTRIBUTE_PREFIX+PDYParam.AppVersion, prevValue)
                }
                PDYStorage.setString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.AppVersion, version)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun getLanguage() : String? {
            if (_context != null) {
                val value = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.Language)
                return value ?: null
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setLanguage(language:String) {
            if (_context != null) {
                val prevValue = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.Language)
                if (prevValue != null) {
                    PDYStorage.setString(_context!!, PREV_ATTTRIBUTE_PREFIX+PDYParam.Language, prevValue)
                }
                PDYStorage.setString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.Language, language)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun getCountry() : String? {
            if (_context != null) {
                val value = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.Country)
                return value ?: null
            }
            throw noContextWasSetException()
        }

        @JvmStatic
        fun setCountry(country:String) {
            if (_context != null) {
                val prevValue = PDYStorage.getString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.Country)
                if (prevValue != null) {
                    PDYStorage.setString(_context!!, PREV_ATTTRIBUTE_PREFIX+PDYParam.Country, prevValue)
                }
                PDYStorage.setString(_context!!, ATTTRIBUTE_PREFIX+PDYParam.Country, country)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun setFetchedAttributes(fetched:Boolean) {
            if (_context != null) {
                PDYStorage.setBool(_context!!, "PUSHDY_FETCHED_ATTRIBUTES", fetched)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun isFetchedAttributes() : Boolean {
            return true
            //if (_context != null) {
            //    val value = PDYStorage.getBool(_context!!, "PUSHDY_FETCHED_ATTRIBUTES")
            //    return value ?: false
            //}
            //throw noContextWasSetException()
        }

        @JvmStatic
        fun setAttributesSchema(attributes: JsonArray) {
            if (_context != null) {
                val newValue = attributes.toString()
                val currentValue = PDYStorage.getString(_context!!, ATTRIBUTES_SCHEMA)
                if (currentValue != null) {
                    PDYStorage.setString(_context!!, PREV_ATTRIBUTES_SCHEMA, currentValue)
                }
                PDYStorage.setString(_context!!, ATTRIBUTES_SCHEMA, newValue)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun getAttributesSchema() : JsonArray? {
            if (_context != null) {
                val currentValueStr = PDYStorage.getString(_context!!, ATTRIBUTES_SCHEMA)
                val attributes = Gson().fromJson(currentValueStr, JsonArray::class.java) as JsonArray
                return attributes
            }
            else {
                throw noContextWasSetException()
            }
            return null
        }

        @JvmStatic
        fun isAttributeChanged(name:String, newValue:Any) : Boolean {
            var changed = false
            val value = getAttributeValue(name)
            if (value != null) {
                val prevValue = getPrevAttributeValue(name)
                if (prevValue != null) {
                    if (value is String) {
                        changed = value != (prevValue as String)
                    }
                    else if (value is Boolean) {
                        changed = value != (prevValue as String)
                    }
                    else if (value is Int) {
                        changed = value != (prevValue as Int)
                    }
                    else if (value is Long) {
                        changed = value != (prevValue as Long)
                    }
                    else if (value is Float) {
                        changed = value != (prevValue as Float)
                    }
                    else if (value is Double) {
                        changed = value != (prevValue as Double)
                    }
                    else if (value is Array<*>) {
                        val valueStr = Gson().toJson(value, Array<Any>::class.java)
                        val prevValueStr = Gson().toJson(value, Array<Any>::class.java)
                        changed = valueStr != prevValueStr
                    }
                    else if (value is Map<*, *>) {
                        val valueStr = Gson().toJson(value, Map::class.java)
                        val prevValueStr = Gson().toJson(value, Map::class.java)
                        changed = valueStr != prevValueStr
                    }
                }
                else {
                    changed = true
                }
            }
            else {
                changed = true
            }

            return changed
        }

        @JvmStatic
        fun attributesHasChanged() : Boolean {
            val changedStack = getChangedStack()
            if (changedStack != null && !changedStack!!.isEmpty()) {
                return true
            }
            return false
        }

        @JvmStatic
        fun clearChangedStack() {
            if (_context != null) {
                PDYStorage.remove(_context!!, CHANGED_ATTRIBUTES_STACK)
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun getChangedStack() : Map<String, Any>? {
            val changedStackStr = PDYStorage.getString(_context!!, CHANGED_ATTRIBUTES_STACK)
            if (changedStackStr != null) {
                val changedStack = Gson().fromJson(changedStackStr, HashMap::class.java) as HashMap<String, Any>
                return changedStack
            }
            return null
        }

        @JvmStatic
        fun pushToChangedStack(name:String, value:Any) {
            if (_context != null) {
                var resultStr:String? = null
                val changedStackStr = PDYStorage.getString(_context!!, CHANGED_ATTRIBUTES_STACK)
                if (changedStackStr != null) {
                    val changedStack = Gson().fromJson(changedStackStr, HashMap::class.java) as HashMap<String, Any>
                    changedStack.put(name, value)
                    resultStr = Gson().toJson(changedStack, HashMap::class.java)
                }
                else {
                    val changedStack =  HashMap<String, Any>()
                    changedStack.put(name, value)
                    resultStr = Gson().toJson(changedStack, HashMap::class.java)

                }
                if (resultStr != null) {
                    PDYStorage.setString(_context!!, CHANGED_ATTRIBUTES_STACK, resultStr)
                }
            }
            else {
                throw noContextWasSetException()
            }
        }

        @JvmStatic
        fun hasAttribute(name:String) : JsonObject? {
            var attribute:JsonObject? = null
            val attributes = getAttributesSchema()
            if (attributes != null) {
                for (i in 0 until attributes!!.size()) {
                    val attr = attributes!![i] as? JsonObject
                    if (attr != null && attr.has("name")) {
                        val attrName = attr.get("name").asString
                        if (attrName == name) {
                            attribute = attr
                            break
                        }

                    }
                }
            }
            return attribute
        }

        @JvmStatic
        fun isValidAttributeType(name:String, value:Any) : Boolean {
            var isValid = false
            val attribute = hasAttribute(name)
            if (attribute != null) {
                val type = attribute.get("type").asString
                if (
                    (type == PDYConstant.AttributeType.kString && value is String) ||
                    (type == PDYConstant.AttributeType.kArray && value is Array<*>) ||
                    (type == PDYConstant.AttributeType.kNumber && (value is Int || value is Long || value is Float || value is Double)) ||
                    (type == PDYConstant.AttributeType.kBoolean && value is Boolean)
                        ) {
                    isValid = true
                }
            }
            return isValid
        }

        @JvmStatic
        fun setLocalAttribValuesAfterSubmitted() {
            val attributes = getAttributesSchema()
            if (attributes != null) {
                for (i in 0 until attributes!!.size()) {
                    val attr = attributes!![i] as? JsonObject
                    if (attr != null && attr.has("name")) {
                        val attrName = attr.get("name").asString
                        val value = getAttributeValue(attrName)
                        if (value != null) {
                            setPrevAttributeValue(attrName, value)
                        }

                    }
                }
            }
        }


        @JvmStatic
        fun getAttributeValue(name:String) : Any? {
            return getValue(ATTTRIBUTE_PREFIX+name)
        }

        @JvmStatic
        fun setAttributeValue(name:String, value:Any) {
            setValue(ATTTRIBUTE_PREFIX+name, value)
        }

        @JvmStatic
        fun getPrevAttributeValue(name:String) : Any? {
            return getValue(PREV_ATTTRIBUTE_PREFIX+name)
        }

        @JvmStatic
        fun setPrevAttributeValue(name:String, value:Any) {
            setValue(PREV_ATTTRIBUTE_PREFIX+name, value)
        }


        fun getValue(name:String) : Any? {
            if (_context != null) {
                val attribute = hasAttribute(name)
                if (attribute != null && attribute.has("type")) {
                    val type = attribute.get("type").asString
                    if (type == PDYConstant.AttributeType.kArray) {
                        val valueStr = PDYStorage.getString(_context!!, name)
                        if (valueStr != null) {
                           return Gson().fromJson(valueStr, Array<Any>::class.java)
                        }
                    }
                    else if (type == PDYConstant.AttributeType.kString) {
                        return PDYStorage.getString(_context!!, name)
                    }
                    else if (type == PDYConstant.AttributeType.kBoolean) {
                        return PDYStorage.getBool(_context!!, name)
                    }
                    else if (type == PDYConstant.AttributeType.kNumber) {
                        val intValue = PDYStorage.getInt(_context!!, name)
                        if (intValue != null) {
                            return intValue
                        }

                        val longValue = PDYStorage.getLong(_context!!, name)
                        if (longValue != null) {
                            return longValue
                        }

                        return PDYStorage.getFloat(_context!!, name)
                    }
                }
            }
            else {
                throw noContextWasSetException()
            }

            return null
        }

        fun setValue(name:String, value:Any) {
            val attribute = hasAttribute(name)
            if (attribute != null && attribute.has("type")) {
                val type = attribute.get("type").asString
                if (type == PDYConstant.AttributeType.kArray) {
                    val valueStr = Gson().toJson(value, Array<Any>::class.java)
                    PDYStorage.setString(_context!!, name, valueStr)
                }
                else if (type == PDYConstant.AttributeType.kString && value is String) {
                    PDYStorage.setString(_context!!, name, value)
                }
                else if (type == PDYConstant.AttributeType.kBoolean && value is Boolean) {
                    PDYStorage.setBool(_context!!, name, value)
                }
                else if (type == PDYConstant.AttributeType.kNumber && (value is Int || value is Long || value is Float || value is Double)) {
                    if (value is Int) {
                        PDYStorage.setInt(_context!!, name, value)
                    }
                    else if (value is Long) {
                        PDYStorage.setLong(_context!!, name, value)
                    }
                    else if (value is Float || value is Double) {
                        PDYStorage.setFloat(_context!!, name, value as Float)
                    }
                }
            }
        }



    }

}