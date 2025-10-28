package com.appstentcompose.example
import com.appversation.appstentcompose.ModuleConfigs

class TestDataProvider: ModuleConfigs.CustomContentDataProvider {
    override fun getStringFor(fieldName: String): String {
        return "some value"
    }

    override fun getVisibility(fieldName: String): Boolean {
        return  when ( fieldName) {
            "lab_test_ranges", "lab_test_image" -> false
            else -> true
        }
    }
}