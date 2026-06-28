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
        get() = NetworkConstants.BASE_URL + ModuleConfigs.deploymentStageName + NetworkConstants.CONTENT_URL

    /**
     * Get content for a specific content ID
     */
    suspend fun getContent(forContentId: String): JSONObject {
        return withContext(Dispatchers.IO) {
            RequestHandler.requestGET(urlString = contentURL + forContentId)
        }
    }

    /**
     * Validate that a content environment can be accessed with the configured API key.
     */
    suspend fun validateContentEnvironment(
        contentEnvironment: String = ModuleConfigs.contentEnvironment
    ): ContentEnvironmentValidationResult {
        return withContext(Dispatchers.IO) {
            val selectedEnvironment = ModuleConfigs.normalizeContentEnvironment(contentEnvironment)
            val url = URL(contentURL)
            val response = RequestHandler.requestGETResponse(
                url = url,
                additionalHeaders = RequestHandler.appstentHeaders(contentEnvironment = selectedEnvironment)
            )

            if (response.statusCode in 200..299) {
                ContentEnvironmentValidationResult(
                    contentEnvironment = selectedEnvironment,
                    isValid = true,
                    statusCode = response.statusCode,
                    message = null
                )
            } else {
                val error = ViewContentRequestException(
                    statusCode = response.statusCode,
                    body = response.body,
                    contentEnvironment = selectedEnvironment,
                    url = url
                )

                ContentEnvironmentValidationResult(
                    contentEnvironment = selectedEnvironment,
                    isValid = false,
                    statusCode = response.statusCode,
                    message = error.message
                )
            }
        }
    }

    /**
     * Throw a typed request exception unless the content environment is active.
     */
    suspend fun assertContentEnvironmentIsActive(
        contentEnvironment: String = ModuleConfigs.contentEnvironment
    ) {
        withContext(Dispatchers.IO) {
            val selectedEnvironment = ModuleConfigs.normalizeContentEnvironment(contentEnvironment)
            RequestHandler.requestGET(
                url = URL(contentURL),
                additionalHeaders = RequestHandler.appstentHeaders(contentEnvironment = selectedEnvironment)
            )
        }
    }

    /**
     * Fetch active DTCG design tokens for the configured content environment.
     */
    suspend fun fetchActiveDesignTokens(
        contentEnvironment: String = ModuleConfigs.contentEnvironment
    ): AppstentRemoteDesignTokens {
        return withContext(Dispatchers.IO) {
            val selectedEnvironment = ModuleConfigs.normalizeContentEnvironment(contentEnvironment)
            val url = URL(contentURL + "__appstent/design-tokens")
            val responseString = RequestHandler.requestGET(
                url = url,
                additionalHeaders = RequestHandler.appstentHeaders(contentEnvironment = selectedEnvironment)
            )
            val response = if (responseString.isNotEmpty()) JSONObject(responseString) else JSONObject()
            parseRemoteDesignTokens(response, selectedEnvironment)
        }
    }

    /**
     * Fetch active DTCG design tokens and install the resolver into ModuleConfigs.
     *
     * If the backend returns 404 because no active tokens are configured, the resolver is cleared
     * and an empty remote token result is returned so existing content keeps rendering.
     */
    suspend fun loadActiveDesignTokens(
        contentEnvironment: String = ModuleConfigs.contentEnvironment
    ): AppstentRemoteDesignTokens {
        return try {
            fetchActiveDesignTokens(contentEnvironment).also {
                ModuleConfigs.setDesignTokens(it.resolver)
            }
        } catch (error: ViewContentRequestException) {
            if (error.statusCode != 404) {
                throw error
            }

            val selectedEnvironment = ModuleConfigs.normalizeContentEnvironment(contentEnvironment)
            ModuleConfigs.clearDesignTokens()
            AppstentRemoteDesignTokens(
                contentEnvironment = selectedEnvironment,
                metadata = null,
                tokens = null,
                resolver = AppstentDesignTokenResolver()
            )
        }
    }
    
    /**
     * Get all documents recursively, optionally under a specific path
     */
    suspend fun getAllViewContents(subPath: String = ""): List<AppstentDoc> {
        return withContext(Dispatchers.IO) {
            
            try {
                val additionalHeaders = RequestHandler.appstentHeaders(
                    extraHeaders = mapOf("folder-prefix" to subPath)
                )

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
                if (e is ViewContentRequestException || e is IllegalArgumentException) {
                    throw e
                }

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

    private fun parseRemoteDesignTokens(
        response: JSONObject,
        selectedEnvironment: String
    ): AppstentRemoteDesignTokens {
        val tokens = response.optJSONObject("tokens")
        return AppstentRemoteDesignTokens(
            contentEnvironment = response.optString("contentEnvironment", selectedEnvironment),
            metadata = response.optJSONObject("designTokens")?.let(::parseRemoteDesignTokenMetadata),
            tokens = tokens,
            resolver = tokens?.let { AppstentDesignTokenResolver(it) } ?: AppstentDesignTokenResolver()
        )
    }

    private fun parseRemoteDesignTokenMetadata(metadata: JSONObject): AppstentRemoteDesignTokenMetadata {
        return AppstentRemoteDesignTokenMetadata(
            active = metadata.optBoolean("active", false),
            fileName = metadata.optString("fileName").takeIf { it.isNotEmpty() },
            s3Key = metadata.optString("s3Key").takeIf { it.isNotEmpty() },
            contentType = metadata.optString("contentType", "application/json"),
            tokenCount = metadata.optInt("tokenCount", 0),
            checksum = metadata.optString("checksum")
                .takeIf { it.isNotEmpty() }
                ?: metadata.optString("version").takeIf { it.isNotEmpty() }?.let { "sha256:$it" },
            updatedBy = metadata.optString("updatedBy").takeIf { it.isNotEmpty() }
        )
    }
}
