package com.appversation.appstentcompose

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppstentDesignTokensTest {
    @Test
    fun resolver_flattensNestedDtcgTokensAndInheritsType() {
        val resolver = AppstentDesignTokenResolver(
            JSONObject(
                """
                {
                  "color": {
                    "${'$'}type": "color",
                    "text": {
                      "primary": { "${'$'}value": "#123456" }
                    }
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals("#123456", resolver.resolveColor("{color.text.primary}").value)
        assertEquals("color", resolver.token("color.text.primary")?.type)
    }

    @Test
    fun resolver_resolvesAliasesAndDetectsCycles() {
        val resolver = AppstentDesignTokenResolver(
            JSONObject(
                """
                {
                  "color": {
                    "${'$'}type": "color",
                    "primary": { "${'$'}value": "#146C78" },
                    "alias": { "${'$'}value": "{color.primary}" },
                    "loopA": { "${'$'}value": "{color.loopB}" },
                    "loopB": { "${'$'}value": "{color.loopA}" }
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals("#146C78", resolver.resolveColor("{color.alias}").value)

        val cycle = resolver.resolveColor("{color.loopA}")
        assertEquals("{color.loopA}", cycle.value)
        assertTrue(cycle.diagnostics.any { it.code == AppstentDesignTokenDiagnosticCode.CircularReference })
    }

    @Test
    fun resolver_parsesSupportedDimensionsAndReportsUnsupportedUnits() {
        val resolver = AppstentDesignTokenResolver(
            JSONObject(
                """
                {
                  "space": {
                    "${'$'}type": "dimension",
                    "stack": { "${'$'}value": "16dp" },
                    "bad": { "${'$'}value": "2rem" }
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals(16.0, resolver.resolveNumber("{space.stack}").value!!, 0.001)

        val unsupported = resolver.resolveNumber("{space.bad}")
        assertNull(unsupported.value)
        assertTrue(unsupported.diagnostics.any { it.code == AppstentDesignTokenDiagnosticCode.UnsupportedUnit })
    }

    @Test
    fun resolver_returnsDiagnosticsForMissingAndMismatchedTokens() {
        val resolver = AppstentDesignTokenResolver(
            JSONObject(
                """
                {
                  "space": {
                    "${'$'}type": "dimension",
                    "stack": { "${'$'}value": 12 }
                  }
                }
                """.trimIndent()
            )
        )

        val missing = resolver.resolveColor("{color.missing}")
        assertEquals("{color.missing}", missing.value)
        assertTrue(missing.diagnostics.any { it.code == AppstentDesignTokenDiagnosticCode.MissingToken })

        val mismatch = resolver.resolveColor("{space.stack}")
        assertEquals("{space.stack}", mismatch.value)
        assertTrue(mismatch.diagnostics.any { it.code == AppstentDesignTokenDiagnosticCode.TypeMismatch })
    }

    @Test
    fun resolver_exposesTypographyAndShadowObjects() {
        val resolver = AppstentDesignTokenResolver(
            JSONObject(
                """
                {
                  "text": {
                    "${'$'}type": "typography",
                    "body": {
                      "${'$'}value": {
                        "fontFamily": "sansSerif",
                        "fontSize": "16sp"
                      }
                    }
                  },
                  "shadow": {
                    "${'$'}type": "shadow",
                    "card": {
                      "${'$'}value": {
                        "color": "#000000",
                        "radius": "8dp"
                      }
                    }
                  }
                }
                """.trimIndent()
            )
        )

        assertEquals("sansSerif", resolver.resolveTypography("{text.body}").value?.getString("fontFamily"))
        assertEquals("#000000", resolver.resolveShadow("{shadow.card}").value?.getString("color"))
    }

    @Test
    fun jsonHelpersResolveTokenizedStringsAndNumbers() {
        val originalResolver = ModuleConfigs.designTokenResolver
        try {
            ModuleConfigs.designTokenResolver = AppstentDesignTokenResolver(
                JSONObject(
                    """
                    {
                      "space": {
                        "${'$'}type": "dimension",
                        "stack": { "${'$'}value": "24dp" }
                      },
                      "font": {
                        "${'$'}type": "fontFamily",
                        "body": { "${'$'}value": "sansSerif" }
                      }
                    }
                    """.trimIndent()
                )
            )

            val content = JSONObject("""{"padding":"{space.stack}","font":"{font.body}"}""")

            assertEquals(24.0, content.appstentResolvedDouble("padding"), 0.001)
            assertEquals("sansSerif", content.appstentResolvedString("font"))
            assertFalse(AppstentDesignTokenResolver.isTokenReference("prefix {space.stack}"))
        } finally {
            ModuleConfigs.designTokenResolver = originalResolver
        }
    }

    @Test
    fun jsonHelpersPreferAndroidOverrides() {
        val originalResolver = ModuleConfigs.designTokenResolver
        try {
            ModuleConfigs.designTokenResolver = AppstentDesignTokenResolver(
                JSONObject(
                    """
                    {
                      "size": {
                        "${'$'}type": "dimension",
                        "card": { "${'$'}value": "320dp" }
                      }
                    }
                    """.trimIndent()
                )
            )

            val content = JSONObject("""{"width": 120, "android:width": "{size.card}"}""")

            assertEquals(320.0, content.appstentResolvedDouble("width"), 0.001)
        } finally {
            ModuleConfigs.designTokenResolver = originalResolver
        }
    }
}
