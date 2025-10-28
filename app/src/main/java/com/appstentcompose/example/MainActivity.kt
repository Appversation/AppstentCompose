package com.appstentcompose.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHost
import com.appstentcompose.example.ui.theme.AppstentComposeTheme
import com.appversation.appstentcompose.AppstentView
import com.appversation.appstentcompose.ModuleConfigs
import com.appversation.appstentcompose.ViewContentRepository
import org.json.JSONObject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
                val application = LocalContext.current.applicationContext as Application
                val mainViewModel: MainViewModel = viewModel(factory = ViewModelFactory(application))

                ModuleConfigs.customContentViewProvider = mainViewModel
                val navController = rememberNavController()

                val startDestination = Screen.Main.route

                val navigateTo by mainViewModel.navigateTo.collectAsStateWithLifecycle()
                LaunchedEffect(navigateTo) {
                    navigateTo?.let {
                        navController.navigate(it.route)
                        mainViewModel.onNavigationConsumed()
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    composable(Screen.Main.route) {
                        MainContent()
                    }
                }

        }
    }
}

@Composable
fun MainContent(viewModel: AppstentViewModel = viewModel(
    key = "Coherent/MainTabs",
    factory = AppstentViewModelFactory(
        ViewContentRepository(),
        "Coherent/MainTabs"
    )
)) {
    val viewContent by viewModel.viewContent.collectAsStateWithLifecycle(initialValue = JSONObject())

    AppstentView(viewContent = viewContent, customContentDataProvider = TestDataProvider())
}