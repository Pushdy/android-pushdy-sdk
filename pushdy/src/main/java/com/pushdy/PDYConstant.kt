package com.pushdy

open class PDYConstant {
    class Keys {
        private constructor()
        companion object {
            @JvmStatic
            val NOTIFICATION_ID = "_notification_id"

            @JvmStatic
            val IN_APP_PUSH_BOARDCAST = "PUSHDY_IN_APP_PUSH_BOARDCAST"
        }
    }

    class AppState {
        private constructor()
        companion object {
            @JvmStatic
            val ACTIVE = "active"

            @JvmStatic
            val BACKGROUND = "background"
        }
    }

    class AttributeType {
        private constructor()
        companion object {
            val kBoolean = "boolean"
            val kArray = "array"
            val kString = "string"
            val kNumber = "number"

            fun types() : Array<String> {
                return arrayOf(kBoolean, kArray, kString, kNumber)
            }
        }
    }

    companion object {

    }
}