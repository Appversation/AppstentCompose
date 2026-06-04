package com.appversation.appstentcompose

import org.json.JSONObject
import java.io.IOException
import java.net.URL

class ViewContentRequestException(
    val statusCode: Int,
    val body: String?,
    val contentEnvironment: String,
    val url: URL
) : IOException(
    "Appstent content request failed with status $statusCode for contentEnvironment \"$contentEnvironment\": ${
        parseMessage(body) ?: "HTTP $statusCode"
    }"
) {
    val statusText: String = "HTTP $statusCode"
    val responseMessage: String = parseMessage(body) ?: statusText

    companion object {
        internal fun parseMessage(body: String?): String? {
            if (body.isNullOrBlank()) {
                return null
            }

            Regex(""""(?:message|error)"\s*:\s*"([^"]*)"""")
                .find(body)
                ?.groups
                ?.get(1)
                ?.value
                ?.let { return it }

            return try {
                val json = JSONObject(body)
                when {
                    json.has("message") -> json.optString("message")
                    json.has("error") -> json.optString("error")
                    else -> body
                }
            } catch (_: Exception) {
                body
            }
        }
    }
}
