package com.appversation.appstentcompose

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

object ModuleConfigs {

    var deploymentStage = "demo"
    var apiKey = ""
    var inPreviewMode = false
    var previewSelectionHandler: ((String) -> Unit)? = null
    var customContentViewProvider: CustomContentViewProvider? = null

    private var pendingPreviewSelectionPath: String? = null
    private var isPreviewSelectionDispatchScheduled = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun reportPreviewSelection(path: String) {
        if (!inPreviewMode || path.isBlank()) {
            return
        }

        pendingPreviewSelectionPath = pendingPreviewSelectionPath
            ?.let { deepestPreviewPath(it, path) }
            ?: path

        if (isPreviewSelectionDispatchScheduled) {
            return
        }

        isPreviewSelectionDispatchScheduled = true
        mainHandler.post {
            val selectedPath = pendingPreviewSelectionPath

            pendingPreviewSelectionPath = null
            isPreviewSelectionDispatchScheduled = false

            if (!selectedPath.isNullOrBlank()) {
                previewSelectionHandler?.invoke(selectedPath)
            }
        }
    }

    private fun deepestPreviewPath(lhs: String, rhs: String): String {
        val lhsDepth = previewPathDepth(lhs)
        val rhsDepth = previewPathDepth(rhs)

        return when {
            rhsDepth > lhsDepth -> rhs
            rhsDepth == lhsDepth && rhs.length > lhs.length -> rhs
            else -> lhs
        }
    }

    private fun previewPathDepth(path: String): Int {
        return path.count { it == '.' || it == '[' }
    }

    interface CustomContentViewProvider {

        @Composable
        fun CustomComposable(viewName: String)

        fun getFontFamilyFrom(name: String) : FontFamily

        fun visibility(ruleName: String, ruleValue: String): Boolean
    }

    interface CustomContentDataProvider {

        fun getStringFor(fieldName: String): String

        fun getVisibility(fieldName: String): Boolean
    }

    interface AppstentViewNavigationProvider {

        fun handleRoute(route: String)
    }
}
