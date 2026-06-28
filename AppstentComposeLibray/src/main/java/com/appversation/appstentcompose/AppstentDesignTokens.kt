package com.appversation.appstentcompose

import org.json.JSONArray
import org.json.JSONObject

enum class AppstentDesignTokenDiagnosticCode {
    MissingToken,
    CircularReference,
    TypeMismatch,
    UnsupportedUnit,
    InvalidDocument
}

data class AppstentDesignTokenDiagnostic(
    val code: AppstentDesignTokenDiagnosticCode,
    val message: String,
    val path: String? = null
)

data class AppstentDesignToken(
    val path: String,
    val type: String?,
    val value: Any?
)

data class AppstentDesignTokenResolution<T>(
    val value: T,
    val diagnostics: List<AppstentDesignTokenDiagnostic> = emptyList()
)

data class AppstentRemoteDesignTokenMetadata(
    val active: Boolean,
    val fileName: String?,
    val s3Key: String?,
    val contentType: String,
    val tokenCount: Int,
    val checksum: String?,
    val updatedBy: String?
)

data class AppstentRemoteDesignTokens(
    val contentEnvironment: String,
    val metadata: AppstentRemoteDesignTokenMetadata?,
    val tokens: JSONObject?,
    val resolver: AppstentDesignTokenResolver
) {
    val hasActiveTokens: Boolean
        get() = metadata?.active == true && tokens != null
}

