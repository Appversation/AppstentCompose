package com.appversation.appstentcompose

import java.util.Date

/**
 * Model class representing a document or folder in the Appstent system
 */
data class AppstentDoc(
    val id: String = "",
    val name: String = "",
    val path: String = "",
    val isFolder: Boolean = false,
    val content: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val accountId: String = "",
    val children: List<AppstentDoc>? = null
) {
    /**
     * Get the display name (last component of the path)
     */
    fun getDisplayName(): String {
        return if (path.isEmpty()) {
            name
        } else {
            val components = path.split("/")
            if (components.isNotEmpty()) components.last() else name
        }
    }
}
