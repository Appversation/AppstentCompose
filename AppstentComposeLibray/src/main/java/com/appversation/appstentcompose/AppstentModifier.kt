package com.appversation.appstentcompose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.accessibilityClassName
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.appversation.appstentcompose.ModuleConfigs.CustomContentDataProvider
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.abs

fun Modifier.getModifier(
    modifierContent: JSONObject,
    context: Context,
    customContentDataProvider: CustomContentDataProvider? = null
) : Modifier {
    return this
        .getPaddingModifier(modifierContent)
        .getFixedSizeModifier(modifierContent)
        .getFrameSizeModifier(modifierContent)
        .getFillSizeModifier(modifierContent)
        .getOffsetModifier(modifierContent)
        .getShadowModifier(modifierContent, context, customContentDataProvider)
        .getClipShapeModifier(modifierContent)
        .getCornerRadiusModifier(modifierContent)
        .getBackgroundModifier(modifierContent, context, customContentDataProvider)
        .getBorderModifier(modifierContent, context, customContentDataProvider)
        .getPreviewSelectionInputModifier(modifierContent)
        .getPreviewSelectionModifier(modifierContent)
        .getAccessibilityModifier(modifierContent)
}

internal data class AppstentAccessibilityConfiguration(
    val label: String,
    val hint: String,
    val identifier: String,
    val role: String,
    val traits: List<String>,
    val isEnabled: Boolean,
    val isDecorative: Boolean,
) {
    val spokenDescription: String
        get() = listOf(label, hint).filter { it.isNotBlank() }.joinToString(". ")
}

internal fun resolveAccessibilityConfiguration(
    viewContent: JSONObject,
): AppstentAccessibilityConfiguration {
    val traits = viewContent
        .optPlatformValue("accessibilityTraits")
        .let { it as? org.json.JSONArray }
        ?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf(String::isNotBlank)
            }
        }
        ?: emptyList()

    return AppstentAccessibilityConfiguration(
        label = viewContent.optString(keyName = "accessibilityLabel", fallback = ""),
        hint = viewContent.optString(keyName = "accessibilityHint", fallback = ""),
        identifier = viewContent.optString(keyName = "accessibilityIdentifier", fallback = ""),
        role = viewContent.optString(keyName = "accessibilityRole", fallback = ""),
        traits = traits,
        isEnabled = viewContent.optBoolean(keyName = "isEnabled", fallback = true),
        isDecorative = viewContent.optBoolean(keyName = "isDecorative", fallback = false),
    )
}

fun Modifier.getAccessibilityModifier(viewContent: JSONObject): Modifier {
    val configuration = resolveAccessibilityConfiguration(viewContent)

    if (configuration.isDecorative) {
        return clearAndSetSemantics { }
    }

    val semanticNames = listOf(configuration.role) + configuration.traits
    var result = this

    if (configuration.identifier.isNotBlank()) {
        result = result.testTag(configuration.identifier)
    }

    return result.semantics {
        if (configuration.spokenDescription.isNotBlank()) {
            contentDescription = configuration.spokenDescription
        }
        if (!configuration.isEnabled) {
            disabled()
        }
        if ("header" in semanticNames) {
            heading()
        }
        if ("selected" in semanticNames) {
            selected = true
        }
        if ("link" in semanticNames) {
            // Compose has no Link role. A clickable TextView is Android's native
            // accessibility representation for a standalone text link and avoids
            // announcing authored links as buttons.
            accessibilityClassName = android.widget.TextView::class.java.name
        }

        val resolvedRole = when {
            "button" in semanticNames -> Role.Button
            "image" in semanticNames -> Role.Image
            "toggle" in semanticNames -> Role.Switch
            else -> null
        }
        if (resolvedRole != null) {
            role = resolvedRole
        }
    }
}

