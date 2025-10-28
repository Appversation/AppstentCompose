package com.appstentcompose.example

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.appversation.appstentcompose.ModuleConfigs.CustomContentViewProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application), CustomContentViewProvider {


    private val _navigateTo = MutableStateFlow<Screen?>(null)
    val navigateTo: StateFlow<Screen?> = _navigateTo.asStateFlow()

    fun onNavigate(screen: Screen) {
        _navigateTo.value = screen
    }

    fun onNavigationConsumed() {
        _navigateTo.value = null
    }

    @Composable
    override fun CustomComposable(viewName: String) {
        when (viewName) {
            else -> Text(text = "This is a default custom view")
        }
    }

    override fun getFontFamilyFrom(name: String): FontFamily {
        return when (name) {
            else        -> FontFamily.Default
        }
    }

    override fun visibility(ruleName: String, ruleValue: String): Boolean {
        when (ruleName) {
            "userState" -> return ruleValue == "loggedIn"
        }
        return true
    }

    init {
    }
}