package com.appversation.appstentcompose

import org.json.JSONObject
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*


@OptIn(ExperimentalUnitApi::class)
fun getCustomFontStyle(viewContent: JSONObject) : TextStyle {

    val textStyle: TextStyle

    // Font family
    var fontFamily: FontFamily? = null
    if (viewContent.has("font")) {
        val androidFontFamily = viewContent.getString("font")
        fontFamily = when (androidFontFamily) {
            "cursive"   -> FontFamily.Cursive
            "serif"     -> FontFamily.Serif
            "sansSerif" -> FontFamily.SansSerif
            "monospace" -> FontFamily.Monospace
            else        -> ModuleConfigs.customContentDataProvider?.getFontFamilyFrom(androidFontFamily)
        }
    }

    // Font Size
    var fontSize = TextUnit.Unspecified
    if (viewContent.has("fontSize")) {

        val fontSizeVal = viewContent.getDouble("fontSize")
        fontSize = TextUnit(fontSizeVal.toFloat(), TextUnitType.Sp)
    }

    // Font Weight
    var fontWeight: FontWeight? = null
    if (viewContent.has("fontWeight")) {

        val fontWeightVal = viewContent.getString("fontWeight")
        fontWeight = when(fontWeightVal) {
            "ultraLight"    -> FontWeight.ExtraLight
            "thin"          -> FontWeight.Thin
            "light"         -> FontWeight.Light
            "regular"       -> FontWeight.Normal
            "medium"        -> FontWeight.Medium
            "semibold"      -> FontWeight.SemiBold
            "bold"          -> FontWeight.Bold
            "heavy"         -> FontWeight.ExtraBold
            "black"         -> FontWeight.Black
            else            -> null
        }
    }

    // Text Decoration/Emphasis
    var fontStyle: FontStyle? = null
    var textDecoration: TextDecoration? = null
    if (viewContent.has("textEmphasis")) {
        val decorations = viewContent.getJSONArray("textEmphasis")

        (0 until decorations.length()).forEach {
            val decoration = decorations.getString(it)

            when (decoration) {
                "bold"      -> fontWeight = FontWeight.ExtraBold
                "underline" -> textDecoration = TextDecoration.Underline
                "italic"    -> fontStyle = FontStyle.Italic
            }
        }
    }

    textStyle = TextStyle(
        fontWeight = fontWeight,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
    textDecoration = textDecoration)

    return textStyle
}