fun Modifier.getPreviewSelectionInputModifier(modifierContent: JSONObject) : Modifier {
    val previewPath = modifierContent.optString(keyName = "__appstentPreviewPath", fallback = "")

    if (!ModuleConfigs.inPreviewMode || previewPath.isBlank()) {
        return this
    }

    return this.pointerInput(previewPath) {
        awaitPointerEventScope {
            var downX = 0f
            var downY = 0f
            var isTrackingTap = false
            var didMove = false
            val moveTolerance = 8.dp.toPx()

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val firstChange = event.changes.firstOrNull() ?: continue

                if (firstChange.pressed && !isTrackingTap) {
                    downX = firstChange.position.x
                    downY = firstChange.position.y
                    isTrackingTap = true
                    didMove = false
                    continue
                }

                if (firstChange.pressed && isTrackingTap) {
                    if (abs(firstChange.position.x - downX) > moveTolerance ||
                        abs(firstChange.position.y - downY) > moveTolerance) {
                        didMove = true
                    }
                    continue
                }

                if (!firstChange.pressed && isTrackingTap) {
                    if (!didMove) {
                        ModuleConfigs.reportPreviewSelection(previewPath)
                    }

                    isTrackingTap = false
                    didMove = false
                }
            }
        }
    }
}

fun Modifier.getPreviewSelectionModifier(modifierContent: JSONObject) : Modifier {
    if (!ModuleConfigs.inPreviewMode || !modifierContent.optBoolean(keyName = "isSelected", fallback = false)) {
        return this
    }

    return this.drawWithContent {
        drawContent()

        val cornerRadius = 4.dp.toPx()
        drawRoundRect(
            color = Color(0x142196F3),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
        drawRoundRect(
            color = Color(0xFF2196F3),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

fun Modifier.getClipShapeModifier(modifierContent: JSONObject) : Modifier {

    return try {
        when (modifierContent.getString(keyName = "clipShape")) {
            "circle" -> this.clip(CircleShape)
            else -> this
        }
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getCornerRadiusModifier(modifierContent: JSONObject) : Modifier {

    return try {
        val cornerRadius = modifierContent.appstentResolvedDouble(keyName = "cornerRadius")
        this.clip(RoundedCornerShape(cornerRadius.dp))
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getBackgroundModifier(
    modifierContent: JSONObject,
    context: Context,
    customContentDataProvider: CustomContentDataProvider?
) : Modifier {

    return try {
        val bgColor = modifierContent.getString(keyName = "backgroundColor")
        resolveAppstentColor(bgColor, context, customContentDataProvider)
            ?.let { this.background(it) }
            ?: this
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getBorderModifier(
    modifierContent: JSONObject,
    context: Context,
    customContentDataProvider: CustomContentDataProvider?
) : Modifier {

    return try {
        val borderColorString = modifierContent.optString(keyName = "borderColor", fallback = "")
        val color = resolveAppstentColor(borderColorString, context, customContentDataProvider)

        if (borderColorString.isEmpty() || color == null) {
            this
        } else {
            val borderWidth = modifierContent.optDouble(keyName = "borderWidth", fallback = 1.0)
                .takeUnless { modifierContent.optString(keyName = "borderWidth", fallback = "").startsWith("{") }
                ?: modifierContent.appstentResolvedDouble(keyName = "borderWidth", fallback = 1.0)

            val borderShape = when {
                modifierContent.optString(keyName = "clipShape", fallback = "") == "circle" -> CircleShape
                modifierContent.has(keyName = "cornerRadius") -> {
                    val cornerRadius = modifierContent.appstentResolvedDouble(keyName = "cornerRadius")
                    RoundedCornerShape(cornerRadius.dp)
                }
                else -> RoundedCornerShape(0.dp)
            }

            this.border(width = borderWidth.dp, color = color, shape = borderShape)
        }
    } catch (e: JSONException) {
        this
    } catch (e: IllegalArgumentException) {
        this
    }
}

fun Modifier.getPaddingModifier(modifierContent: JSONObject) : Modifier {

    return try {
        val padding = modifierContent.appstentResolvedDouble(keyName = "padding")
        this.padding(padding.dp)
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getFrameSizeModifier(modifierContent: JSONObject) : Modifier {

    var modifier: Modifier = this

    return try {

        modifier = if (modifierContent.has(keyName = "width")) {

            val width = modifierContent.appstentResolvedDouble(keyName = "width")

            // Keep authored widths within the available viewport or grid cell.
            modifier.width(width.dp)
        } else {
            modifier
        }

        if (modifierContent.has(keyName = "height")) {
            val height = modifierContent.appstentResolvedDouble(keyName = "height")

            modifier = modifier.requiredHeight(height.dp)
        }

        if (!modifierContent.has(keyName = "width") && modifierContent.has(keyName = "percentWidth")) {
            val widthFraction = modifierContent
                .appstentResolvedDouble(keyName = "percentWidth")
                .coerceAtLeast(0.0)
                .toFloat()
            modifier = modifier.fillMaxWidth(widthFraction)
        }

        if (!modifierContent.has(keyName = "height") && modifierContent.has(keyName = "percentHeight")) {
            val heightFraction = modifierContent
                .appstentResolvedDouble(keyName = "percentHeight")
                .coerceAtLeast(0.0)
                .toFloat()
            modifier = modifier.fillMaxHeight(heightFraction)
        }

        val idealWidth = modifierContent.resolvedDimension("idealWidth", "ideal_width")
        val idealHeight = modifierContent.resolvedDimension("idealHeight", "ideal_height")
        modifier = modifier.idealSize(idealWidth, idealHeight)

        val minWidth = modifierContent.resolvedDimension("minWidth", "min_width")?.dp
            ?: Dp.Unspecified

        val minHeight = modifierContent.resolvedDimension("minHeight", "min_height")?.dp
            ?: Dp.Unspecified

        val maxWidth = modifierContent.resolvedDimension("maxWidth", "max_width")?.dp
            ?: Dp.Unspecified

        val maxHeight = modifierContent.resolvedDimension("maxHeight", "max_height")?.dp
            ?: Dp.Unspecified

        modifier = modifier.sizeIn(minWidth = minWidth, minHeight = minHeight, maxWidth = maxWidth, maxHeight = maxHeight)

        modifier
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getFixedSizeModifier(modifierContent: JSONObject): Modifier {
    var modifier = this

    if (modifierContent.optBoolean(keyName = "fixedWidth", fallback = false)) {
        modifier = modifier.width(IntrinsicSize.Max)
    }
    if (modifierContent.optBoolean(keyName = "fixedHeight", fallback = false)) {
        modifier = modifier.height(IntrinsicSize.Max)
    }

    return modifier
}

fun Modifier.getFillSizeModifier(modifierContent: JSONObject) : Modifier {

    var modifier: Modifier = this

    return try {

        modifier = if (modifierContent.has(keyName = "fillMaxWidth")) {

            if (modifierContent.optBoolean(keyName = "fillMaxWidth", fallback = false)) {
                modifier.fillMaxWidth()
            } else {
                modifier
            }
        } else {
            modifier
        }

        modifier = if (modifierContent.has(keyName = "fillMaxHeight")) {

            if (modifierContent.optBoolean(keyName = "fillMaxHeight", fallback = false)) {
                modifier.fillMaxHeight()
            } else {
                modifier
            }
        } else {
            modifier
        }

        modifier = when (modifierContent.optString(keyName = "fillModifier", fallback = "")) {
            "fillMaxWidth" -> modifier.fillMaxWidth()
            "fillMaxHeight" -> modifier.fillMaxHeight()
            "fillMaxSize" -> modifier.fillMaxSize()
            else -> modifier
        }

        modifier
    } catch (e: JSONException) {
        this
    }
}

private fun JSONObject.resolvedDimension(vararg keyNames: String): Double? {
    val keyName = keyNames.firstOrNull { has(it) } ?: return null
    return appstentResolvedDouble(keyName = keyName)
}

private fun Modifier.idealSize(idealWidth: Double?, idealHeight: Double?): Modifier {
    if (idealWidth == null && idealHeight == null) {
        return this
    }

    return layout { measurable, constraints ->
        val width = idealWidth?.dp?.roundToPx()
        val height = idealHeight?.dp?.roundToPx()
        val childConstraints = Constraints(
            minWidth = if (!constraints.hasBoundedWidth && width != null) width else constraints.minWidth,
            maxWidth = if (!constraints.hasBoundedWidth && width != null) width else constraints.maxWidth,
            minHeight = if (!constraints.hasBoundedHeight && height != null) height else constraints.minHeight,
            maxHeight = if (!constraints.hasBoundedHeight && height != null) height else constraints.maxHeight,
        )
        val placeable = measurable.measure(childConstraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
}

fun Modifier.getOffsetModifier(modifierContent: JSONObject) : Modifier {

    return try {

        val offsetX = modifierContent.appstentResolvedDouble(keyName = "offsetX", fallback = 0.0)
        val offsetY = modifierContent.appstentResolvedDouble(keyName = "offsetY", fallback = 0.0)

        return this.offset(x = offsetX.dp, y= offsetY.dp)

    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getShadowModifier(
    modifierContent: JSONObject,
    context: Context,
    customContentDataProvider: CustomContentDataProvider?
) : Modifier {

    var modifier: Modifier = this

    return try {
        modifier = if (modifierContent.has(keyName = "shadow")) {

            val rawShadow = modifierContent.optPlatformValue("shadow")
            val shadowObject = when {
                rawShadow is JSONObject -> rawShadow
                rawShadow is String && AppstentDesignTokenResolver.isTokenReference(rawShadow) ->
                    ModuleConfigs.designTokenResolver.resolveShadow(rawShadow).value
                else -> null
            }

            if (shadowObject != null && shadowObject.has(keyName = "radius") && shadowObject.has(keyName = "spread")) {
                val colorString = shadowObject.optString("color", fallback = "#000000")
                val color = resolveAppstentColor(colorString, context, customContentDataProvider)
                    ?: Color(android.graphics.Color.parseColor("#000000"))
                val radius = shadowObject.appstentResolvedDouble(keyName = "radius")
                val spread = shadowObject.appstentResolvedDouble(keyName ="spread")

                modifier.dropShadow(
                    shape = RoundedCornerShape(radius.dp),
                    shadow = Shadow(
                        radius = radius.dp,
                        color = color,
                        spread = spread.dp
                    )
                )
            } else {
                modifier
            }
        } else {
            modifier
        }

        modifier
    } catch (e: JSONException) {
        this
    }
}

internal fun resolveAppstentColor(
    rawValue: String,
    context: Context,
    customContentDataProvider: CustomContentDataProvider?
): Color? {
    if (rawValue.isEmpty()) {
        return null
    }

    val resolvedValue = if (AppstentDesignTokenResolver.isTokenReference(rawValue)) {
        ModuleConfigs.designTokenResolver.resolveColor(rawValue).value
    } else {
        customContentDataProvider
            ?.getStringFor(rawValue)
            ?.takeIf { it.isNotEmpty() }
            ?: rawValue
    }

    return parseAppstentColor(resolvedValue, context)
}

private fun parseAppstentColor(colorString: String, context: Context): Color? {
    try {
        return Color(android.graphics.Color.parseColor(colorString))
    } catch (_: IllegalArgumentException) {
    }

    val colorResourceName = colorString
        .substringAfterLast('.')
        .substringAfterLast('/')
    val colorResourceId = context.resources.getIdentifier(
        colorResourceName,
        "color",
        context.packageName
    )

    if (colorResourceId != 0) {
        try {
            return Color(context.getColor(colorResourceId))
        } catch (_: Exception) {
        }
    }

    return null
}
