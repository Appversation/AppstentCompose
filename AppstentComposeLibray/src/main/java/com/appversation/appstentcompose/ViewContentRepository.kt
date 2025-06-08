package com.appversation.appstentcompose

import com.appversation.appstentcompose.RequestHandler.requestGET
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.Dispatchers.IO // Using Dispatchers.IO directly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
// import kotlinx.coroutines.runBlocking // Assuming not used elsewhere in this file
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
     * Get all documents recursively, optionally under a specific path
     */
    suspend fun getAllViewContents(subPath: String = ""): List<AppstentDoc> {
        return withContext(Dispatchers.IO) {
            
            try {
                val additionalHeaders = mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/json",
                    "x-api-key" to ModuleConfigs.apiKey,
                    "folder-prefix" to subPath)

                val responseString = RequestHandler.requestGET(URL(contentURL), additionalHeaders)

                val response = if (responseString.isNotEmpty()) JSONObject(responseString) else JSONObject()
                val initialDocs = parseDocumentsResponse(response)
                
                // Recursively get children for each folder
                val allDocs = mutableListOf<AppstentDoc>()
                val folders = initialDocs.filter { it.isFolder }
                
                allDocs.addAll(initialDocs) // Add initial documents and folders
                
                // For each folder, get its children recursively
                for (folder in folders) {
                    val folderPath = folder.path.ifEmpty { folder.name }
                    val children = getAllViewContents(folderPath)
                    
                    // Update the folder's children
                    val folderIndex = allDocs.indexOf(folder)
                    if (folderIndex != -1) {
                        allDocs[folderIndex] = folder.copy(children = children)
                    }
                }
                
                allDocs
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
     * Parse the S3-like JSON response into a list of AppstentDoc objects.
     * Paths in AppstentDoc will be relative to the basePrefix of the S3 listing.
     * IDs in AppstentDoc will be the full S3 key/prefix.
     */
    private fun parseDocumentsResponse(response: JSONObject): List<AppstentDoc> {
        val documents = mutableListOf<AppstentDoc>()
        val basePrefix = response.optString("Prefix", "") // e.g., "accountId/" or "accountId/subfolder/"
        val startAfter = response.optString("StartAfter", "")

        // Parse files from "Contents"
        if (response.has("Contents")) {
            val contentsArray = response.getJSONArray("Contents")
            for (i in 0 until contentsArray.length()) {
                val contentObj = contentsArray.getJSONObject(i)
                val key = contentObj.optString("Key", "")
                
                // Skip if key is empty, is the basePrefix itself (representing the folder being listed), or ends with / (another way S3 lists folders)
                if (key.isEmpty() || key == basePrefix || key.endsWith("/")) { 
                    continue
                }

                val relativeKey = if (key.startsWith(startAfter)) key.substring(startAfter.length) else key
                
                val name = relativeKey.substringAfterLast('/')

                documents.add(
                    AppstentDoc(
                        id = key, 
                        name = name,
                        path = relativeKey,
                        isFolder = false,
                        content = null, 
                        createdAt = parseS3DateString(contentObj.optString("LastModified")),
                        updatedAt = parseS3DateString(contentObj.optString("LastModified")),
                        accountId = "" // Will be filled by DocumentRepository
                    )
                )
            }
        }

        // Parse folders from "CommonPrefixes"
        if (response.has("CommonPrefixes")) {
            val commonPrefixesArray = response.getJSONArray("CommonPrefixes")

            for (i in 0 until commonPrefixesArray.length()) {
                val prefixObj = commonPrefixesArray.getJSONObject(i)
                val s3FolderPrefix = prefixObj.optString("Prefix", "") 
                
                if (s3FolderPrefix.isNotEmpty() && s3FolderPrefix.endsWith("/")) {
                    val relativeFolderPrefix = if (s3FolderPrefix.startsWith(startAfter)) s3FolderPrefix.substring(startAfter.length) else s3FolderPrefix
                    
                    val fullRelativePath = relativeFolderPrefix.dropLast(1) 
                    if (fullRelativePath.isEmpty()) continue

                    val name = fullRelativePath.substringAfterLast('/')

                    documents.add(
                        AppstentDoc(
                            id = s3FolderPrefix.dropLast(1), 
                            name = name,
                            path = fullRelativePath,
                            isFolder = true,
                            content = null,
                            createdAt = Date(), 
                            updatedAt = Date(),
                            accountId = "" // Will be filled by DocumentRepository
                        )
                    )
                }
            }
        }
        return documents
    }

    private fun parseS3DateString(dateString: String?): Date {
        if (dateString.isNullOrEmpty()) {
            return Date() 
        }
        return try {
            // S3 format: "2025-04-19T03:57:17.000Z"
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(dateString) ?: Date()
        } catch (e: Exception) {
            e.printStackTrace()
            Date() 
        }
    }
}