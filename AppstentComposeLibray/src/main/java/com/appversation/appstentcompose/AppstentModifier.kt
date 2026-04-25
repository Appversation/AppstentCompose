package com.appversation.appstentcompose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appversation.appstentcompose.ModuleConfigs.CustomContentDataProvider
import org.json.JSONException
import org.json.JSONObject

fun Modifier.getModifier(
    modifierContent: JSONObject,
    context: Context,
    customContentDataProvider: CustomContentDataProvider? = null
) : Modifier {
    return this
        .getPaddingModifier(modifierContent)
        .getFrameSizeModifier(modifierContent)
        .getOffsetModifier(modifierContent)
        .getShadowModifier(modifierContent, context, customContentDataProvider)
        .getClipShapeModifier(modifierContent)
        .getCornerRadiusModifier(modifierContent)
        .getBackgroundModifier(modifierContent, context, customContentDataProvider)
        .getBorderModifier(modifierContent, context, customContentDataProvider)
        .getFillSizeModifier(modifierContent)
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
        val cornerRadius = modifierContent.getInt(keyName = "cornerRadius")
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

            val borderShape = when {
                modifierContent.optString(keyName = "clipShape", fallback = "") == "circle" -> CircleShape
                modifierContent.has(keyName = "cornerRadius") -> {
                    val cornerRadius = modifierContent.getInt(keyName = "cornerRadius")
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
        val padding = modifierContent.getInt(keyName = "padding")
        this.padding(padding.dp)
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getFrameSizeModifier(modifierContent: JSONObject) : Modifier {

    var modifier: Modifier = this

    return try {

        modifier = if (modifierContent.has(keyName = "width")) {

            val width = modifierContent.getDouble(keyName = "width")

            modifier.requiredWidth(width.dp)
        } else {
            modifier.fillMaxWidth()
        }

        if (modifierContent.has(keyName = "height")) {
            val height = modifierContent.getDouble(keyName = "height")

            modifier = modifier.requiredHeight(height.dp)
        }

        val minWidth = if (modifierContent.has(keyName = "minWidth")) {
            modifierContent.getDouble(keyName = "minWidth").dp
        } else {
            Dp.Unspecified
        }

        val minHeight = if (modifierContent.has(keyName = "minHeight")) {
            modifierContent.getDouble(keyName = "minHeight").dp
        } else {
            Dp.Unspecified
        }

        val maxWidth = if (modifierContent.has(keyName = "maxWidth")) {
            modifierContent.getDouble(keyName = "maxWidth").dp
        } else {
            Dp.Unspecified
        }

        val maxHeight = if (modifierContent.has(keyName = "maxHeight")) {
            modifierContent.getDouble(keyName = "maxHeight").dp
        } else {
            Dp.Unspecified
        }

        modifier = modifier.sizeIn(minWidth = minWidth, minHeight = minHeight, maxWidth = maxWidth, maxHeight = maxHeight)

        modifier
    } catch (e: JSONException) {
        this
    }
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

        modifier
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getOffsetModifier(modifierContent: JSONObject) : Modifier {

    return try {

        val offsetX = modifierContent.optDouble(keyName = "offsetX", 0.0)
        val offsetY = modifierContent.optDouble(keyName = "offsetY", 0.0)

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

            val shadowObject = modifierContent.getJSONObject(keyName = "shadow")
            val colorString = shadowObject.optString("color", fallback = "#000000")
            val color = resolveAppstentColor(colorString, context, customContentDataProvider)
                ?: Color(android.graphics.Color.parseColor("#000000"))

            if (shadowObject.has(keyName = "radius") && shadowObject.has(keyName = "spread")) {

                val radius = shadowObject.getDouble(keyName = "radius")
                val spread = shadowObject.getDouble(keyName ="spread")

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

    val resolvedValue = customContentDataProvider
        ?.getStringFor(rawValue)
        ?.takeIf { it.isNotEmpty() }
        ?: rawValue

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
