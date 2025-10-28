package com.appstentcompose.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appversation.appstentcompose.ModuleConfigs
import com.appversation.appstentcompose.ViewContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for handling Appstent content
 */
class AppstentViewModel(
    private val appstentRepository: ViewContentRepository,
    private val contentPath: String
) : ViewModel() {

    // Content state
    private val _viewContent = MutableStateFlow(JSONObject())
    val viewContent: StateFlow<JSONObject> = _viewContent.asStateFlow()

    // Loading state
    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()

    // Error state
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        // Default API key for loading static content
        ModuleConfigs.apiKey = "8CB8D395-1604-461A-8B4B-EAACCBD5EE48"

        loadContent()
    }

    /**
     * Load content from the repository
     */
    fun loadContent() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _loadingState.value = true
                _errorState.value = null

                val content = appstentRepository.getContent(contentPath)
                _viewContent.emit(content)

            } catch (e: Exception) {
                _errorState.value = e.message ?: "Unknown error occurred"
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _errorState.value = null
    }
}