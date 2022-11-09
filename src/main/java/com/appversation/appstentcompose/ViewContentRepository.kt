package com.appversation.appstentcompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ViewContentRepository(val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {


    val contentURL:String
        get() = NetworkConstants.BASE_URL + ModuleConfigs.deploymentStage + NetworkConstants.CONTENT_URL

    suspend fun getContent(forContentId: String): JSONObject  {

        return withContext(Dispatchers.IO) {
            RequestHandler.requestGET(urlString = contentURL + forContentId)
        }
    }
}