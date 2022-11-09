package com.appversation.appstentcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

