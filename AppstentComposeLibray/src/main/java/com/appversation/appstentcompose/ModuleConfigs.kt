package com.appversation.appstentcompose

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

object ModuleConfigs {

    var deploymentStage = "demo"
    var apiKey = ""
    var customContentViewProvider: CustomContentViewProvider? = null

    interface CustomContentViewProvider {

        @Composable
        fun CustomComposable(viewName: String)

        fun getFontFamilyFrom(name: String) : FontFamily
    }

    interface CustomContentDataProvider {

        fun getStringFor(fieldName: String): String

        fun visibility(ruleName: String, ruleValue: String): Boolean
    }

    interface AppstentViewNavigationProvider {

        fun handleRoute(route: String)
    }
}