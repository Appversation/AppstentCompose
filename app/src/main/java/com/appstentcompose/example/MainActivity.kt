package com.appstentcompose.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.appversation.appstentcompose.AppstentView
import com.appversation.appstentcompose.ModuleConfigs
import com.appversation.appstentcompose.ViewContentRepository
import org.json.JSONObject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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

                val appstentViewModel: AppstentViewModel = viewModel(
                    key = "Coherent/Test",
                    factory = AppstentViewModelFactory(
                        ViewContentRepository(),
                        "Coherent/Test"
                    )
                )
                val viewContent by appstentViewModel.viewContent.collectAsStateWithLifecycle(initialValue = JSONObject())

                val startDestination = Screen.Main.route

                val navigateTo by mainViewModel.navigateTo.collectAsStateWithLifecycle()
                LaunchedEffect(navigateTo) {
                    navigateTo?.let {
                        navController.navigate(it.route)
                        mainViewModel.onNavigationConsumed()
                    }
                }

                NavHost(navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                )
                {

                    composable(Screen.Main.route) {
                        MainContent(navController = navController, viewContent)
                    }
                    composable("lab_reports_view") {
                        // You need to decide what to show here.
                        // For example, you could create a new Composable called LabReportsScreen()
                        // Or just put a placeholder for now:
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Text(text = "This is the Lab Reports Screen")
                        }
                    }
                }

        }
    }
}

@Composable
fun MainContent(navController: NavHostController, viewContent: JSONObject) {

    AppstentView(modifier = Modifier.fillMaxSize(), viewContent = viewContent, navController = navController)
}
