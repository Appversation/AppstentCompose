package com.appversation.appstentcompose

import android.support.v4.os.IResultReceiver._Parcel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONException
import org.json.JSONObject

fun Modifier.getModifier(modifierContent: JSONObject) : Modifier {

    return this
        .getPaddingModifier(modifierContent)
        .getFrameSizeModifier(modifierContent)
        .getOffsetModifier(modifierContent)
        .getClipShapeModifier(modifierContent)
        .getCornerRadiusModifier(modifierContent)
        .getBackgroundModifier(modifierContent)
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
        val padding = modifierContent.getInt(keyName = "padding")
        this.padding(padding.dp)
    } catch (e: JSONException) {
        this
    }
}

fun Modifier.getFrameSizeModifier(modifierContent: JSONObject) : Modifier {

    return try {
        if (modifierContent.has(keyName = "width") || modifierContent.has(keyName = "height")) {
            val width = modifierContent.getDouble(keyName = "width")

            val height = if (modifierContent.has(keyName = "height"))
                            modifierContent.getDouble(keyName = "height")
                        else
                            width

            return this.size(width.dp, height.dp)
        } else
            this
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