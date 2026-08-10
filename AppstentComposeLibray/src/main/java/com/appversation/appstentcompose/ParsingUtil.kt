package com.appversation.appstentcompose

import org.json.JSONArray
import org.json.JSONObject

private val canonicalPropertyAliases = mapOf(
    "textAlignment" to listOf("text_alignment"),
    "percentWidth" to listOf("percent_width"),
    "percentHeight" to listOf("percent_height"),
    "minWidth" to listOf("min_width"),
    "maxWidth" to listOf("max_width"),
    "minHeight" to listOf("min_height"),
    "maxHeight" to listOf("max_height"),
    "overlayView" to listOf("overlay_view"),
)

private fun propertyCandidates(keyName: String): List<String> {
    val canonicalKey = canonicalPropertyAliases.entries
        .firstOrNull { (canonical, aliases) -> keyName == canonical || keyName in aliases }
        ?.key
        ?: keyName
    return listOf(canonicalKey) + canonicalPropertyAliases[canonicalKey].orEmpty()
}

private fun JSONObject.resolvedKey(keyName: String): String? {
    val candidates = propertyCandidates(keyName)
    return candidates
        .map { "android:$it" }
        .firstOrNull { this.has(it) }
        ?: candidates.firstOrNull { this.has(it) }
}

fun JSONObject.has(keyName: String): Boolean = resolvedKey(keyName) != null

fun JSONObject.getString(keyName: String): String =
    getString(resolvedKey(keyName) ?: keyName)

fun JSONObject.optString(keyName: String, fallback: String): String {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optString(resolvedKey, fallback)
}

fun JSONObject.getDouble(keyName: String): Double =
    getDouble(resolvedKey(keyName) ?: keyName)

fun JSONObject.optDouble(keyName: String, fallback: Double): Double {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optDouble(resolvedKey, fallback)
}

fun JSONObject.getInt(keyName: String): Int =
    getInt(resolvedKey(keyName) ?: keyName)

fun JSONObject.optInt(keyName: String, fallback: Int): Int {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optInt(resolvedKey, fallback)
}

fun JSONObject.optBoolean(keyName: String, fallback: Boolean): Boolean {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optBoolean(resolvedKey, fallback)
}

fun JSONObject.optPlatformValue(keyName: String): Any? =
    resolvedKey(keyName)?.let(::opt)

fun JSONObject.getJSONArray(keyName: String): JSONArray =
    resolvedKey(keyName)?.let(::getJSONArray) ?: JSONArray()

fun JSONObject.getJSONObject(keyName: String): JSONObject =
    resolvedKey(keyName)?.let(::getJSONObject) ?: JSONObject()
