package com.appversation.appstentcompose

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.appversation.appstentcompose.ui.theme.AppstentTheme
import org.json.JSONObject


@Composable
fun AppstentView(viewContent: JSONObject) {
    AppstentTheme {
        if (viewContent.has("type")) {
            when (viewContent.getString("type")) {
                "spacer"    -> SpacerView(viewContent)
                "text"      -> TextView(viewContent)
                "image"     -> ImageView(viewContent)
                "hStack"    -> StackView(viewContent = viewContent, direction = Direction.x)
                "vStack"    -> StackView(viewContent = viewContent, direction = Direction.y)
                "zStack"    -> StackView(viewContent = viewContent, direction = Direction.z)

                else -> { }
            }
        }
    }
}

@Composable
fun SpacerView(viewContent: JSONObject) {

    if (viewContent.has("minLength")) {
        val minLength = viewContent.getDouble("minLength").toFloat()
        Spacer(modifier = Modifier.defaultMinSize(Dp(minLength), Dp(minLength)))
    } else {
        Spacer(modifier = Modifier)
    }
}

@Composable
fun TextView(viewContent: JSONObject) {
    var textString = ""

    if (viewContent.has("text")) {
        textString = viewContent.getString("text")
    }

    if (viewContent.has("dynamicText")) {
        val dynamicTextFieldName = viewContent.getString("dynamicText")
        textString = ModuleConfigs.customContentDataProvider?.getStringFor(dynamicTextFieldName) ?: textString
    }

    //foreground color
    var color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black
    if (viewContent.has("foregroundColor")) {
        val fgColor = viewContent.getString("foregroundColor")
        color = Color(android.graphics.Color.parseColor(fgColor))
    }

    var textStyle = TextStyle.Default
    if (viewContent.has("font")) {
        val fontString = viewContent.getString("font")
        textStyle = when (fontString) {
            "largeTitle" -> androidx.compose.material.MaterialTheme.typography.h3
            "title" -> androidx.compose.material.MaterialTheme.typography.h4
            "title2" -> androidx.compose.material.MaterialTheme.typography.h5
            "title3" -> androidx.compose.material.MaterialTheme.typography.h6
            "headline" -> androidx.compose.material.MaterialTheme.typography.subtitle1
            "subheadline" -> androidx.compose.material.MaterialTheme.typography.subtitle2
            "body" -> androidx.compose.material.MaterialTheme.typography.body1
            "callout" -> androidx.compose.material.MaterialTheme.typography.body2
            "footnote" -> androidx.compose.material.MaterialTheme.typography.overline
            "caption" -> androidx.compose.material.MaterialTheme.typography.caption
            else -> {

                getCustomFontStyle(viewContent)
            }
        }
    }

    Text(textString,
        color = color,
        style = textStyle,
        modifier = Modifier.getModifier(viewContent))
}

@Composable
fun ImageView(viewContent: JSONObject) {

    val sourceType = viewContent.getString("sourceType")
    val imageSource = viewContent.getString("source")

    when (sourceType) {
        "remote"    -> AsyncImage(imageSource, null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.getModifier(viewContent).fillMaxWidth()
        )
        "system"    -> Icon(imageSource, viewContent)
        else        -> Image(painterResource(id = imageSource.toInt()),null, modifier = Modifier.getModifier(viewContent))
    }
}

@Composable
fun Icon(name: String, viewContent: JSONObject) {

    val icon = when(name) {
        "house"     -> Icons.Outlined.Home
        "plus"      -> Icons.Outlined.Add
        "exclamationmark.triangle"     -> Icons.Outlined.Warning
        "mail"      -> Icons.Outlined.MailOutline
        "phone"     -> Icons.Outlined.Call
        "person.crop.square"      -> Icons.Outlined.AccountBox
        "person.circle"     -> Icons.Outlined.AccountCircle
        "plus.circle"      -> Icons.Outlined.AddCircle
        "arrow.backward"     -> Icons.Outlined.ArrowBack
        "arrow.forward"      -> Icons.Outlined.ArrowForward
        "arrow.down"     -> Icons.Outlined.ArrowDropDown
        "checkmark"      -> Icons.Outlined.Check
        "checkmark.circle"     -> Icons.Outlined.CheckCircle
        "clear"      -> Icons.Outlined.Clear
        "xmark"     -> Icons.Outlined.Close
        "calendar"      -> Icons.Outlined.DateRange
        "delete.left"     -> Icons.Outlined.Delete
        "pencil"      -> Icons.Outlined.Edit
        "face.smiling"     -> Icons.Outlined.Face
        "heart"      -> Icons.Outlined.Favorite
        "heart.square"     -> Icons.Outlined.FavoriteBorder
        "info"      -> Icons.Outlined.Info
        "keyboard.chevron.compact.down"      -> Icons.Outlined.KeyboardArrowDown
        "keyboard.chevron.compact.left"      -> Icons.Outlined.KeyboardArrowLeft
        "keyboard.chevron.compact.right"     -> Icons.Outlined.KeyboardArrowRight
        "list.bullet"     -> Icons.Outlined.List
        "location"        -> Icons.Outlined.LocationOn
        "lock"            -> Icons.Outlined.Lock
        "play"            -> Icons.Outlined.PlayArrow
        "place"           -> Icons.Outlined.Place
        "arrow.clockwise" -> Icons.Outlined.Refresh
        "magnifyingglass" -> Icons.Outlined.Search
        "gear"            -> Icons.Outlined.Settings
        "square.and.arrow.up" -> Icons.Outlined.Share
        "cart"            -> Icons.Outlined.ShoppingCart
        "star"            -> Icons.Outlined.Star
        "hand.thumbsup"   -> Icons.Outlined.ThumbUp
        else        ->  Icons.Outlined.Warning
    }

    return Icon(imageVector = icon, "",
        modifier = Modifier.getModifier(viewContent).fillMaxWidth())
}


enum class Direction {
    x, y, z
}

@Composable
fun StackView(viewContent: JSONObject, direction: Direction) {

    val views = viewContent.getJSONArray("views")
    val scrollable = viewContent.optBoolean("scrollable", false)

    val appstentModifier = Modifier.getModifier(viewContent)

    val columnModifier: Modifier = if (scrollable)
                                        appstentModifier
                                            .verticalScroll(rememberScrollState())
                                            .fillMaxWidth()
                                    else appstentModifier
                                            .fillMaxWidth()

    val rowModifier: Modifier = if (scrollable)
                                    appstentModifier
                                        .horizontalScroll(rememberScrollState())
                                        .fillMaxWidth()
                                else appstentModifier
                                        .fillMaxWidth()

    when (direction) {
        Direction.x -> Row(
            modifier = rowModifier) {
            (0 until views.length()).forEach {
                AppstentView(viewContent = views.getJSONObject(it))
            }
        }

        Direction.y -> {

            Column(
                modifier = columnModifier
            ) {
                (0 until views.length()).forEach {
                    AppstentView(viewContent = views.getJSONObject(it))
                }
            }
        }

        Direction.z -> Box(modifier = appstentModifier) {
            (0 until views.length()).forEach {
                AppstentView(viewContent = views.getJSONObject(it))
            }
        }
    }
}
