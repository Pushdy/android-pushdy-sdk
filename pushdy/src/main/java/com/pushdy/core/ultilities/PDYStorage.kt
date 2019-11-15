package com.pushdy.core.ultilities

import android.content.Context

open class PDYStorage {
    companion object {
        private var PREFS_NAME:String = "PUSHDY_PREFS_NAME"

        @JvmStatic
        fun remove(context: Context, key:String) {
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings?.edit()
            editor?.remove(key)
            editor?.commit()
        }

        @JvmStatic
        fun setString(context: Context, key:String, value:String) {
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings?.edit()
            editor?.putString(key, value)
            editor?.commit()
        }

        @JvmStatic
        fun getString(context: Context, key:String) : String? {
            var value:String? = null
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (settings != null && settings!!.contains(key)) {
                value = settings?.getString(key, null)
            }
            return value
        }

        @JvmStatic
        fun setBool(context: Context, key:String, value:Boolean) {
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings?.edit()

            editor?.putBoolean(key, value)
            editor?.commit()
        }

        @JvmStatic
        fun getBool(context: Context, key:String) : Boolean? {
            var value:Boolean? = null
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (settings != null && settings!!.contains(key)) {
                value = settings?.getBoolean(key, false)
            }
            return value
        }

        @JvmStatic
        fun setInt(context: Context, key:String, value:Int) {
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings?.edit()

            editor?.putInt(key, value)
            editor?.commit()
        }

        @JvmStatic
        fun getInt(context: Context, key:String) : Int? {
            var value:Int? = null
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (settings != null && settings!!.contains(key)) {
                value = settings?.getInt(key, -1)
            }
            return value
        }

        @JvmStatic
        fun setFloat(context: Context, key:String, value:Float) {
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings?.edit()

            editor?.putFloat(key, value)
            editor?.commit()
        }

        @JvmStatic
        fun getFloat(context: Context, key:String) : Float? {
            var value:Float? = null
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (settings != null && settings!!.contains(key)) {
                value = settings?.getFloat(key, 0.0f)
            }
            return value
        }

        @JvmStatic
        fun setLong(context: Context, key:String, value:Long) {
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings?.edit()

            editor?.putLong(key, value)
            editor?.commit()
        }

        @JvmStatic
        fun getLong(context: Context, key:String) : Long? {
            var value:Long? = null
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (settings != null && settings!!.contains(key)) {
                value = settings?.getLong(key, 0)
            }
            return value
        }

        @JvmStatic
        fun setStringSet(context: Context, key:String, value:Set<String>) {
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings?.edit()

            editor?.putStringSet(key, value)
            editor?.commit()
        }

        @JvmStatic
        fun getStringSet(context: Context, key:String) : Set<String>? {
            var value:Set<String>? = null
            val settings = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (settings != null && settings!!.contains(key)) {
                value = settings?.getStringSet(key, null)
            }
            return value
        }
    }
}