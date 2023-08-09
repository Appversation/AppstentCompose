package com.appversation.appstentcompose

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.has(keyName: String) : Boolean {
    return this.has(keyName) || this.has("android:$keyName")
}

fun JSONObject.getString(keyName: String): String {

    return if (this.has("android:$keyName")) {
        getString("android:$keyName")
    } else {
        getString(keyName)
    }
}

fun JSONObject.optString(keyName: String, fallback: String): String {

    val androidKeyValue = optString("android:$keyName")

    return androidKeyValue.ifEmpty {
        optString(keyName, fallback)
    }
}

fun JSONObject.getDouble(keyName: String): Double {

    return if (this.has("android:$keyName")) {
        getDouble("android:$keyName")
    } else {
        getDouble(keyName)
    }
}

fun JSONObject.optDouble(keyName: String, fallback: Double): Double {

    return if (this.has("android:$keyName")) {
        optDouble("android:$keyName", fallback)
    } else {
        optDouble(keyName, fallback)
    }
}

fun JSONObject.getInt(keyName: String): Int {

    return if (this.has("android:$keyName")) {
        getInt("android:$keyName")
    } else {
        getInt(keyName)
    }
}

fun JSONObject.optInt(keyName: String, fallback: Int): Int {

    return if (this.has("android:$keyName")) {
        optInt("android:$keyName", fallback)
    } else {
        optInt(keyName, fallback)
    }
}

fun JSONObject.optBoolean(keyName: String, fallback: Boolean): Boolean {

    return if (this.has("android:$keyName")) {
        optBoolean("android:$keyName", false)
    } else {
        optBoolean(keyName, false)
    }
}

fun JSONObject.getJSONArray(keyName: String): JSONArray {

    return if (this.has("android:$keyName")) {
        getJSONArray("android:$keyName")
    }
    else if (this.has(keyName)) {
        getJSONArray(keyName)
    } else {
        JSONArray()
    }
}

fun JSONObject.getJSONObject(keyName: String): JSONObject {

    return if (this.has("android:$keyName")) {
        getJSONObject("android:$keyName")
    }
    else if (this.has(keyName)) {
        getJSONObject(keyName)
    } else {
        JSONObject()
    }
}