package com.appversation.appstentcompose

import org.json.JSONArray
import org.json.JSONObject

private val canonicalPropertyAliases = mapOf(
    "textAlignment" to listOf("text_alignment"),
    "percentWidth" to listOf("percent_width"),
    "percentHeight" to listOf("percent_height"),
    "minWidth" to listOf("min_width"),
    "maxWidth" to listOf("max_width"),
    "minHeight" to listOf("min_height"),
    "maxHeight" to listOf("max_height"),
    "overlayView" to listOf("overlay_view"),
)

private fun propertyCandidates(keyName: String): List<String> {
    val canonicalKey = canonicalPropertyAliases.entries
        .firstOrNull { (canonical, aliases) -> keyName == canonical || keyName in aliases }
        ?.key
        ?: keyName
    return listOf(canonicalKey) + canonicalPropertyAliases[canonicalKey].orEmpty()
}

private fun JSONObject.resolvedKey(keyName: String): String? {
    val candidates = propertyCandidates(keyName)
    return candidates
        .map { "android:$it" }
        .firstOrNull { this.has(it) }
        ?: candidates.firstOrNull { this.has(it) }
}

fun JSONObject.has(keyName: String): Boolean = resolvedKey(keyName) != null

fun JSONObject.getString(keyName: String): String =
    getString(resolvedKey(keyName) ?: keyName)

fun JSONObject.optString(keyName: String, fallback: String): String {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optString(resolvedKey, fallback)
}

fun JSONObject.getDouble(keyName: String): Double =
    getDouble(resolvedKey(keyName) ?: keyName)

fun JSONObject.optDouble(keyName: String, fallback: Double): Double {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optDouble(resolvedKey, fallback)
}

fun JSONObject.getInt(keyName: String): Int =
    getInt(resolvedKey(keyName) ?: keyName)

fun JSONObject.optInt(keyName: String, fallback: Int): Int {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optInt(resolvedKey, fallback)
}

fun JSONObject.optBoolean(keyName: String, fallback: Boolean): Boolean {
    val resolvedKey = resolvedKey(keyName) ?: return fallback
    return optBoolean(resolvedKey, fallback)
}

fun JSONObject.optPlatformValue(keyName: String): Any? =
    resolvedKey(keyName)?.let(::opt)

fun JSONObject.getJSONArray(keyName: String): JSONArray =
    resolvedKey(keyName)?.let(::getJSONArray) ?: JSONArray()

fun JSONObject.getJSONObject(keyName: String): JSONObject =
    resolvedKey(keyName)?.let(::getJSONObject) ?: JSONObject()

private val supportedViewTypes = setOf(
    "divider",
    "spacer",
    "text",
    "link",
    "image",
    "video",
    "webView",
    "tabbedView",
    "navigationView",
    "navigationLink",
    "list",
    "grid",
    "hStack",
    "vStack",
    "zStack",
    "disclosureGroup",
    "menu",
    "custom",
    "included",
    "gradientView",
    "progressView",
)

/**
 * Validates the recursive requirements of the versioned ViewContent contract.
 *
 * Unversioned descriptors intentionally retain the legacy permissive behavior. Version 1.0.0
 * descriptors fail with [ViewContentDescriptorException] and a JSON-style descriptor path.
 */
fun validateViewContentDescriptor(viewContent: JSONObject) {
    if (!viewContent.has(keyName = "schemaVersion")) return
    validateVersionedViewContent(viewContent, path = "$")
}

