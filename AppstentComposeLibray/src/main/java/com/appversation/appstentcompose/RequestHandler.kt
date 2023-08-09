package com.appversation.appstentcompose

import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection


object RequestHandler {

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

        val additionalHeaders = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "x-api-key" to ModuleConfigs.apiKey)

        val responseString = requestGET(URL(urlString), additionalHeaders)
        return if (responseString.isNotEmpty()) JSONObject(responseString) else JSONObject()
    }

    @Throws(IOException::class)
    private fun requestGET(url: URL, additionalHeaders:Map<String, String> = HashMap()): String {
        val con = (url.openConnection() as HttpURLConnection)
        additionalHeaders.forEach {
            con.setRequestProperty(it.key, it.value)
        }

        con.requestMethod = "GET"
        val responseCode = con.responseCode
        println("Response Code :: $responseCode")
        return if (responseCode == HttpURLConnection.HTTP_OK) { // connection ok
            val `in` =
                BufferedReader(InputStreamReader(con.inputStream))
            var inputLine: String?
            val response = StringBuffer()
            while (`in`.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            `in`.close()
            response.toString()
        } else {
            ""
        }
    }
}