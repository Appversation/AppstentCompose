package com.appstentcompose.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.appversation.appstentcompose.ViewContentRepository

/**
 * Factory for creating AppstentViewModel instances with dependencies
 */
class AppstentViewModelFactory(
    private val appstentRepository: ViewContentRepository,
    private val contentPath: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppstentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppstentViewModel(appstentRepository, contentPath) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
