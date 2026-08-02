package com.appversation.appstentcompose

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.MessageDigest

class ViewContentConformanceTest {
    @Test
    fun everyFixturePassesStructuralAssertionsBeforeRendering() {
        val manifest = resourceObject("manifest.json")
        val capabilityMatrix = resourceObject("capability-matrix.json")
        val fixtureLock = resourceObject("fixture-lock.json")
        val fixtures = manifest.getJSONArray("fixtures")
        val screenshotFixtureIds = setOf(
            "schema-parity",
            "frame-background-order",
            "text-alignment",
            "stack-spacing",
            "relative-sizing",
            "fill-modifier-sizing",
            "image-unframed-fit",
            "image-bounded-fit-fill",
            "intrinsic-stacks",
            "arranged-stacks-finite-bounds",
            "z-stack-child-sizing",
            "bounded-vertical-grid",
            "bounded-horizontal-grid",
            "list-viewport-sizing",
            "tabs",
            "navigation",
        )

        assertEquals(
            manifest.getString("contractVersion"),
            capabilityMatrix.getString("contractVersion"),
        )
        assertEquals(manifest.getString("contractVersion"), fixtureLock.getString("contractVersion"))
        assertEquals("sha256", fixtureLock.getString("algorithm"))

        val lockedFiles = fixtureLock.getJSONObject("files")
        val expectedFiles = buildSet {
            add("manifest.json")
            add("capability-matrix.json")
            repeat(fixtures.length()) { index ->
                add(fixtures.getJSONObject(index).getString("file"))
            }
        }
        val actualLockedFiles = buildSet {
            val keys = lockedFiles.keys()
            while (keys.hasNext()) add(keys.next())
        }
        assertEquals(expectedFiles, actualLockedFiles)
        expectedFiles.forEach { file ->
            assertEquals("$file: fixture lock mismatch", lockedFiles.getString(file), sha256(file))
        }

        val fixtureIds = mutableSetOf<String>()
        val expectedScreenshotFixtureIds = mutableSetOf<String>()
        repeat(fixtures.length()) { index ->
            val fixture = fixtures.getJSONObject(index)
            val fixtureId = fixture.getString("id")
            assertTrue("Duplicate fixture id: $fixtureId", fixtureIds.add(fixtureId))
            if (fixture.getString("baselinePolicy") != "structural-only") {
                expectedScreenshotFixtureIds.add(fixtureId)
            }

            val document = resourceObject(fixture.getString("file"))
            val assertions = fixture.getJSONArray("assertions")
            repeat(assertions.length()) { assertionIndex ->
                evaluateAssertion(
                    fixtureId = fixtureId,
                    document = document,
                    assertion = assertions.getJSONObject(assertionIndex),
                )
            }

            assertFalse("$fixtureId must have a non-empty type", document.optString("type").isEmpty())
        }

        val androidCapabilities = capabilityMatrix
            .getJSONObject("sdkVersions")
            .getJSONObject("AppstentCompose@1.7.1")
            .getJSONObject("capabilities")
        val capabilityIds = buildSet {
            val keys = androidCapabilities.keys()
            while (keys.hasNext()) {
                add(keys.next())
            }
        }
        assertEquals(fixtureIds, capabilityIds)
        assertEquals(expectedScreenshotFixtureIds, screenshotFixtureIds)
    }

    @Test
    fun manifestUsesFixedLogicalDimensionsAndExplicitNativeBaselines() {
        val manifest = resourceObject("manifest.json")
        val viewport = manifest.getJSONObject("defaultViewport")
        assertTrue(viewport.getDouble("width") > 0)
        assertTrue(viewport.getDouble("height") > 0)

        val fixtures = manifest.getJSONArray("fixtures")
        repeat(fixtures.length()) { index ->
            val fixture = fixtures.getJSONObject(index)
            val policy = fixture.getString("baselinePolicy")
            assertTrue(
                "${fixture.getString("id")} has an unsupported baseline policy",
                policy in setOf("shared", "platform", "structural-only"),
            )
            if (policy == "platform") {
                assertFalse(
                    "${fixture.getString("id")} must explain its native difference",
                    fixture.optString("nativeDifference").isEmpty(),
                )
            }
        }
    }

    private fun resourceObject(file: String): JSONObject {
        return JSONObject(resourceBytes(file).toString(Charsets.UTF_8))
    }

    private fun resourceBytes(file: String): ByteArray {
        val path = "viewcontent-conformance/$file"
        val stream = javaClass.classLoader?.getResourceAsStream(path)
        assertNotNull("Missing conformance resource: $path", stream)
        return stream!!.use { it.readBytes() }
    }

    private fun sha256(file: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(resourceBytes(file))
            .joinToString("") { "%02x".format(it) }
    }

    private fun evaluateAssertion(
        fixtureId: String,
        document: JSONObject,
        assertion: JSONObject,
    ) {
        val pointer = assertion.getString("path")
        val resolved = resolveJsonPointer(document, pointer)
        val message = "$fixtureId$pointer"

        if (assertion.optBoolean("absent", false)) {
            assertFalse("$message must be absent", resolved.exists)
            return
        }

        assertTrue("$message must exist", resolved.exists)
        when {
            assertion.has("equals") -> assertJsonEquals(
                message = message,
                expected = assertion.get("equals"),
                actual = resolved.value,
            )
            assertion.has("length") -> {
                val expectedLength = assertion.getInt("length")
                val actualLength = when (val value = resolved.value) {
                    is JSONArray -> value.length()
                    is String -> value.length
                    else -> fail("$message must be an array or string to assert length")
                }
                assertEquals("$message has an unexpected length", expectedLength, actualLength)
            }
            else -> fail("$message has no supported expectation")
        }
    }

    private fun assertJsonEquals(message: String, expected: Any, actual: Any?) {
        if (expected is Number && actual is Number) {
            assertEquals("$message has an unexpected value", expected.toDouble(), actual.toDouble(), 0.0)
        } else {
            assertEquals("$message has an unexpected value", expected, actual)
        }
    }

    private fun resolveJsonPointer(document: Any, pointer: String): Resolution {
        var value: Any? = document
        for (rawSegment in pointer.removePrefix("/").split("/")) {
            val segment = rawSegment.replace("~1", "/").replace("~0", "~")
            value = when (val container = value) {
                is JSONObject -> {
                    if (!container.has(segment)) return Resolution(false)
                    container.get(segment)
                }
                is JSONArray -> {
                    val index = segment.toIntOrNull() ?: return Resolution(false)
                    if (index !in 0 until container.length()) return Resolution(false)
                    container.get(index)
                }
                else -> return Resolution(false)
            }
        }
        return Resolution(true, value)
    }

    private data class Resolution(
        val exists: Boolean,
        val value: Any? = null,
    )
}
