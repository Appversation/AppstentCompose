package com.appversation.appstentcompose

import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection


object RequestHandler {
    internal data class HttpResponse(
        val statusCode: Int,
        val body: String,
        val url: URL
    )

    @Throws(IOException::class)
    fun requestPOST(r_url: String?, postDataParams: JSONObject): String? {
        val url = URL(r_url)
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.readTimeout = 3000
        conn.connectTimeout = 3000
        conn.requestMethod = "POST"
        conn.doInput = true
        conn.doOutput = true
        val os: OutputStream = conn.outputStream
        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
        writer.write(encodeParams(postDataParams))
        writer.flush()
        writer.close()
        os.close()
        val responseCode: Int = conn.responseCode // To Check for 200
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            val `in` = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuffer("")
            var line: String?
            while (`in`.readLine().also { line = it } != null) {
                sb.append(line)
                break
            }
            `in`.close()
            return sb.toString()
        }
        return null
    }

    @Throws(java.lang.Exception::class)
    private fun encodeParams(params: JSONObject): String {
        val result = StringBuilder()
        var first = true
        val itr = params.keys()
        while (itr.hasNext()) {
            val key = itr.next()
            val value = params[key]
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value.toString(), "UTF-8"))
        }
        return result.toString()
    }

    @Throws(IOException::class)
    fun requestGET(urlString: String?): JSONObject {

        val responseString = requestGET(URL(urlString), appstentHeaders())
        return if (responseString.isNotEmpty()) JSONObject(responseString) else JSONObject()
    }

    @Throws(IOException::class)
    fun requestGET(url: URL, additionalHeaders:Map<String, String> = HashMap()): String {
        val response = requestGETResponse(url, additionalHeaders)
        val contentEnvironment = additionalHeaders["content-environment"]
            ?: ModuleConfigs.normalizedContentEnvironment

        if (response.statusCode in 200..299) {
            return response.body
        }

        throw ViewContentRequestException(
            statusCode = response.statusCode,
            body = response.body,
            contentEnvironment = contentEnvironment,
            url = url
        )
    }

    @Throws(IOException::class)
    internal fun requestGETResponse(url: URL, additionalHeaders: Map<String, String> = HashMap()): HttpResponse {
        val con = (url.openConnection() as HttpURLConnection)
        try {
            additionalHeaders.forEach {
                con.setRequestProperty(it.key, it.value)
            }

            con.requestMethod = "GET"
            val responseCode = con.responseCode
            val stream = if (responseCode in 200..299) con.inputStream else con.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""

            return HttpResponse(responseCode, response, url)
        } finally {
            con.disconnect()
        }
    }

    internal fun appstentHeaders(
        contentEnvironment: String = ModuleConfigs.normalizedContentEnvironment,
        extraHeaders: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "x-api-key" to ModuleConfigs.apiKey,
            "content-environment" to contentEnvironment
        ) + extraHeaders
    }
}
