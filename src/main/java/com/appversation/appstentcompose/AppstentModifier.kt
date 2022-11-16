package com.appversation.appstentcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONException
import org.json.JSONObject

fun Modifier.getModifier(modifierContent: JSONObject) : Modifier {

    return this
        .getBackgroundModifier(modifierContent)
        .getClipShapeModifier(modifierContent)
        .getPaddingModifier(modifierContent)
        .getFrameSizeModifier(modifierContent)

}

fun Modifier.getClipShapeModifier(modifierContent: JSONObject) : Modifier {

    return try {
        val shapeName = modifierContent.getString("clipShape")
        when (shapeName) {
            "circle" -> this.clip(CircleShape)
            else -> this
        }
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getBackgroundModifier(modifierContent: JSONObject) : Modifier {

    return try {
        val bgColor = modifierContent.getString("backgroundColor")
        this.background(Color(android.graphics.Color.parseColor(bgColor)))
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getPaddingModifier(modifierContent: JSONObject) : Modifier {

    return try {
        val padding = modifierContent.getInt("padding")
        this.padding(padding.dp)
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getFrameSizeModifier(modifierContent: JSONObject) : Modifier {

    return try {
        if (modifierContent.has("width") || modifierContent.has("height")) {
            val width = modifierContent.getDouble("width")

            val height = if (modifierContent.has("height"))
                            modifierContent.getDouble("height")
                        else
                            width

            return this.size(width.dp, height.dp)
        } else
            this
    } catch (e: JSONException) {
        this
    }
}