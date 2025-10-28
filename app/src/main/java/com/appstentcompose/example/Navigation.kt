package com.appstentcompose.example

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Login : Screen("login")
    object LabResults : Screen("lab_reports_view")
}