class AppstentDesignTokenResolver(
    val tokens: Map<String, AppstentDesignToken> = emptyMap(),
    val diagnostics: List<AppstentDesignTokenDiagnostic> = emptyList()
) {
    constructor(jsonObject: JSONObject) : this(
        tokens = flattenTokens(jsonObject).first,
        diagnostics = flattenTokens(jsonObject).second
    )

    fun token(path: String): AppstentDesignToken? = tokens[path]

    fun resolveRaw(literalOrReference: String): AppstentDesignTokenResolution<Any?> {
        if (!isTokenReference(literalOrReference)) {
            return AppstentDesignTokenResolution(literalOrReference)
        }

        return resolveToken(tokenPath(literalOrReference), emptyList())
    }

    fun resolveString(literalOrReference: String): AppstentDesignTokenResolution<String> {
        if (!isTokenReference(literalOrReference)) {
            return AppstentDesignTokenResolution(literalOrReference)
        }

        val raw = resolveRaw(literalOrReference)
        return when (val value = raw.value) {
            is String -> AppstentDesignTokenResolution(value, raw.diagnostics)
            is Number -> AppstentDesignTokenResolution(cleanNumber(value.toDouble()), raw.diagnostics)
            null -> AppstentDesignTokenResolution(literalOrReference, raw.diagnostics)
            else -> AppstentDesignTokenResolution(
                literalOrReference,
                raw.diagnostics + typeMismatch(literalOrReference, "string")
            )
        }
    }

    fun resolveColor(literalOrReference: String): AppstentDesignTokenResolution<String> =
        typedString(literalOrReference, setOf("color"))

    fun resolveFontFamily(literalOrReference: String): AppstentDesignTokenResolution<String> =
        typedString(literalOrReference, setOf("fontFamily", "fontFamilies"))

    fun resolveFontWeight(literalOrReference: String): AppstentDesignTokenResolution<String> =
        typedString(literalOrReference, setOf("fontWeight"))

    fun resolveNumber(literalOrReference: String): AppstentDesignTokenResolution<Double?> {
        if (!isTokenReference(literalOrReference)) {
            return AppstentDesignTokenResolution(literalOrReference.toDoubleOrNull())
        }

        val raw = resolveRaw(literalOrReference)
        return when (val value = raw.value) {
            is Number -> AppstentDesignTokenResolution(value.toDouble(), raw.diagnostics)
            is String -> {
                val parsed = parseDimension(value)
                if (parsed != null) {
                    AppstentDesignTokenResolution(parsed, raw.diagnostics)
                } else {
                    AppstentDesignTokenResolution(
                        null,
                        raw.diagnostics + AppstentDesignTokenDiagnostic(
                            code = AppstentDesignTokenDiagnosticCode.UnsupportedUnit,
                            message = "Unsupported dimension token value \"$value\".",
                            path = tokenPath(literalOrReference)
                        )
                    )
                }
            }
            else -> AppstentDesignTokenResolution(
                null,
                raw.diagnostics + typeMismatch(literalOrReference, "number or dimension")
            )
        }
    }

    fun resolveTypography(literalOrReference: String): AppstentDesignTokenResolution<JSONObject?> =
        typedObject(literalOrReference, setOf("typography"))

    fun resolveShadow(literalOrReference: String): AppstentDesignTokenResolution<JSONObject?> =
        typedObject(literalOrReference, setOf("shadow", "boxShadow"))

    private fun typedString(
        literalOrReference: String,
        expectedTypes: Set<String>
    ): AppstentDesignTokenResolution<String> {
        if (!isTokenReference(literalOrReference)) {
            return AppstentDesignTokenResolution(literalOrReference)
        }

        val raw = resolveRaw(literalOrReference)
        val typeDiagnostics = typeDiagnostics(literalOrReference, expectedTypes)
        return if (raw.value is String) {
            AppstentDesignTokenResolution(raw.value, raw.diagnostics + typeDiagnostics)
        } else {
            AppstentDesignTokenResolution(
                literalOrReference,
                raw.diagnostics + typeDiagnostics + typeMismatch(literalOrReference, expectedTypes.joinToString(" or "))
            )
        }
    }

    private fun typedObject(
        literalOrReference: String,
        expectedTypes: Set<String>
    ): AppstentDesignTokenResolution<JSONObject?> {
        if (!isTokenReference(literalOrReference)) {
            return AppstentDesignTokenResolution(null)
        }

        val raw = resolveRaw(literalOrReference)
        val typeDiagnostics = typeDiagnostics(literalOrReference, expectedTypes)
        return if (raw.value is JSONObject) {
            AppstentDesignTokenResolution(raw.value, raw.diagnostics + typeDiagnostics)
        } else {
            AppstentDesignTokenResolution(
                null,
                raw.diagnostics + typeDiagnostics + typeMismatch(literalOrReference, expectedTypes.joinToString(" or "))
            )
        }
    }

    private fun resolveToken(path: String, stack: List<String>): AppstentDesignTokenResolution<Any?> {
        val token = tokens[path]
            ?: return AppstentDesignTokenResolution(
                null,
                listOf(
                    AppstentDesignTokenDiagnostic(
                        code = AppstentDesignTokenDiagnosticCode.MissingToken,
                        message = "No design token named \"$path\".",
                        path = path
                    )
                )
            )

        if (stack.contains(path)) {
            return AppstentDesignTokenResolution(
                null,
                listOf(
                    AppstentDesignTokenDiagnostic(
                        code = AppstentDesignTokenDiagnosticCode.CircularReference,
                        message = "Circular design token alias: ${(stack + path).joinToString(" -> ")}.",
                        path = path
                    )
                )
            )
        }

        val value = token.value
        if (value is String && isTokenReference(value)) {
            return resolveToken(tokenPath(value), stack + path)
        }

        return AppstentDesignTokenResolution(value)
    }

    private fun typeDiagnostics(
        literalOrReference: String,
        expectedTypes: Set<String>
    ): List<AppstentDesignTokenDiagnostic> {
        val token = tokens[tokenPath(literalOrReference)] ?: return emptyList()
        val type = token.type ?: return emptyList()
        if (expectedTypes.contains(type)) {
            return emptyList()
        }
        return listOf(typeMismatch(literalOrReference, expectedTypes.joinToString(" or ")))
    }

    private fun typeMismatch(reference: String, expected: String): AppstentDesignTokenDiagnostic =
        AppstentDesignTokenDiagnostic(
            code = AppstentDesignTokenDiagnosticCode.TypeMismatch,
            message = "Design token $reference is not a $expected token.",
            path = tokenPath(reference)
        )

    companion object {
        private val tokenReferencePattern = Regex("^\\{[A-Za-z0-9_.-]+\\}$")
        private val dimensionPattern = Regex("^(-?\\d+(?:\\.\\d+)?)(px|pt|dp|sp)$")

        fun isTokenReference(value: String): Boolean = tokenReferencePattern.matches(value)

        internal fun tokenPath(reference: String): String =
            reference.removePrefix("{").removeSuffix("}")

        private fun flattenTokens(root: JSONObject): Pair<Map<String, AppstentDesignToken>, List<AppstentDesignTokenDiagnostic>> {
            val tokens = linkedMapOf<String, AppstentDesignToken>()
            val diagnostics = mutableListOf<AppstentDesignTokenDiagnostic>()
            walkGroup(root, emptyList(), inheritedType = null, tokens = tokens, diagnostics = diagnostics)
            return tokens to diagnostics
        }

        private fun walkGroup(
            group: JSONObject,
            path: List<String>,
            inheritedType: String?,
            tokens: MutableMap<String, AppstentDesignToken>,
            diagnostics: MutableList<AppstentDesignTokenDiagnostic>
        ) {
            val groupType = group.optString("\$type", inheritedType ?: "").takeIf { it.isNotEmpty() }

            if (group.has("\$value")) {
                val tokenPath = path.joinToString(".")
                if (tokenPath.isBlank()) {
                    diagnostics.add(
                        AppstentDesignTokenDiagnostic(
                            code = AppstentDesignTokenDiagnosticCode.InvalidDocument,
                            message = "Design token at document root is missing a path."
                        )
                    )
                } else {
                    tokens[tokenPath] = AppstentDesignToken(
                        path = tokenPath,
                        type = group.optString("\$type", groupType ?: "").takeIf { it.isNotEmpty() },
                        value = normalizeValue(group.opt("\$value"))
                    )
                }
                return
            }

            val keys = group.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.startsWith("$")) {
                    continue
                }

                val child = group.opt(key)
                if (child is JSONObject) {
                    walkGroup(child, path + key, groupType, tokens, diagnostics)
                }
            }
        }

        private fun normalizeValue(value: Any?): Any? {
            return when (value) {
                JSONObject.NULL -> null
                is JSONObject -> value
                is JSONArray -> value
                else -> value
            }
        }

        private fun parseDimension(value: String): Double? {
            val match = dimensionPattern.matchEntire(value.trim()) ?: return null
            return match.groupValues[1].toDoubleOrNull()
        }

        private fun cleanNumber(value: Double): String {
            return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        }
    }
}

