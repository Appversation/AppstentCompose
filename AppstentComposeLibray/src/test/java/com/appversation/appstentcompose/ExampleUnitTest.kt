package com.appversation.appstentcompose

import org.junit.Test

import org.junit.Assert.*
import java.net.URL

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun contentEnvironment_isNormalized() {
        val originalEnvironment = ModuleConfigs.contentEnvironment

        try {
            ModuleConfigs.contentEnvironment = " QA_1 "
            assertEquals("qa_1", ModuleConfigs.normalizedContentEnvironment)
        } finally {
            ModuleConfigs.contentEnvironment = originalEnvironment
        }
    }

    @Test
    fun invalidContentEnvironment_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ModuleConfigs.normalizeContentEnvironment("../prod")
        }
    }

    @Test
    fun requestException_readsBackendMessage() {
        val error = ViewContentRequestException(
            statusCode = 404,
            body = """{"message":"Content environment not found or inactive"}""",
            contentEnvironment = "qa",
            url = URL("https://example.com/content/")
        )

        assertEquals("Content environment not found or inactive", error.responseMessage)
        assertTrue(error.message!!.contains("contentEnvironment \"qa\""))
    }
}
