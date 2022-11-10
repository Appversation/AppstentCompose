package com.appversation.appstentcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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

    Text(textString)
}

@Composable
fun ImageView(viewContent: JSONObject) {

    val sourceType = viewContent.getString("sourceType")
    val imageSource = viewContent.getString("source")

    when (sourceType) {
        "remote"    -> AsyncImage(imageSource, null)
        else        -> Image(painterResource(id = imageSource.toInt()),null)
    }
}

enum class Direction {
    x, y, z
}

@Composable
fun StackView(viewContent: JSONObject, direction: Direction) {

    val views = viewContent.getJSONArray("views")
    val scrollable = viewContent.optBoolean("scrollable", false)

    val columnModifier: Modifier = if (scrollable)
                                        Modifier
                                            .verticalScroll(rememberScrollState())
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                    else Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()

    val rowModifier: Modifier = if (scrollable)
                                    Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                else Modifier
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

        Direction.z -> Box {
            (0 until views.length()).forEach {
                AppstentView(viewContent = views.getJSONObject(it))
            }
        }
    }
}