internal fun JSONObject.appstentResolvedString(
    keyName: String,
    fallback: String = "",
    resolver: AppstentDesignTokenResolver = ModuleConfigs.designTokenResolver
): String {
    val rawValue = appstentRawValue(keyName)?.toString() ?: fallback
    if (rawValue.isEmpty() || !AppstentDesignTokenResolver.isTokenReference(rawValue)) {
        return rawValue
    }
    return resolver.resolveString(rawValue).value
}

internal fun JSONObject.appstentResolvedDouble(
    keyName: String,
    fallback: Double = 0.0,
    resolver: AppstentDesignTokenResolver = ModuleConfigs.designTokenResolver
): Double {
    val value = appstentRawValue(keyName) ?: return fallback
    if (value is Number) {
        return value.toDouble()
    }

    val stringValue = value?.toString() ?: return fallback
    if (AppstentDesignTokenResolver.isTokenReference(stringValue)) {
        return resolver.resolveNumber(stringValue).value ?: fallback
    }
    return stringValue.toDoubleOrNull() ?: fallback
}

internal fun JSONObject.appstentResolvedInt(
    keyName: String,
    fallback: Int = 0,
    resolver: AppstentDesignTokenResolver = ModuleConfigs.designTokenResolver
): Int = appstentResolvedDouble(keyName, fallback.toDouble(), resolver).toInt()

private fun JSONObject.appstentRawValue(keyName: String): Any? {
    val androidValue = opt("android:$keyName")
    if (androidValue != null && androidValue != JSONObject.NULL) {
        return androidValue
    }

    val value = opt(keyName)
    if (value != null && value != JSONObject.NULL) {
        return value
    }

    return null
}
