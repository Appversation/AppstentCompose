package com.appversation.appstentcompose

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.has(keyName: String) : Boolean {
    return this.has(keyName) || this.has("android:$keyName")
}

fun JSONObject.getString(keyName: String): String {

    val key = keyName.replace("android:", "")

    return optString(key)
}

fun JSONObject.optString(keyName: String, fallback: String): String {

    val key = keyName.replace("android:", "")

    return optString(key, fallback)
}

fun JSONObject.getDouble(keyName: String): Double {

    val key = keyName.replace("android:", "")

    return optDouble(key)
}

fun JSONObject.getInt(keyName: String): Int {

    val key = keyName.replace("android:", "")

    return optInt(key)
}

fun JSONObject.optInt(keyName: String, fallback: Int): Int {

    val key = keyName.replace("android:", "")

    return optInt(key, fallback)
}

fun JSONObject.optBoolean(keyName: String, fallback: Boolean): Boolean {

    val key = keyName.replace("android:", "")

    return optBoolean(key, false)
}

fun JSONObject.getJSONArray(keyName: String): JSONArray {

    val key = keyName.replace("android:", "")

    return if (this.has(key)) {
        getJSONArray(key)
    } else {
        JSONArray()
    }
}

fun JSONObject.getJSONObject(keyName: String): JSONObject {

    val key = keyName.replace("android:", "")

    return if (this.has(key)) {
        JSONObject(key)
    } else {
        JSONObject()
    }
}