package com.appversation.appstentcompose

import androidx.compose.ui.text.font.FontFamily

object ModuleConfigs {

    var deploymentStage = "demo"
    var apiKey = ""
    var customContentDataProvider: CustomContentDataProvider? = null

    interface CustomContentDataProvider {
        fun getFontFamilyFrom(name: String) : FontFamily
        fun getStringFor(fieldName: String): String
    }
}