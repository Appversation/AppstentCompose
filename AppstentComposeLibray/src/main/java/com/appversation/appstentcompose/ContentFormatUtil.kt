package com.appversation.appstentcompose

import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Alignment.Companion.TopStart
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
    if (viewContent.has(keyName = "font")) {
        val androidFontFamily = viewContent.getString(keyName = "font")
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
    if (viewContent.has(keyName = "fontSize")) {

        val fontSizeVal = viewContent.getDouble(keyName = "fontSize")
        fontSize = TextUnit(fontSizeVal.toFloat(), TextUnitType.Sp)
    }

    // Font Weight
    var fontWeight: FontWeight? = null
    if (viewContent.has(keyName = "fontWeight")) {

        val fontWeightVal = viewContent.getString(keyName = "fontWeight")
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
    if (viewContent.has(keyName = "textEmphasis")) {
        val decorations = viewContent.getJSONArray("textEmphasis")

        (0 until decorations.length()).forEach {

            when (decorations.getString(it)) {
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

fun getAlignment(alignmentString: String) : Alignment {

    return when (alignmentString) {
        "topLeading"    -> TopStart
        "top"           -> TopCenter
        "topTrailing"   -> TopEnd
        "center"        -> Center
        "bottomLeading" -> BottomStart
        "bottom"        -> BottomCenter
        "bottomTrailing"-> BottomEnd
        else -> Alignment.Center
    }
}

fun getVerticalAlignment(alignmentString: String) : Alignment.Vertical {

    return when (alignmentString) {
        "top"           -> Top
        "center"        -> CenterVertically
        "bottom"        -> Bottom
        else -> CenterVertically
    }
}

fun getHorizontalAlignment(alignmentString: String) : Alignment.Horizontal {

    return when (alignmentString) {
        "leading"       -> Start
        "center"        -> CenterHorizontally
        "trailing"      -> End
        else -> CenterHorizontally
    }
}