private fun validateVersionedViewContent(viewContent: JSONObject, path: String) {
    if (viewContent.has(keyName = "schemaVersion")) {
        val schemaVersion = viewContent.requireString(keyName = "schemaVersion", parentPath = path)
        if (schemaVersion != "1.0.0") {
            throw ViewContentDescriptorException(
                descriptorPath = viewContent.pathFor(keyName = "schemaVersion", parentPath = path),
                detail = "Unsupported schema version '$schemaVersion'",
            )
        }
    }

    val type = viewContent.requireString(keyName = "type", parentPath = path)
    if (type !in supportedViewTypes) {
        throw ViewContentDescriptorException(
            descriptorPath = viewContent.pathFor(keyName = "type", parentPath = path),
            detail = "Unsupported view type '$type'",
        )
    }

    validateOptionalViewObject(viewContent, keyName = "triggerView", parentPath = path)
    validateOptionalViewObject(viewContent, keyName = "overlayView", parentPath = path)
    validateOptionalViewArray(viewContent, keyName = "views", parentPath = path)
    validateOptionalViewArray(viewContent, keyName = "toolbarViews", parentPath = path)
    validateOptionalViewArray(viewContent, keyName = "content", parentPath = path)
    validateOptionalLabel(viewContent, parentPath = path)
    validateOptionalSections(viewContent, parentPath = path)
    validateOptionalTabs(viewContent, parentPath = path)
    validateOptionalObjectArray(
        viewContent = viewContent,
        keyName = "visibility",
        parentPath = path,
        requiredItemKey = "ruleName",
    )
    validateOptionalObjectArray(
        viewContent = viewContent,
        keyName = "rows",
        parentPath = path,
        requiredItemKey = "itemType",
    )
    validateOptionalObjectArray(
        viewContent = viewContent,
        keyName = "columns",
        parentPath = path,
        requiredItemKey = "itemType",
    )

    when (type) {
        "text" -> viewContent.requireAnyString(
            keyNames = listOf("text", "dynamicText", "previewText"),
            parentPath = path,
        )
        "link" -> {
            viewContent.requireString(keyName = "text", parentPath = path)
            viewContent.requireString(keyName = "url", parentPath = path)
        }
        "image", "video", "webView", "included" -> {
            viewContent.requireString(keyName = "source", parentPath = path)
            if (
                type == "image" &&
                viewContent.optString(keyName = "sourceType", fallback = "local") == "system"
            ) {
                viewContent.requireExactString(keyName = "ios:source", parentPath = path)
                viewContent.requireExactString(keyName = "android:source", parentPath = path)
            }
        }
        "tabbedView" -> viewContent.requireArray(keyName = "tabs", parentPath = path)
        "navigationView" -> viewContent.requireArray(keyName = "views", parentPath = path)
        "navigationLink" -> {
            viewContent.requireObject(keyName = "triggerView", parentPath = path)
            viewContent.requireAnyString(
                keyNames = listOf("route", "customViewName"),
                parentPath = path,
            )
        }
        "list" -> viewContent.requireAny(
            keyNames = listOf("views", "sections"),
            parentPath = path,
        )
        "grid" -> {
            viewContent.requireArray(keyName = "views", parentPath = path)
            viewContent.requireAny(
                keyNames = listOf("numRows", "numColumns", "rows", "columns", "minCellWidth"),
                parentPath = path,
            )
        }
        "hStack", "vStack", "zStack" ->
            viewContent.requireArray(keyName = "views", parentPath = path)
        "disclosureGroup" -> {
            viewContent.requireArray(keyName = "label", parentPath = path)
            viewContent.requireArray(keyName = "content", parentPath = path)
        }
        "menu" -> {
            viewContent.requireString(keyName = "title", parentPath = path)
            viewContent.requireArray(keyName = "views", parentPath = path)
        }
        "custom" -> viewContent.requireString(keyName = "customViewName", parentPath = path)
        "gradientView" -> {
            val colors = viewContent.requireArray(keyName = "colors", parentPath = path)
            if (colors.length() == 0) {
                throw ViewContentDescriptorException(
                    descriptorPath = viewContent.pathFor(keyName = "colors", parentPath = path),
                    detail = "Expected at least one item",
                )
            }
        }
        "progressView" -> viewContent.requireString(keyName = "label", parentPath = path)
    }
}

private fun validateOptionalViewObject(
    viewContent: JSONObject,
    keyName: String,
    parentPath: String,
) {
    if (!viewContent.has(keyName = keyName)) return
    val path = viewContent.pathFor(keyName = keyName, parentPath = parentPath)
    validateVersionedViewContent(
        viewContent = viewContent.requireObject(keyName = keyName, parentPath = parentPath),
        path = path,
    )
}

private fun validateOptionalViewArray(
    viewContent: JSONObject,
    keyName: String,
    parentPath: String,
) {
    if (!viewContent.has(keyName = keyName)) return
    val arrayPath = viewContent.pathFor(keyName = keyName, parentPath = parentPath)
    validateViewArray(
        views = viewContent.requireArray(keyName = keyName, parentPath = parentPath),
        path = arrayPath,
    )
}

private fun validateViewArray(views: JSONArray, path: String) {
    (0 until views.length()).forEach { index ->
        val itemPath = "$path[$index]"
        val item = views.opt(index)
        if (item !is JSONObject) {
            throw ViewContentDescriptorException(itemPath, "Expected an object")
        }
        validateVersionedViewContent(item, path = itemPath)
    }
}

private fun validateOptionalLabel(viewContent: JSONObject, parentPath: String) {
    if (!viewContent.has(keyName = "label")) return
    val labelPath = viewContent.pathFor(keyName = "label", parentPath = parentPath)
    when (val label = viewContent.resolvedValue(keyName = "label")) {
        is String -> Unit
        is JSONArray -> validateViewArray(label, path = labelPath)
        else -> throw ViewContentDescriptorException(labelPath, "Expected a string or array")
    }
}

