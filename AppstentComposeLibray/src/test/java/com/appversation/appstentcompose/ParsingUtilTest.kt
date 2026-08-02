package com.appversation.appstentcompose

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsingUtilTest {
    @Test
    fun optBooleanReturnsRequestedFallbackWhenKeyIsMissing() {
        val descriptor = JSONObject()

        assertTrue(descriptor.optBoolean(keyName = "autoplay", fallback = true))
        assertFalse(descriptor.optBoolean(keyName = "autoplay", fallback = false))
    }

    @Test
    fun optBooleanReturnsRequestedFallbackWhenValueCannotBeParsed() {
        val descriptor = JSONObject().put("autoplay", "not-a-boolean")

        assertTrue(descriptor.optBoolean(keyName = "autoplay", fallback = true))
        assertFalse(descriptor.optBoolean(keyName = "autoplay", fallback = false))
    }

    @Test
    fun optBooleanPrefersAndroidOverrideAndUsesFallbackForMalformedOverride() {
        val overridden = JSONObject()
            .put("autoplay", false)
            .put("android:autoplay", true)
        val malformedOverride = JSONObject()
            .put("autoplay", false)
            .put("android:autoplay", "not-a-boolean")

        assertTrue(overridden.optBoolean(keyName = "autoplay", fallback = false))
        assertTrue(malformedOverride.optBoolean(keyName = "autoplay", fallback = true))
    }

    @Test
    fun optPlatformValuePrefersAndroidObjectOverride() {
        val sharedShadow = JSONObject().put("radius", 2)
        val androidShadow = JSONObject().put("radius", 8)
        val descriptor = JSONObject()
            .put("shadow", sharedShadow)
            .put("android:shadow", androidShadow)

        assertTrue(descriptor.optPlatformValue("shadow") === androidShadow)
    }

    @Test
    fun includedContentMergeAppliesWrapperOverridesWithoutReplacingIdentity() {
        val base = JSONObject()
            .put("type", "text")
            .put("text", "Base text")
            .put("foregroundColor", "#000000")
        val wrapper = JSONObject()
            .put("type", "included")
            .put("source", "conformance/included-base")
            .put("text", "Wrapper override")
            .put("padding", 12)

        val merged = mergeIncludedContent(base, wrapper)

        assertEquals("text", merged.getString("type"))
        assertEquals("Wrapper override", merged.getString("text"))
        assertEquals("#000000", merged.getString("foregroundColor"))
        assertEquals(12, merged.getInt("padding"))
        assertFalse(merged.has("source"))
        assertEquals("Base text", base.getString("text"))
    }
}
