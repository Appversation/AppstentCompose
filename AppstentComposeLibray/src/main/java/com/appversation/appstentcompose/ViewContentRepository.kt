package com.appversation.appstentcompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Repository for fetching content from the Appstent backend
 */
class ViewContentRepository(val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    // Base URL for content
    val contentURL: String
        get() = NetworkConstants.BASE_URL + ModuleConfigs.deploymentStage + NetworkConstants.CONTENT_URL

    /**
     * Get content for a specific content ID
     */
    suspend fun getContent(forContentId: String): JSONObject {
        return withContext(Dispatchers.IO) {
            RequestHandler.requestGET(urlString = contentURL + forContentId)
        }
    }
    
    /**
     * Get all documents, optionally under a specific path
     */
    suspend fun getAllViewContents(subPath: String = ""): List<AppstentDoc> {
        return withContext(Dispatchers.IO) {
            val url = if (subPath.isEmpty()) {
                contentURL
            } else {
                "$contentURL/$subPath"
            }
            
            try {
                val response = RequestHandler.requestGET(urlString = url)
                parseDocumentsResponse(response)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Get all documents as a Flow, optionally under a specific path
     */
    fun getAllViewContentsFlow(subPath: String = ""): Flow<List<AppstentDoc>> = flow {
        emit(getAllViewContents(subPath))
    }
    
    /**
     * Parse the JSON response into a list of AppstentDoc objects
     */
    private fun parseDocumentsResponse(response: JSONObject): List<AppstentDoc> {
        val documents = mutableListOf<AppstentDoc>()
        
        try {
            if (response.has("documents")) {
                val docsArray = response.getJSONArray("documents")
                for (i in 0 until docsArray.length()) {
                    val docObj = docsArray.getJSONObject(i)
                    documents.add(parseDocObject(docObj))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return documents
    }
    
    /**
     * Parse a single JSON document object into an AppstentDoc
     */
    private fun parseDocObject(docObj: JSONObject): AppstentDoc {
        return AppstentDoc(
            id = docObj.optString("id", ""),
            name = docObj.optString("name", ""),
            path = docObj.optString("path", ""),
            isFolder = docObj.optBoolean("isFolder", false),
            content = if (docObj.has("content")) docObj.getString("content") else null,
            createdAt = Date(docObj.optLong("createdAt", System.currentTimeMillis())),
            updatedAt = Date(docObj.optLong("updatedAt", System.currentTimeMillis())),
            accountId = docObj.optString("accountId", "")
        )
    }
}