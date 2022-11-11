package com.appversation.appstentcompose

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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

    val minLength = viewContent.getDouble("minLength").toFloat()
    Spacer(modifier = Modifier.defaultMinSize(Dp(minLength), Dp(minLength)))
}

@Composable
fun TextView(viewContent: JSONObject) {
    val textString = viewContent.getString("text")

    var color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black

    if (viewContent.has("foregroundColor")) {
        val fgColor = viewContent.getString("foregroundColor")
        color = Color(android.graphics.Color.parseColor(fgColor))
    }

    Text(textString, color = color, modifier = Modifier.getModifier(viewContent))
}

@Composable
fun ImageView(viewContent: JSONObject) {

    val sourceType = viewContent.getString("sourceType")
    val imageSource = viewContent.getString("source")

    when (sourceType) {
        "remote"    -> AsyncImage(imageSource, null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().getModifier(viewContent)
        )
        "system"    -> Icon(imageSource)
        else        -> Image(painterResource(id = imageSource.toInt()),null, modifier = Modifier.getModifier(viewContent))
    }
}

@Composable
fun Icon(name: String) {

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
        "photo"           -> Icons.Outlined.Place
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
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .fillMaxSize()
            .aspectRatio(1f))
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
                                            .fillMaxHeight()
                                    else appstentModifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()

    val rowModifier: Modifier = if (scrollable)
                                    appstentModifier
                                        .horizontalScroll(rememberScrollState())
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                else appstentModifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()

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
