package com.appversation.appstentcompose

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

object ModuleConfigs {

    var deploymentStage = "demo"
    var apiKey = ""
    var customContentDataProvider: CustomContentDataProvider? = null

    interface CustomContentDataProvider {
        fun getFontFamilyFrom(name: String) : FontFamily
        fun getStringFor(fieldName: String): String

        @Composable
        fun CustomComposable(viewName: String)
    }

    interface AppstentViewNavigationProvider {

        fun handleRoute(route: String)
    }
}