private fun validateOptionalSections(viewContent: JSONObject, parentPath: String) {
    if (!viewContent.has(keyName = "sections")) return
    val sectionsPath = viewContent.pathFor(keyName = "sections", parentPath = parentPath)
    val sections = viewContent.requireArray(keyName = "sections", parentPath = parentPath)
    (0 until sections.length()).forEach { index ->
        val sectionPath = "$sectionsPath[$index]"
        val section = sections.opt(index)
        if (section !is JSONObject) {
            throw ViewContentDescriptorException(sectionPath, "Expected an object")
        }
        if (section.has(keyName = "title")) {
            section.requireString(keyName = "title", parentPath = sectionPath)
        }
        val viewsPath = section.pathFor(keyName = "views", parentPath = sectionPath)
        validateViewArray(
            views = section.requireArray(keyName = "views", parentPath = sectionPath),
            path = viewsPath,
        )
    }
}

private fun validateOptionalTabs(viewContent: JSONObject, parentPath: String) {
    if (!viewContent.has(keyName = "tabs")) return
    val tabsPath = viewContent.pathFor(keyName = "tabs", parentPath = parentPath)
    val tabs = viewContent.requireArray(keyName = "tabs", parentPath = parentPath)
    (0 until tabs.length()).forEach { index ->
        val tabPath = "$tabsPath[$index]"
        val tab = tabs.opt(index)
        if (tab !is JSONObject) {
            throw ViewContentDescriptorException(tabPath, "Expected an object")
        }
        if (tab.has(keyName = "title")) {
            tab.requireString(keyName = "title", parentPath = tabPath)
        }
        validateOptionalViewObject(tab, keyName = "icon", parentPath = tabPath)
        val contentPath = tab.pathFor(keyName = "tabContent", parentPath = tabPath)
        validateVersionedViewContent(
            viewContent = tab.requireObject(keyName = "tabContent", parentPath = tabPath),
            path = contentPath,
        )
    }
}

private fun validateOptionalObjectArray(
    viewContent: JSONObject,
    keyName: String,
    parentPath: String,
    requiredItemKey: String,
) {
    if (!viewContent.has(keyName = keyName)) return
    val arrayPath = viewContent.pathFor(keyName = keyName, parentPath = parentPath)
    val values = viewContent.requireArray(keyName = keyName, parentPath = parentPath)
    (0 until values.length()).forEach { index ->
        val itemPath = "$arrayPath[$index]"
        val item = values.opt(index)
        if (item !is JSONObject) {
            throw ViewContentDescriptorException(itemPath, "Expected an object")
        }
        item.requireString(keyName = requiredItemKey, parentPath = itemPath)
    }
}

private fun JSONObject.requireAnyString(keyNames: List<String>, parentPath: String) {
    requireAny(keyNames = keyNames, parentPath = parentPath)
    keyNames.filter { has(keyName = it) }.forEach { keyName ->
        requireString(keyName = keyName, parentPath = parentPath)
    }
}

private fun JSONObject.requireAny(keyNames: List<String>, parentPath: String) {
    if (keyNames.none { has(keyName = it) }) {
        throw ViewContentDescriptorException(
            descriptorPath = parentPath,
            detail = "Expected one of ${keyNames.joinToString()}",
        )
    }
}

private fun JSONObject.requireString(keyName: String, parentPath: String): String {
    val path = pathFor(keyName = keyName, parentPath = parentPath)
    val value = resolvedValue(keyName = keyName)
        ?: throw ViewContentDescriptorException(path, "Missing required property")
    return value as? String
        ?: throw ViewContentDescriptorException(path, "Expected a string")
}

private fun JSONObject.requireExactString(keyName: String, parentPath: String): String {
    val path = jsonPropertyPath(parentPath, keyName)
    if (!has(keyName)) {
        throw ViewContentDescriptorException(path, "Missing required property")
    }
    return opt(keyName) as? String
        ?: throw ViewContentDescriptorException(path, "Expected a string")
}

private fun JSONObject.requireArray(keyName: String, parentPath: String): JSONArray {
    val path = pathFor(keyName = keyName, parentPath = parentPath)
    val value = resolvedValue(keyName = keyName)
        ?: throw ViewContentDescriptorException(path, "Missing required property")
    return value as? JSONArray
        ?: throw ViewContentDescriptorException(path, "Expected an array")
}

private fun JSONObject.requireObject(keyName: String, parentPath: String): JSONObject {
    val path = pathFor(keyName = keyName, parentPath = parentPath)
    val value = resolvedValue(keyName = keyName)
        ?: throw ViewContentDescriptorException(path, "Missing required property")
    return value as? JSONObject
        ?: throw ViewContentDescriptorException(path, "Expected an object")
}

private fun JSONObject.resolvedValue(keyName: String): Any? =
    resolvedKey(keyName)?.let(::opt)?.takeUnless { it == JSONObject.NULL }

private fun JSONObject.pathFor(keyName: String, parentPath: String): String =
    jsonPropertyPath(parentPath, resolvedKey(keyName) ?: propertyCandidates(keyName).first())

private fun jsonPropertyPath(parentPath: String, keyName: String): String =
    if (keyName.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
        "$parentPath.$keyName"
    } else {
        "$parentPath['${keyName.replace("'", "\\'")}']"
    }
