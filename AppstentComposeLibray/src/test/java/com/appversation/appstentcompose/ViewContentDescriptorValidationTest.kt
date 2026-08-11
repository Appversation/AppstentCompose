package com.appversation.appstentcompose

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ViewContentDescriptorValidationTest {
    @Test
    fun unversionedDescriptorsRetainPermissiveParsing() {
        validateViewContentDescriptor(JSONObject().put("views", JSONArray().put(JSONObject())))
    }

    @Test
    fun nestedMissingTypeReportsDescriptorPath() {
        val descriptor = JSONObject()
            .put("schemaVersion", "1.0.0")
            .put("type", "vStack")
            .put(
                "views",
                JSONArray()
                    .put(JSONObject().put("type", "divider"))
                    .put(JSONObject().put("text", "Missing type")),
            )

        val error = assertThrows(ViewContentDescriptorException::class.java) {
            validateViewContentDescriptor(descriptor)
        }

        assertEquals("$.views[1].type", error.descriptorPath)
        assertEquals("$.views[1].type: Missing required property", error.message)
    }

    @Test
    fun malformedSectionReportsStructuralPath() {
        val descriptor = JSONObject()
            .put("schemaVersion", "1.0.0")
            .put("type", "list")
            .put("sections", JSONArray().put(JSONObject().put("title", "Section")))

        val error = assertThrows(ViewContentDescriptorException::class.java) {
            validateViewContentDescriptor(descriptor)
        }

        assertEquals("$.sections[0].views", error.descriptorPath)
    }

    @Test
    fun tabMissingContentReportsTabPath() {
        val descriptor = JSONObject()
            .put("schemaVersion", "1.0.0")
            .put("type", "tabbedView")
            .put("tabs", JSONArray().put(JSONObject().put("title", "First")))

        val error = assertThrows(ViewContentDescriptorException::class.java) {
            validateViewContentDescriptor(descriptor)
        }

        assertEquals("$.tabs[0].tabContent", error.descriptorPath)
    }

    @Test
    fun validVersionedDescriptorPassesRecursiveValidation() {
        val descriptor = JSONObject()
            .put("schemaVersion", "1.0.0")
            .put("type", "vStack")
            .put(
                "views",
                JSONArray()
                    .put(JSONObject().put("type", "text").put("text", "Hello"))
                    .put(
                        JSONObject()
                            .put("type", "gradientView")
                            .put("colors", JSONArray().put("#000000")),
                    ),
            )

        validateViewContentDescriptor(descriptor)
    }
}
