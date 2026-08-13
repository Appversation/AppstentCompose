package com.appversation.appstentcompose

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.Button as LegacyButton
import androidx.compose.material.CircularProgressIndicator as LegacyCircularProgressIndicator
import androidx.compose.material.Icon as LegacyIcon
import androidx.compose.material.LinearProgressIndicator as LegacyLinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar as LegacyTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider as Material3Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.appversation.appstentcompose.ModuleConfigs.ActionHandlerProvider
import com.appversation.appstentcompose.ModuleConfigs.CustomContentDataProvider
import com.appversation.appstentcompose.ui.theme.AppstentTheme
import com.appversation.appstentcompose.ui.theme.LegacyTypography
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as MarkdownTextNode
import org.commonmark.parser.Parser
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min


private val LocalVerticallyScrollableAppstentParent = compositionLocalOf { false }
private val LocalAncestorAppstentActions = compositionLocalOf<List<AppstentActionDescriptor>> {
    emptyList()
}

private data class AppstentActionDescriptor(
    val name: String,
    val data: JSONObject?,
)

internal fun shouldApplyVerticalScroll(
    viewContent: JSONObject,
    isInsideVerticalScroller: Boolean,
): Boolean {
    if (!viewContent.optBoolean(keyName = "scrollable", fallback = false)) return false

    // Compose cannot measure an unbounded vertical scroller inside another vertical scroller.
    // Keep independently bounded child viewports scrollable, but let an unframed child expand
    // intrinsically and rely on the nearest ancestor viewport for scrolling.
    return !isInsideVerticalScroller || viewContent.has(keyName = "height")
}

@Composable
fun AppstentView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null,
    handlesTapActions: Boolean = true,
) {
    val descriptorFingerprint = viewContent.toString()
    remember(descriptorFingerprint) {
        validateViewContentDescriptor(viewContent)
    }

    AppstentTheme {
        if (viewContent.has(keyName = "type") && isVisible(viewContent, customContentDataProvider)) {
            val ancestorActions = LocalAncestorAppstentActions.current
            val ownAction = if (
                handlesTapActions &&
                viewContent.optBoolean(keyName = "isEnabled", fallback = true)
            ) {
                viewContent.appstentActionDescriptor()
            } else {
                null
            }
            val type = viewContent.optString(keyName = "type", fallback = "")
            val actionHandledByNativeControl = type == "link" || type == "navigationLink"
            val interactiveModifier = if (ownAction != null && !actionHandledByNativeControl) {
                modifier.appstentClickableAction(
                    actions = listOf(ownAction) + ancestorActions,
                    actionHandlerProvider = actionHandlerProvider,
                )
            } else {
                modifier
            }
            val descendantActions = ownAction?.let { listOf(it) + ancestorActions } ?: ancestorActions
            val overlayView = if (viewContent.has(keyName = "overlayView")) {
                viewContent.getJSONObject(keyName = "overlayView")
            } else {
                null
            }

            CompositionLocalProvider(LocalAncestorAppstentActions provides descendantActions) {
                if (overlayView == null) {
                    AppstentViewContent(viewContent, interactiveModifier, navController, customContentDataProvider, actionHandlerProvider)
                } else {
                    val overlayAlignment = getAlignment(viewContent.optString(keyName = "alignment", fallback = "center"))
                    Box(modifier = interactiveModifier) {
                        AppstentViewContent(viewContent, Modifier, navController, customContentDataProvider, actionHandlerProvider)
                        Box(
                            modifier = Modifier.matchParentSize(),
                            contentAlignment = overlayAlignment,
                        ) {
                            AppstentView(
                                viewContent = overlayView,
                                navController = navController,
                                customContentDataProvider = customContentDataProvider,
                                actionHandlerProvider = actionHandlerProvider,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppstentViewContent(
    viewContent: JSONObject,
    modifier: Modifier,
    navController: NavHostController?,
    customContentDataProvider: CustomContentDataProvider?,
    actionHandlerProvider: ActionHandlerProvider?,
) {
    when (viewContent.getString(keyName = "type")) {
        "spacer" -> SpacerView(viewContent, modifier)
        "divider" -> DividerView(viewContent, modifier, customContentDataProvider)
        "gradientView" -> GradientView(viewContent, modifier, customContentDataProvider)
        "text" -> TextView(viewContent, modifier, customContentDataProvider)
        "link" -> LinkView(viewContent, modifier, customContentDataProvider, actionHandlerProvider)
        "image" -> ImageView(viewContent, modifier, customContentDataProvider)
        "video" -> VideoView(viewContent, modifier, customContentDataProvider)
        "webView" -> WebView(
            viewContent = viewContent,
            modifier = modifier,
            customContentDataProvider = customContentDataProvider,
        )
        "tabbedView" -> {
            val tabStyle = viewContent.optString(keyName = "tabStyle", fallback = "")
            if (tabStyle == "pageStyle") {
                PagerView(viewContent, modifier, navController, customContentDataProvider, actionHandlerProvider)
            } else {
                BottomBar(
                    viewContent,
                    modifier,
                    customContentDataProvider = customContentDataProvider,
                    actionHandlerProvider = actionHandlerProvider,
                )
            }
        }
        "hStack" -> StackView(viewContent, Direction.x, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "vStack" -> StackView(viewContent, Direction.y, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "zStack" -> StackView(viewContent, Direction.z, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "included" -> IncludedView(viewContent, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "grid" -> GridView(viewContent, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "list" -> ListView(viewContent, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "disclosureGroup" -> DisclosureGroupView(viewContent, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "menu" -> MenuView(viewContent, modifier, navController, customContentDataProvider, actionHandlerProvider)
        "progressView" -> ProgressView(viewContent, modifier, customContentDataProvider)
        "custom" -> {
            val context = LocalContext.current
            ModuleConfigs.customContentViewProvider?.CustomComposable(
                ModuleConfigs.CustomContentViewContext(
                    viewContent = viewContent,
                    viewName = viewContent.getString(keyName = "customViewName"),
                    modifier = modifier.getModifier(viewContent, context, customContentDataProvider),
                    customContentDataProvider = customContentDataProvider,
                    actionHandlerProvider = actionHandlerProvider,
                ),
            )
        }
        "navigationView" -> NavigationApstentView(viewContent, modifier, customContentDataProvider, actionHandlerProvider)
        "navigationLink" -> NavigationApstentLink(viewContent, modifier, navController, customContentDataProvider, actionHandlerProvider)
    }
}

private fun JSONObject.appstentActionDescriptor(): AppstentActionDescriptor? {
    val name = optString(keyName = "tapHandlerName", fallback = "")
    if (name.isBlank()) return null

    return AppstentActionDescriptor(
        name = name,
        data = if (has(keyName = "tapHandlerData")) {
            getJSONObject(keyName = "tapHandlerData")
        } else {
            null
        },
    )
}

private fun Modifier.appstentClickableAction(
    actions: List<AppstentActionDescriptor>,
    actionHandlerProvider: ActionHandlerProvider?
): Modifier {
    if (actions.isEmpty()) {
        return this
    }

    return this.clickable {
        dispatchAppstentActions(actions, actionHandlerProvider)
    }
}

private fun dispatchAppstentActions(
    actions: List<AppstentActionDescriptor>,
    actionHandlerProvider: ActionHandlerProvider?,
) {
    actions.forEach { action ->
        dispatchAppstentAction(
            handlerName = action.name,
            data = action.data,
            localHandler = actionHandlerProvider,
            globalHandler = ModuleConfigs.globalActionHandlerProvider,
        )
    }
}

internal fun dispatchAppstentAction(
    handlerName: String,
    data: JSONObject?,
    localHandler: ActionHandlerProvider?,
    globalHandler: ActionHandlerProvider?,
) {
    if (handlerName.isBlank()) {
        return
    }

    localHandler?.handleTap(handlerName, data)
    globalHandler?.handleTap(handlerName, data)
}

private fun isVisible(viewContent: JSONObject, customContentDataProvider: CustomContentDataProvider? = null) : Boolean {

    var isVisible = true

    //Field level visibility overrides
    val fieldName = viewContent.optString(keyName = "fieldVisibility", fallback = "")
    if (fieldName.isNotEmpty()) {
        isVisible = isVisible && customContentDataProvider?.getVisibility(fieldName) ?: true
    }

    if (!viewContent.has(keyName = "visibility")) {
        return isVisible
    }

    val visibilityRules = viewContent.getJSONArray(keyName = "visibility")

    (0 until visibilityRules.length()).forEach {
        val visibilityRule = visibilityRules.getJSONObject(it)

        val ruleName = visibilityRule.getString(keyName = "ruleName")
        val ruleValue = visibilityRule.optString(keyName = "ruleValue", "")

        isVisible = isVisible && ModuleConfigs.customContentViewProvider?.visibility(ruleName, ruleValue) ?: true

        when (ruleName) {
            "daily" -> {

                val startTimeOfDayString = visibilityRule.getString(keyName = "starts")
                val endTimeOfDayString = visibilityRule.getString(keyName = "end")

                val dateFormatter = SimpleDateFormat("HH:mm", Locale.US)

                val calendar = Calendar.getInstance()

                if (startTimeOfDayString.isNotEmpty() && endTimeOfDayString.isNotEmpty()) {
                    val scheduledStart = dateFormatter.parse(startTimeOfDayString)
                    val scheduledEnd = dateFormatter.parse(endTimeOfDayString)

                    val now = Date()

                    val scheduledStartTimeComponents = calendar.apply {
                        if (scheduledStart != null) {
                            time = scheduledStart
                        }
                    }.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
                    val currentTimeComponents = calendar.apply { time = now }.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
                    val scheduledEndTimeComponents = calendar.apply {
                        if (scheduledEnd != null) {
                            time = scheduledEnd
                        }
                    }.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)

                    val scheduledStartTimeInMinutes = (scheduledStartTimeComponents.first) * 60 + (scheduledStartTimeComponents.second)
                    val currentTimeComponentsInMinutes = (currentTimeComponents.first) * 60 + (currentTimeComponents.second)
                    val scheduledEndTimeInMinutes = (scheduledEndTimeComponents.first) * 60 + (scheduledEndTimeComponents.second)

                    isVisible = currentTimeComponentsInMinutes in scheduledStartTimeInMinutes..scheduledEndTimeInMinutes
                }
            }

            "schedule" -> {
                val scheduleStartString = visibilityRule.getString(keyName = "starts")
                val scheduleDuration = visibilityRule.getDouble(keyName = "duration")

                val dateFormatter = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US)

                if (scheduleStartString.isNotEmpty()) {
                    val scheduledStart = dateFormatter.parse(scheduleStartString)

                    val now = Date()

                    val visibility = now >= scheduledStart

                    if (scheduledStart?.time != null) {
                        val scheduledEnd =
                            Date(scheduledStart.time + (scheduleDuration * 60 * 1000).toLong())

                        isVisible = visibility && now < scheduledEnd
                    }
                }
            }
        }
    }



    return isVisible
}

@Composable
fun SpacerView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    var spacerModifier = modifier

    if (viewContent.has(keyName = "minLength")) {
        val minLength = viewContent.appstentResolvedDouble(keyName = "minLength").toFloat()
        spacerModifier = modifier.defaultMinSize(Dp(minLength), Dp(minLength))
    }

    if (viewContent.has(keyName = "fillModifier")) {

        when (viewContent.getString(keyName = "fillModifier")) {
            "fillMaxWidth" -> spacerModifier = modifier.fillMaxWidth()
            "fillMaxHeight" -> spacerModifier = modifier.fillMaxHeight()
            "fillMaxSize" -> spacerModifier = modifier.fillMaxSize()
        }
    }

    Spacer(modifier = spacerModifier)
}

@Composable
fun DividerView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null
) {
    val context = LocalContext.current

    PixelCompatibleDivider(
        modifier = modifier.getModifier(viewContent, context, customContentDataProvider)
    )
}

@Suppress("DEPRECATION")
@Composable
private fun PixelCompatibleDivider(modifier: Modifier = Modifier) {
    // HorizontalDivider has different default pixels in Material 3 1.4.0; keep the renamed
    // primitive until that observable renderer change is versioned intentionally.
    Material3Divider(modifier = modifier)
}

@Composable
fun GradientView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null
) {
    val context = LocalContext.current

    val colorJSONArray = viewContent.getJSONArray(keyName = "colors")
    val colors = arrayListOf<Color>()
    (0 until colorJSONArray.length()).forEach {
        val colorString = colorJSONArray.getString(it)
        colors.add(
            it,
            resolveAppstentColor(colorString, context, customContentDataProvider) ?: Color.Transparent
        )
    }

    var gradientModifier = modifier.getModifier(viewContent, context, customContentDataProvider)

    if (!viewContent.has(keyName = "width") && !viewContent.has(keyName = "fillMaxWidth")) {
        gradientModifier = gradientModifier.fillMaxWidth()
    }

    if (!viewContent.has(keyName = "height") && !viewContent.has(keyName = "fillMaxHeight")) {
        gradientModifier = gradientModifier.fillMaxHeight()
    }

    val startPoint = viewContent.optString(keyName = "startPoint", fallback = "top")
    val endPoint = viewContent.optString(keyName = "endPoint", fallback = "bottom")

    Spacer(
        modifier = gradientModifier
            .drawWithCache {
                val brush = Brush.linearGradient(
                    colors = colors,
                    start = gradientOffset(startPoint, size),
                    end = gradientOffset(endPoint, size),
                )
                onDrawBehind { drawRect(brush) }
            }
    )
}

private fun gradientOffset(unitPoint: String, size: Size): Offset = when (unitPoint) {
    "topLeading" -> Offset.Zero
    "top" -> Offset(size.width / 2f, 0f)
    "topTrailing" -> Offset(size.width, 0f)
    "leading" -> Offset(0f, size.height / 2f)
    "center" -> Offset(size.width / 2f, size.height / 2f)
    "trailing" -> Offset(size.width, size.height / 2f)
    "bottomLeading" -> Offset(0f, size.height)
    "bottom" -> Offset(size.width / 2f, size.height)
    "bottomTrailing" -> Offset(size.width, size.height)
    "zero" -> Offset.Zero
    else -> Offset.Zero
}

@Composable
fun TextView(viewContent: JSONObject, modifier: Modifier = Modifier, customContentDataProvider: CustomContentDataProvider? = null) {
    val context = LocalContext.current
    var textString = ""

    if (viewContent.has(keyName = "text")) {
        textString = viewContent.getString(keyName = "text")
    }

    if (viewContent.has(keyName = "previewText")) {
        textString = viewContent.getString(keyName = "previewText")
    }

    if (viewContent.has(keyName = "dynamicText")) {
        val dynamicTextFieldName = viewContent.getString(keyName = "dynamicText")
        textString = customContentDataProvider?.getStringFor(dynamicTextFieldName) ?: textString
    }

    //foreground color
    var color: Color = Color.Black
    if (viewContent.has(keyName = "foregroundColor")) {
        val fgColor = viewContent.getString(keyName = "foregroundColor")
        color = resolveAppstentColor(fgColor, context, customContentDataProvider) ?: Color.Black
    }

    var textStyle = TextStyle.Default
    if (viewContent.has(keyName = "font")) {
        val fontString = viewContent.appstentResolvedString(keyName = "font")
        textStyle = when (fontString) {
            "largeTitle" -> LegacyTypography.h3
            "title" -> LegacyTypography.h4
            "title2" -> LegacyTypography.h5
            "title3" -> LegacyTypography.h6
            "headline" -> LegacyTypography.subtitle1
            "subheadline" -> LegacyTypography.subtitle2
            "body" -> LegacyTypography.body1
            "callout" -> LegacyTypography.body2
            "footnote" -> LegacyTypography.overline
            "caption" -> LegacyTypography.caption
            else -> {

                getCustomFontStyle(viewContent)
            }
        }
    } else {
        //apply text styles for cases when font field is not specified
        textStyle = getCustomFontStyle(viewContent)
    }

    //Text alignment
    val textAlignment = if (viewContent.has(keyName = "textAlignment")) {
        getTextAlignment(viewContent.optString(keyName = "textAlignment", "leading"))
    } else {
        TextAlign.Start
    }


    val lineLimit = if (viewContent.has(keyName = "lineLimit")) {
        val limit = viewContent.optInt(keyName = "lineLimit", Int.MAX_VALUE)
        if (limit > 0) limit else Int.MAX_VALUE
    } else {
        Int.MAX_VALUE
    }

    val textOverflow = if (lineLimit == Int.MAX_VALUE) {
        TextOverflow.Clip
    } else {
        TextOverflow.Ellipsis
    }

    val renderedText = if (viewContent.optBoolean(keyName = "isMarkdown", fallback = false)) {
        rememberMarkdownText(
            markdown = textString,
            textStyle = textStyle,
            color = color
        )
    } else {
        AnnotatedString(textString)
    }
    val styledModifier = modifier.getModifier(viewContent, context, customContentDataProvider)
    val textContent: @Composable (Modifier) -> Unit = { textModifier ->
        Text(
            text = renderedText,
            color = color,
            style = textStyle,
            modifier = textModifier,
            maxLines = lineLimit,
            overflow = textOverflow,
            textAlign = textAlignment
        )
    }

    if (viewContent.fillsVerticalSpace()) {
        Box(
            modifier = styledModifier,
            contentAlignment = getAlignment(
                viewContent.optString(keyName = "alignment", fallback = "center")
            ),
        ) {
            textContent(Modifier)
        }
    } else {
        textContent(styledModifier)
    }
}

internal fun JSONObject.fillsVerticalSpace(): Boolean =
    optBoolean(keyName = "fillMaxHeight", fallback = false) ||
        optString(keyName = "fillModifier", fallback = "") in setOf("fillMaxHeight", "fillMaxSize")

@Composable
private fun rememberMarkdownText(
    markdown: String,
    textStyle: TextStyle,
    color: Color
): AnnotatedString {

    val parser = remember { Parser.builder().build() }
    val baseSpanStyle = remember(textStyle, color) {
        // Mirror the incoming TextStyle into a SpanStyle so inline markdown spans inherit it.
        SpanStyle(
            color = if (textStyle.color != Color.Unspecified) textStyle.color else color,
            fontWeight = textStyle.fontWeight,
            fontStyle = textStyle.fontStyle,
            fontFamily = textStyle.fontFamily,
            fontSize = textStyle.fontSize,
            letterSpacing = textStyle.letterSpacing,
            textDecoration = textStyle.textDecoration,
            background = textStyle.background,
            fontSynthesis = textStyle.fontSynthesis
        )
    }

    return remember(markdown, baseSpanStyle, parser) {
        val document = parser.parse(markdown)
        buildAnnotatedString {
            appendMarkdownNode(document, baseSpanStyle)
        }
    }
}

private fun AnnotatedString.Builder.appendMarkdownNode(
    node: Node?,
    baseSpanStyle: SpanStyle
) {

    var current = node

    while (current != null) {
        when (current) {
            is Document -> {
                appendMarkdownNode(current.firstChild, baseSpanStyle)
            }
            is Paragraph -> {
                appendMarkdownNode(current.firstChild, baseSpanStyle)
                if (current.next != null) {
                    append("\n\n")
                }
            }
            is MarkdownTextNode -> append(current.literal)
            is SoftLineBreak, is HardLineBreak -> append("\n")
            is Emphasis -> {
                val italicStyle = baseSpanStyle.merge(SpanStyle(fontStyle = FontStyle.Italic))
                withStyle(italicStyle) {
                    appendMarkdownNode(current.firstChild, italicStyle)
                }
            }
            is StrongEmphasis -> {
                val boldStyle = baseSpanStyle.merge(SpanStyle(fontWeight = FontWeight.Bold))
                withStyle(boldStyle) {
                    appendMarkdownNode(current.firstChild, boldStyle)
                }
            }
            is Code -> {
                val codeStyle = baseSpanStyle.merge(SpanStyle(fontFamily = FontFamily.Monospace))
                withStyle(codeStyle) {
                    append(current.literal)
                }
            }
            is Link -> {
                val start = length
                appendMarkdownNode(current.firstChild, baseSpanStyle)
                val end = length

                current.destination?.let { destination ->
                    addStringAnnotation("URL", destination, start, end)
                }
                addStyle(
                    style = baseSpanStyle.merge(SpanStyle(textDecoration = TextDecoration.Underline)),
                    start = start,
                    end = end
                )
            }
            is Heading -> {
                appendMarkdownNode(current.firstChild, baseSpanStyle)
                if (current.next != null) {
                    append("\n\n")
                }
            }
            is BulletList -> {
                var item = current.firstChild as? ListItem
                while (item != null) {
                    append("• ")
                    appendMarkdownNode(item.firstChild, baseSpanStyle)
                    item = item.next as? ListItem
                    if (item != null) {
                        append("\n")
                    }
                }
                if (current.next != null) {
                    append("\n\n")
                }
            }
            is OrderedList -> {
                var item = current.firstChild as? ListItem
                var index = current.startNumber
                while (item != null) {
                    append("${index}. ")
                    appendMarkdownNode(item.firstChild, baseSpanStyle)
                    item = item.next as? ListItem
                    index += 1
                    if (item != null) {
                        append("\n")
                    }
                }
                if (current.next != null) {
                    append("\n\n")
                }
            }
            is ListItem -> appendMarkdownNode(current.firstChild, baseSpanStyle)
            else -> appendMarkdownNode(current.firstChild, baseSpanStyle)
        }
        current = current.next
    }
}

@Composable
fun LinkView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null,
) {
    val uriHandler = LocalUriHandler.current
    val url = viewContent.optString(keyName = "url", fallback = "")
    val actions = LocalAncestorAppstentActions.current

    TextView(
        viewContent = viewContent,
        modifier = if (url.isNotEmpty() || actions.isNotEmpty()) {
            modifier.clickable(
                enabled = viewContent.optBoolean(keyName = "isEnabled", fallback = true),
            ) {
                dispatchAppstentActions(actions, actionHandlerProvider)
                if (url.isNotEmpty()) {
                    uriHandler.openUri(url)
                }
            }
        } else {
            modifier
        },
        customContentDataProvider = customContentDataProvider
    )
}

@Composable
fun DisclosureGroupView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current
    val ancestorActions = LocalAncestorAppstentActions.current
    val labelViews = viewContent.getJSONArray(keyName = "label")
    var expanded by remember(viewContent) {
        mutableStateOf(
            if (viewContent.has(keyName = "isExpanded")) {
                viewContent.optBoolean(keyName = "isExpanded", fallback = false)
            } else {
                false
            }
        )
    }

    Column(modifier = modifier.getModifier(viewContent, context, customContentDataProvider)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val labelActions = (0 until labelViews.length()).mapNotNull { index ->
                        labelViews.getJSONObject(index).appstentActionDescriptor()
                    }
                    dispatchAppstentActions(labelActions + ancestorActions, actionHandlerProvider)
                    expanded = !expanded
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                RenderViewContentArray(
                    views = labelViews,
                    navController = navController,
                    customContentDataProvider = customContentDataProvider,
                    actionHandlerProvider = actionHandlerProvider,
                    handlesTapActions = false,
                )
            }

            LegacyIcon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null
            )
        }

        if (expanded) {
            RenderViewContentArray(
                views = viewContent.getJSONArray(keyName = "content"),
                navController = navController,
                customContentDataProvider = customContentDataProvider,
                actionHandlerProvider = actionHandlerProvider
            )
        }
    }
}

@Composable
fun MenuView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val title = viewContent.optString(keyName = "title", fallback = "")
    val views = viewContent.getJSONArray(keyName = "views")
    val context = LocalContext.current

    Box(modifier = modifier.getModifier(viewContent, context, customContentDataProvider)) {
        LegacyButton(onClick = { expanded = true }) {
            Text(title)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (0 until views.length()).forEach {
                val menuItem = views.getJSONObject(it)
                DropdownMenuItem(
                    text = {
                        AppstentView(
                            viewContent = menuItem,
                            navController = navController,
                            customContentDataProvider = customContentDataProvider,
                            actionHandlerProvider = actionHandlerProvider,
                            handlesTapActions = false,
                        )
                    },
                    enabled = menuItem.optBoolean(keyName = "isEnabled", fallback = true),
                    onClick = {
                        expanded = false
                        dispatchAppstentAction(
                            handlerName = menuItem.optString(keyName = "tapHandlerName", fallback = ""),
                            data = if (menuItem.has(keyName = "tapHandlerData")) {
                                menuItem.getJSONObject(keyName = "tapHandlerData")
                            } else {
                                null
                            },
                            localHandler = actionHandlerProvider,
                            globalHandler = ModuleConfigs.globalActionHandlerProvider,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun ProgressView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null
) {
    val context = LocalContext.current
    val label = viewContent.optString(keyName = "label", fallback = "")
    val total = viewContent.appstentResolvedDouble(keyName = "total", fallback = 100.0).takeIf { it > 0.0 } ?: 100.0
    val progress = resolveProgressValue(viewContent, customContentDataProvider)
    val progressFraction = (progress / total).toFloat().coerceIn(0f, 1f)
    val progressType = viewContent.optString(keyName = "progressType", fallback = "linear")
    val labelsHidden = viewContent.optBoolean(keyName = "labelsHidden", fallback = false)
    val progressTintColor = viewContent
        .optString(keyName = "progressTintColor", fallback = "")
        .takeIf { it.isNotEmpty() }
        ?.let { resolveAppstentColor(it, context, customContentDataProvider) }
        ?: MaterialTheme.colorScheme.primary

    Column(modifier = modifier.getModifier(viewContent, context, customContentDataProvider)) {
        if (!labelsHidden && label.isNotEmpty()) {
            val labelColor = viewContent
                .optString(keyName = "labelColor", fallback = "")
                .takeIf { it.isNotEmpty() }
                ?.let { resolveAppstentColor(it, context, customContentDataProvider) }
                ?: Color.Unspecified

            Text(text = label, color = labelColor)
        }

        if (progressType == "circular") {
            LegacyCircularProgressIndicator(
                progress = progressFraction,
                color = progressTintColor
            )
        } else {
            LegacyLinearProgressIndicator(
                progress = progressFraction,
                color = progressTintColor,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!labelsHidden) {
            val valueLabel = resolveProgressValueLabel(viewContent, customContentDataProvider)
            if (valueLabel.isNotEmpty()) {
                val valueLabelColor = viewContent
                    .optString(keyName = "valueLabelColor", fallback = "")
                    .takeIf { it.isNotEmpty() }
                    ?.let { resolveAppstentColor(it, context, customContentDataProvider) }
                    ?: Color.Unspecified

                Text(
                    text = valueLabel,
                    color = valueLabelColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun resolveProgressValue(
    viewContent: JSONObject,
    customContentDataProvider: CustomContentDataProvider?
): Double {
    val dynamicProgressField = viewContent.optString(keyName = "dynamicProgress", fallback = "")
    if (dynamicProgressField.isNotEmpty()) {
        return customContentDataProvider
            ?.getStringFor(dynamicProgressField)
            ?.toDoubleOrNull()
            ?: 0.0
    }

    return viewContent.appstentResolvedDouble(keyName = "progress", fallback = 0.0)
}

private fun resolveProgressValueLabel(
    viewContent: JSONObject,
    customContentDataProvider: CustomContentDataProvider?
): String {
    val dynamicValueLabelField = viewContent.optString(keyName = "dynamicValueLabel", fallback = "")
    if (dynamicValueLabelField.isNotEmpty()) {
        return customContentDataProvider?.getStringFor(dynamicValueLabelField) ?: ""
    }

    return viewContent.optString(keyName = "currentValueLabel", fallback = "")
}

@Composable
private fun RenderViewContentArray(
    views: JSONArray,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null,
    handlesTapActions: Boolean = true,
) {
    Column(modifier = modifier) {
        (0 until views.length()).forEach {
            AppstentView(
                viewContent = views.getJSONObject(it),
                navController = navController,
                customContentDataProvider = customContentDataProvider,
                actionHandlerProvider = actionHandlerProvider,
                handlesTapActions = handlesTapActions,
            )
        }
    }
}

@Composable
fun ImageView(viewContent: JSONObject, modifier: Modifier = Modifier, customContentDataProvider: CustomContentDataProvider? = null) {
    val context = LocalContext.current
    val imageSource = viewContent.optString(keyName = "source", fallback = "")
    val sourceType = viewContent.optString(keyName = "sourceType", fallback = "local")
    val dynamicImageSource = if (sourceType == "dynamic" || sourceType == "dynamic_system") {
        customContentDataProvider?.getStringFor(imageSource)
    } else {
        null
    }
    val resolvedImageSource = resolveImageSource(
        viewContent = viewContent,
        previewMode = ModuleConfigs.inPreviewMode,
        dynamicImageSource = dynamicImageSource,
    )

    val scalingMode = if (viewContent.optString(keyName = "scalingMode", "") == "scaledToFill") {
        ContentScale.Crop
    } else {
        ContentScale.Fit
    }

    val imageModifier = modifier
        .getModifier(viewContent, context, customContentDataProvider)
        .let { resolvedModifier ->
            if (viewContent.hasExplicitImageSize()) resolvedModifier else resolvedModifier.fillMaxWidth()
        }

    when (resolvedImageSource) {
        is ResolvedImageSource.Remote -> AsyncImage(resolvedImageSource.source, null,
            contentScale = scalingMode,
            modifier = imageModifier

        )
        is ResolvedImageSource.System -> Icon(
            name = resolvedImageSource.name,
            viewContent = viewContent,
            modifier = imageModifier,
            customContentDataProvider = customContentDataProvider,
            applyContentModifier = false,
            fitToContainer = !viewContent.hasExplicitImageSize(),
        )
        is ResolvedImageSource.Local -> {
            resolveLocalImageResourceId(context, resolvedImageSource.nameOrId)?.let { imageId ->
                Image(
                    painterResource(id = imageId),
                    null,
                    contentScale = scalingMode,
                    modifier = imageModifier,
                )
            }
        }
        ResolvedImageSource.Empty -> Unit
    }
}

internal sealed interface ResolvedImageSource {
    data class Remote(val source: String) : ResolvedImageSource
    data class System(val name: String) : ResolvedImageSource
    data class Local(val nameOrId: String) : ResolvedImageSource
    data object Empty : ResolvedImageSource
}

internal fun resolveImageSource(
    viewContent: JSONObject,
    previewMode: Boolean,
    dynamicImageSource: String?,
): ResolvedImageSource {
    val previewImageSource = viewContent.optString(keyName = "previewImageSource", fallback = "")
    if (previewMode && previewImageSource.isNotBlank()) {
        return ResolvedImageSource.Remote(previewImageSource)
    }

    val imageSource = viewContent.optString(keyName = "source", fallback = "")
    if (imageSource.isBlank()) return ResolvedImageSource.Empty

    return when (viewContent.optString(keyName = "sourceType", fallback = "local")) {
        "remote" -> ResolvedImageSource.Remote(imageSource)
        "system" -> ResolvedImageSource.System(imageSource)
        "dynamic", "dynamic_system" -> {
            val resolvedDynamicSource = dynamicImageSource?.takeIf { it.isNotBlank() }
                ?: return ResolvedImageSource.Empty

            when {
                viewContent.optString(keyName = "sourceType", fallback = "") == "dynamic_system" -> {
                    ResolvedImageSource.System(resolvedDynamicSource)
                }
                resolvedDynamicSource.isHttpUrl() -> ResolvedImageSource.Remote(resolvedDynamicSource)
                else -> ResolvedImageSource.Local(resolvedDynamicSource)
            }
        }
        else -> ResolvedImageSource.Local(imageSource)
    }
}

private fun String.isHttpUrl(): Boolean =
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

private fun resolveLocalImageResourceId(context: android.content.Context, nameOrId: String): Int? {
    nameOrId.toIntOrNull()?.takeIf { it != 0 }?.let { return it }

    val normalizedName = nameOrId
        .substringAfterLast('/')
        .substringBeforeLast('.')
    val resourceId = context.resources.getIdentifier(normalizedName, "drawable", context.packageName)
        .takeIf { it != 0 }
        ?: context.resources.getIdentifier(normalizedName, "mipmap", context.packageName)
            .takeIf { it != 0 }

    return resourceId
}

private fun JSONObject.hasExplicitImageSize(): Boolean = listOf(
    "width", "height", "idealWidth", "idealHeight",
    "minWidth", "maxWidth", "minHeight", "maxHeight",
    "percent_width", "percent_height", "min_width", "max_width", "min_height", "max_height",
    "fillMaxWidth", "fillMaxHeight", "fillModifier",
).any { has(keyName = it) }

@Composable
fun Icon(
    name: String,
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null,
    applyContentModifier: Boolean = true,
    fitToContainer: Boolean = false,
) {
    val context = LocalContext.current

    val icon = remember(name) { iconFromOutlinedPack(name) } ?: Icons.Outlined.Warning

    //foreground color
    var color: Color = Color.Black
    if (viewContent.has(keyName = "foregroundColor")) {
        val fgColor = viewContent.getString(keyName = "foregroundColor")
        color = resolveAppstentColor(fgColor, context, customContentDataProvider) ?: Color.Black
    }

    var iconModifier = if (applyContentModifier) {
            modifier.getModifier(viewContent, context, customContentDataProvider)
        } else {
            modifier
        }
    if (fitToContainer) {
        iconModifier = iconModifier.aspectRatio(icon.defaultWidth.value / icon.defaultHeight.value)
    }

    Icon(imageVector = icon, "",
        modifier = iconModifier,
        tint = color
    )
}

internal fun iconFromOutlinedPack(rawName: String): ImageVector? {
    when (rawName.trim().lowercase(Locale.ROOT)) {
        "house" -> return Icons.Outlined.Home
        "qrcode.viewfinder" -> return Icons.Outlined.QrCodeScanner
        "person.crop.circle.badge.plus" -> return Icons.Outlined.PersonAdd
        "slider.horizontal.3" -> return Icons.Outlined.Tune
        "curlybraces.square.fill" -> return Icons.Outlined.Code
        "chevron.right" -> return Icons.Outlined.ChevronRight
    }

    val normalizedName = rawName
        .split('.', '_', '-')
        .filter { it.isNotBlank() }
        .joinToString("") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase(Locale.getDefault())
                } else {
                    char.toString()
                }
            }
        }

    val className = "androidx.compose.material.icons.outlined.${normalizedName}Kt"
    val getterName = "get$normalizedName"

    return runCatching {
        val method = Class.forName(className)
            .methods
            .firstOrNull { it.name == getterName && it.parameterTypes.size == 1 }

        method?.invoke(null, Icons.Outlined) as? ImageVector
    }.getOrNull()
}

@Composable
fun VideoView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null
) {
    val context = LocalContext.current

    val sourceType = viewContent.optString(keyName = "sourceType", fallback = "remote")
    val source = viewContent.optString(keyName = "source", fallback = "")
    if (source.isBlank()) return

    val videoUri = if (sourceType == "local") "asset:///$source" else source

    val scalingModeString = viewContent.optString(keyName = "scalingMode", fallback = "")

    val scalingMode = when (scalingModeString)  {
        "scaledToFill" -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        "scaledToFit"-> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        else -> C.VIDEO_SCALING_MODE_DEFAULT
    }

    val playerContentSizeMode = when (scalingModeString)  {
        "scaledToFill" -> RESIZE_MODE_FILL
        "scaledToFit"-> RESIZE_MODE_FIT
        else -> RESIZE_MODE_FIT
    }

    val playbackConfig = resolveVideoPlaybackConfig(viewContent)

    val exoPlayer = remember(context, videoUri) {
        ExoPlayer.Builder(context)
        .build()
        .also { exoPlayer ->
            val mediaItem = MediaItem.Builder()
                .setUri(videoUri)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    SideEffect {
        exoPlayer.playWhenReady = playbackConfig.autoplay
        exoPlayer.volume = if (playbackConfig.isMuted) 0f else 1f
        exoPlayer.videoScalingMode = scalingMode
        exoPlayer.repeatMode = if (playbackConfig.autoLoop) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                useController = playbackConfig.showPlaybackControls
                player = exoPlayer
                layoutParams =
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT)

                resizeMode =  playerContentSizeMode
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
            playerView.useController = playbackConfig.showPlaybackControls
            playerView.resizeMode = playerContentSizeMode
        },
        modifier = modifier.getModifier(viewContent, context, customContentDataProvider),
    )

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
}

internal data class VideoPlaybackConfig(
    val autoplay: Boolean,
    val isMuted: Boolean,
    val autoLoop: Boolean,
    val showPlaybackControls: Boolean,
)

internal fun resolveVideoPlaybackConfig(viewContent: JSONObject) = VideoPlaybackConfig(
    autoplay = viewContent.optBoolean(keyName = "autoplay", fallback = false),
    isMuted = viewContent.optBoolean(keyName = "isMuted", fallback = false),
    autoLoop = viewContent.optBoolean(keyName = "autoLoop", fallback = false),
    showPlaybackControls = viewContent.optBoolean(keyName = "showPlaybackControls", fallback = false),
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null,
) {
    val context = LocalContext.current

    val sourceType = viewContent.optString(keyName = "sourceType", "")
    val source = viewContent.optString(keyName = "source", fallback = "")
    if (source.isBlank()) return

    var backEnabled by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun loadContent(target: WebView) {
        val requestedContent = sourceType to source
        if (loadedContent == requestedContent) return

        if (sourceType == "embedded") {
            target.loadDataWithBaseURL(null, source, "text/html", "UTF-8", null)
        } else {
            target.loadUrl(source)
        }
        loadedContent = requestedContent
    }

    AndroidView(
        modifier = modifier.getModifier(viewContent, context, customContentDataProvider),
        factory = { viewContext ->
            WebView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        backEnabled = view.canGoBack()
                    }
                }
                settings.javaScriptEnabled = true
                loadContent(this)
                webView = this
            }
        }, update = {
            webView = it
            loadContent(it)
        })

    BackHandler(enabled = backEnabled) {
        webView?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
        }
    }
}

enum class Direction {
    x, y, z
}

@Composable
fun PagerView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current
    val tabs = viewContent.getJSONArray(keyName = "tabs")
    if (tabs.length() == 0) return

    val pagerState = rememberPagerState(pageCount = tabs::length)
    val pagerBehavior = resolvePagerBehavior(viewContent)
    val pagerContainerModifier = modifier.getViewportContainerModifier(
        viewContent,
        context,
        customContentDataProvider,
    )

    LaunchedEffect(pagerBehavior.autorotate, tabs.length()) {
        if (!pagerBehavior.autorotate || tabs.length() < 2) return@LaunchedEffect

        while (true) {
            delay(PAGER_AUTOROTATE_INTERVAL_MILLIS)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % tabs.length())
        }
    }

    Column(modifier = pagerContainerModifier) {
        val pagerModifier = Modifier
            .weight(1f)
            .fillMaxWidth()

        val pageContent: @Composable PagerScope.(Int) -> Unit = { currentPage ->
            val tab = tabs.getJSONObject(currentPage)
            val tabContent = tab.getJSONObject(keyName = "tabContent")

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pagerEffectModifier(
                        behavior = pagerBehavior,
                        currentPage = currentPage,
                        selectedPage = pagerState.currentPage,
                        selectedPageOffset = pagerState.currentPageOffsetFraction,
                    ),
            ) {
                if (tabContent.has(keyName = "type")) {
                    AppstentView(
                        viewContent = tabContent,
                        navController = navController,
                        customContentDataProvider = customContentDataProvider,
                        actionHandlerProvider = actionHandlerProvider,
                    )
                }
            }
        }

        if (pagerBehavior.axis == PagerAxis.Vertical) {
            VerticalPager(
                state = pagerState,
                modifier = pagerModifier,
                pageContent = pageContent,
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = pagerModifier,
                pageContent = pageContent,
            )
        }

        if (pagerBehavior.showsPageIndex(tabs.length())) {
            Spacer(modifier = Modifier.padding(4.dp))

            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                DotsIndicator(
                    totalDots = tabs.length(),
                    selectedIndex = pagerState.currentPage,
                    selectedColor = Color(0xFF666666),
                    unSelectedColor = Color(0xFFE6E6E6),
                )
            }
        }
    }
}

private const val PAGER_AUTOROTATE_INTERVAL_MILLIS = 3_000L

internal enum class PagerAxis {
    Horizontal,
    Vertical,
}

internal enum class PageIndexMode {
    Show,
    Hide,
    Automatic,
}

internal data class PagerBehavior(
    val axis: PagerAxis,
    val pageIndexMode: PageIndexMode,
    val autorotate: Boolean,
    val usesThreeDimensionalRotation: Boolean,
    val rotationAngle: Float,
) {
    fun showsPageIndex(pageCount: Int): Boolean = when (pageIndexMode) {
        PageIndexMode.Show -> true
        PageIndexMode.Hide -> false
        PageIndexMode.Automatic -> pageCount > 1
    }
}

internal fun resolvePagerBehavior(viewContent: JSONObject) = PagerBehavior(
    axis = if (viewContent.optString(keyName = "pagerAxis", fallback = "horizontal") == "vertical") {
        PagerAxis.Vertical
    } else {
        PagerAxis.Horizontal
    },
    pageIndexMode = when (viewContent.optString(keyName = "pageIndexMode", fallback = "automatic")) {
        "show" -> PageIndexMode.Show
        "hide" -> PageIndexMode.Hide
        else -> PageIndexMode.Automatic
    },
    autorotate = viewContent.optBoolean(keyName = "autorotate", fallback = false),
    usesThreeDimensionalRotation =
        viewContent.optString(keyName = "effects", fallback = "") == "3DRotate",
    rotationAngle = viewContent.appstentResolvedDouble(keyName = "rotationAngle", fallback = -10.0).toFloat(),
)

private fun Modifier.pagerEffectModifier(
    behavior: PagerBehavior,
    currentPage: Int,
    selectedPage: Int,
    selectedPageOffset: Float,
): Modifier {
    if (!behavior.usesThreeDimensionalRotation) return this

    val pageOffset = (selectedPage - currentPage) + selectedPageOffset
    return graphicsLayer {
        if (behavior.axis == PagerAxis.Vertical) {
            rotationX = pageOffset * behavior.rotationAngle
        } else {
            rotationY = pageOffset * behavior.rotationAngle
        }
        cameraDistance = 8f * density
    }
}

@Composable
fun DotsIndicator(totalDots : Int, selectedIndex : Int, selectedColor: Color, unSelectedColor: Color) {

    LazyRow(modifier = Modifier
        .wrapContentWidth()
        .wrapContentHeight()
    ) {

        items(totalDots) { index ->
            if (index == selectedIndex) {
                Box(modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                )
            } else {
                Box(modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(unSelectedColor)
                )
            }

            if (index != totalDots - 1) {
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
            }
        }
    }
}

@Composable
fun StackView(
    viewContent: JSONObject,
    direction: Direction,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current

    val views = viewContent.getJSONArray(keyName = "views")
    val scrollable = viewContent.optBoolean(keyName ="scrollable", false)
    val spacing = viewContent.appstentResolvedDouble(keyName = "spacing", fallback = 0.0).dp
    val arrangement = viewContent.optString(keyName = "arrangement", fallback = "")

    val appstentModifier = modifier
        .getModifier(viewContent, context, customContentDataProvider)

    val columnBaseModifier = if (arrangement.isNotEmpty() && !scrollable) {
        appstentModifier.fillMaxHeight()
    } else {
        appstentModifier
    }
    val rowBaseModifier = if (arrangement.isNotEmpty() && !scrollable) {
        appstentModifier.fillMaxWidth()
    } else {
        appstentModifier
    }
    val rowModifier: Modifier = if (scrollable) {
        rowBaseModifier.horizontalScroll(rememberScrollState())
    } else {
        rowBaseModifier
    }

    val horizontalArrangement: Arrangement.Horizontal = when (arrangement) {
        "spaceBetween" -> Arrangement.SpaceBetween
        "spaceAround" -> Arrangement.SpaceAround
        "spaceEvenly" -> Arrangement.SpaceEvenly
        "end" -> Arrangement.spacedBy(spacing, Alignment.End)
        "center" -> Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
        else -> Arrangement.spacedBy(spacing, Alignment.Start)
    }
    val verticalArrangement: Arrangement.Vertical = when (arrangement) {
        "spaceBetween" -> Arrangement.SpaceBetween
        "spaceAround" -> Arrangement.SpaceAround
        "spaceEvenly" -> Arrangement.SpaceEvenly
        "bottom", "end" -> Arrangement.spacedBy(spacing, Alignment.Bottom)
        "center" -> Arrangement.spacedBy(spacing, Alignment.CenterVertically)
        else -> Arrangement.spacedBy(spacing, Alignment.Top)
    }

    when (direction) {
        Direction.x -> Row(modifier = rowModifier,
                horizontalArrangement = horizontalArrangement) {

            var alignmentVal = Alignment.CenterVertically

            if (viewContent.has(keyName = "alignment")) {
                val alignmentString = viewContent.getString(keyName = "alignment")
                alignmentVal = getVerticalAlignment(alignmentString)
            }

            (0 until views.length()).forEach {
                val viewContentIt = views.getJSONObject(it)
                var itemModifier: Modifier = Modifier.align(alignmentVal)

                if (viewContentIt.optString("type") == "spacer") {
                    if (!viewContentIt.has("minLength") && !viewContentIt.has("width") && !viewContentIt.has("height")) {
                        itemModifier = itemModifier.weight(1f)
                    }
                }

                AppstentView(viewContent = viewContentIt,
                    itemModifier,
                    navController,
                    customContentDataProvider,
                    actionHandlerProvider)
            }
        }

        Direction.y -> {

            var alignmentVal = Alignment.Start

            if (viewContent.has(keyName = "alignment")) {
                val alignmentString = viewContent.getString(keyName = "alignment")
                alignmentVal = getHorizontalAlignment(alignmentString)
            }

            val inheritedScrollableParent = LocalVerticallyScrollableAppstentParent.current
            val appliesVerticalScroll = shouldApplyVerticalScroll(
                viewContent = viewContent,
                isInsideVerticalScroller = inheritedScrollableParent,
            )
            val resolvedColumnModifier = if (appliesVerticalScroll) {
                columnBaseModifier.verticalScroll(rememberScrollState())
            } else {
                columnBaseModifier
            }
            CompositionLocalProvider(
                LocalVerticallyScrollableAppstentParent provides
                    (appliesVerticalScroll || inheritedScrollableParent),
            ) {
                Column(modifier = resolvedColumnModifier,
                    verticalArrangement = verticalArrangement) {
                    (0 until views.length()).forEach {
                        val viewContentIt = views.getJSONObject(it)
                        var itemModifier: Modifier = Modifier.align(alignmentVal)

                        if (viewContentIt.optString("type") == "spacer") {
                            if (!viewContentIt.has("minLength") && !viewContentIt.has("width") && !viewContentIt.has("height")) {
                                itemModifier = itemModifier.weight(1f)
                            }
                        }

                        AppstentView(viewContent = viewContentIt,
                            itemModifier,
                            navController,
                            customContentDataProvider,
                            actionHandlerProvider)
                    }
                }
            }
        }

        Direction.z -> {

            var alignmentVal = Alignment.Center

            if (viewContent.has(keyName = "alignment")) {
                val alignmentString = viewContent.getString(keyName = "alignment")
                alignmentVal = getAlignment(alignmentString)
            }

            Box(modifier = appstentModifier,
                contentAlignment = alignmentVal) {

                (0 until views.length()).forEach {

                    AppstentView(viewContent = views.getJSONObject(it),
                        Modifier
                            .align(alignmentVal)
                        , navController,
                        customContentDataProvider,
                        actionHandlerProvider)
                }
            }
        }
    }
}


@Composable
fun IncludedView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current
    val fromSource = viewContent.optString(keyName = "source", fallback = "")
    val overrides = viewContent.toString()
    val repository = remember { ViewContentRepository() }
    var state by remember(fromSource, overrides) {
        mutableStateOf<IncludedContentState>(IncludedContentState.Loading)
    }

    LaunchedEffect(fromSource, overrides) {
        state = IncludedContentState.Loading
        if (fromSource.isBlank()) {
            state = IncludedContentState.Error
            return@LaunchedEffect
        }

        state = try {
            val baseContent = repository.getContent(fromSource)
            resolveIncludedContentState(
                baseContent = baseContent,
                wrapperContent = JSONObject(overrides),
                error = null,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            resolveIncludedContentState(
                baseContent = null,
                wrapperContent = JSONObject(overrides),
                error = error,
            )
        }
    }

    when (val currentState = state) {
        IncludedContentState.Loading -> Box(
            modifier = modifier.getModifier(viewContent, context, customContentDataProvider),
            contentAlignment = Alignment.Center,
        ) {
            LegacyCircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = "Loading included content"
                },
            )
        }

        IncludedContentState.Error -> Box(
            modifier = modifier.getModifier(viewContent, context, customContentDataProvider),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Unable to load content",
                color = MaterialTheme.colorScheme.error,
            )
        }

        is IncludedContentState.Loaded -> AppstentView(
            viewContent = currentState.content,
            modifier = modifier,
            navController,
            customContentDataProvider,
            actionHandlerProvider,
        )
    }
}

internal sealed interface IncludedContentState {
    data object Loading : IncludedContentState
    data class Loaded(val content: JSONObject) : IncludedContentState
    data object Error : IncludedContentState
}

internal fun resolveIncludedContentState(
    baseContent: JSONObject?,
    wrapperContent: JSONObject,
    error: Throwable?,
): IncludedContentState = if (error != null || baseContent == null) {
    IncludedContentState.Error
} else {
    IncludedContentState.Loaded(mergeIncludedContent(baseContent, wrapperContent))
}

internal fun mergeIncludedContent(baseContent: JSONObject, wrapperContent: JSONObject): JSONObject {
    val mergedContent = JSONObject(baseContent.toString())
    val keys = wrapperContent.keys()

    while (keys.hasNext()) {
        val key = keys.next()
        if (key != "type" && key != "source") {
            mergedContent.put(key, wrapperContent.get(key))
        }
    }

    return mergedContent
}

@Composable
fun GridView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current

    val views = viewContent.getJSONArray(keyName = "views")

    val rowSpacing = viewContent.appstentResolvedDouble(keyName = "rowSpacing", fallback = 0.0)
    val colSpacing = viewContent.appstentResolvedDouble(keyName = "colSpacing", fallback = 0.0)

    val gridCells: GridCells = getGridCells(viewContent)

    val gridType = viewContent.optString(keyName = "gridType", "vertical")
    val isInsideVerticalScroller = LocalVerticallyScrollableAppstentParent.current
    val gridModifier = modifier.getViewportContainerModifier(
        viewContent,
        context,
        customContentDataProvider,
    )

    if (gridType == "horizontal") {
        LazyHorizontalGrid(rows = gridCells,
            modifier = gridModifier,
            horizontalArrangement = Arrangement.spacedBy(colSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing.dp)) {

            items(views.length()) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppstentView(viewContent = views.getJSONObject(it), Modifier, navController, customContentDataProvider, actionHandlerProvider)
                }
            }
        }
    } else if (isInsideVerticalScroller && !viewContent.has(keyName = "height")) {
        IntrinsicVerticalGrid(
            viewContent = viewContent,
            modifier = modifier.getModifier(viewContent, context, customContentDataProvider),
            navController = navController,
            customContentDataProvider = customContentDataProvider,
            actionHandlerProvider = actionHandlerProvider,
        )
    } else {
        LazyVerticalGrid(columns = gridCells,
            modifier = gridModifier,
            horizontalArrangement = Arrangement.spacedBy(colSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing.dp)) {

            items(views.length()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppstentView(viewContent = views.getJSONObject(it), Modifier, navController, customContentDataProvider, actionHandlerProvider)
                }
            }
        }
    }
}

@Composable
private fun IntrinsicVerticalGrid(
    viewContent: JSONObject,
    modifier: Modifier,
    navController: NavHostController?,
    customContentDataProvider: CustomContentDataProvider?,
    actionHandlerProvider: ActionHandlerProvider?,
) {
    val views = viewContent.getJSONArray(keyName = "views")
    val rowSpacing = viewContent.appstentResolvedDouble(keyName = "rowSpacing", fallback = 0.0).dp
    val columnSpacing = viewContent.appstentResolvedDouble(keyName = "colSpacing", fallback = 0.0).dp

    Layout(
        modifier = modifier,
        content = {
            (0 until views.length()).forEach { index ->
                AppstentView(
                    viewContent = views.getJSONObject(index),
                    navController = navController,
                    customContentDataProvider = customContentDataProvider,
                    actionHandlerProvider = actionHandlerProvider,
                )
            }
        },
    ) { measurables, constraints ->
        val availableWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val columnSpacingPx = columnSpacing.roundToPx()
        val rowSpacingPx = rowSpacing.roundToPx()
        val trackSizes = calculateIntrinsicVerticalGridTrackSizes(
            viewContent = viewContent,
            availableSize = availableWidth,
            spacing = columnSpacingPx,
            density = density,
        ).ifEmpty { listOf(availableWidth) }

        val placeables = measurables.mapIndexed { index, measurable ->
            val trackWidth = trackSizes[index % trackSizes.size]
            measurable.measure(
                androidx.compose.ui.unit.Constraints(
                    minWidth = trackWidth,
                    maxWidth = trackWidth,
                    minHeight = 0,
                    maxHeight = androidx.compose.ui.unit.Constraints.Infinity,
                ),
            )
        }
        val rowCount = if (placeables.isEmpty()) 0 else
            (placeables.size + trackSizes.size - 1) / trackSizes.size
        val rowHeights = List(rowCount) { row ->
            val start = row * trackSizes.size
            val end = min(start + trackSizes.size, placeables.size)
            (start until end).maxOfOrNull { placeables[it].height } ?: 0
        }
        val contentHeight = rowHeights.sum() + rowSpacingPx * max(0, rowCount - 1)
        val layoutWidth = (
            trackSizes.sum() + columnSpacingPx * max(0, trackSizes.size - 1)
        ).coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            val columnOffsets = trackSizes.runningFold(0) { offset, width ->
                offset + width + columnSpacingPx
            }
            val rowOffsets = rowHeights.runningFold(0) { offset, height ->
                offset + height + rowSpacingPx
            }
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = columnOffsets[index % trackSizes.size],
                    y = rowOffsets[index / trackSizes.size],
                )
            }
        }
    }
}

internal fun calculateIntrinsicVerticalGridTrackSizes(
    viewContent: JSONObject,
    availableSize: Int,
    spacing: Int,
    density: Float,
): List<Int> {
    if (viewContent.has(keyName = "minCellWidth")) {
        val minimumWidth = max(
            1,
            (viewContent.appstentResolvedDouble(keyName = "minCellWidth") * density).toInt(),
        )
        val count = max(1, (availableSize + spacing) / (minimumWidth + max(0, spacing)))
        val availableForCells = max(0, availableSize - max(0, spacing) * (count - 1))
        return List(count) { availableForCells / count }
    }

    if (viewContent.has(keyName = "numColumns")) {
        val count = max(1, viewContent.appstentResolvedInt(keyName = "numColumns", fallback = 1))
        val availableForCells = max(0, availableSize - max(0, spacing) * (count - 1))
        return List(count) { availableForCells / count }
    }

    val trackSpecs = resolveGridTrackSpecs(viewContent.getJSONArray(keyName = "columns"))
    return if (trackSpecs.isEmpty()) {
        listOf(availableSize)
    } else {
        calculateGridTrackSizes(trackSpecs, availableSize, spacing, density)
    }
}

private fun getGridCells(viewContent: JSONObject): GridCells {

    return if (viewContent.has(keyName = "minCellWidth")) {

        GridCells.Adaptive(viewContent.appstentResolvedDouble(keyName = "minCellWidth").dp)

    } else if (viewContent.has(keyName = "numColumns") || viewContent.has(keyName = "numRows") ) {

        val rowColCount = if (viewContent.has(keyName = "numColumns")) {
            viewContent.appstentResolvedInt(keyName = "numColumns", fallback = 1)
        } else {
            viewContent.appstentResolvedInt(keyName = "numRows", fallback = 1)
        }

        GridCells.Fixed(rowColCount)
    }
    else {

        val gridRowColConfigs: JSONArray = when {
            viewContent.has(keyName = "columns") -> viewContent.getJSONArray(keyName = "columns")
            viewContent.has(keyName = "rows") -> viewContent.getJSONArray(keyName = "rows")
            else -> JSONArray()
        }
        val trackSpecs = resolveGridTrackSpecs(gridRowColConfigs)

        if (trackSpecs.isEmpty()) {
            return GridCells.Fixed(1)
        }

        object: GridCells {
            override fun Density.calculateCrossAxisCellSizes(
                availableSize: Int,
                spacing: Int
            ): List<Int> {

                return calculateGridTrackSizes(
                    trackSpecs = trackSpecs,
                    availableSize = availableSize,
                    spacing = spacing,
                    density = density,
                )
            }
        }
    }
}

internal sealed interface GridTrackSpec {
    data class Fixed(val width: Double) : GridTrackSpec
    data class Flexible(val minWidth: Double, val maxWidth: Double) : GridTrackSpec
    data class Adaptive(val minWidth: Double, val maxWidth: Double) : GridTrackSpec
}

internal fun resolveGridTrackSpecs(configs: JSONArray): List<GridTrackSpec> =
    (0 until configs.length()).map { index ->
        val config = configs.getJSONObject(index)
        val minWidth = max(0.0, config.appstentResolvedDouble("minWidth", fallback = 0.0))
        val authoredMax = config.appstentResolvedDouble("maxWidth", fallback = Double.POSITIVE_INFINITY)
        val maxWidth = max(minWidth, authoredMax)

        when (config.optString("itemType", fallback = "adaptive")) {
            "fixed" -> GridTrackSpec.Fixed(
                max(0.0, config.appstentResolvedDouble("width", fallback = 0.0)),
            )
            "flexible" -> GridTrackSpec.Flexible(minWidth, maxWidth)
            else -> GridTrackSpec.Adaptive(minWidth, maxWidth)
        }
    }

internal fun calculateGridTrackSizes(
    trackSpecs: List<GridTrackSpec>,
    availableSize: Int,
    spacing: Int,
    density: Float,
): List<Int> {
    if (trackSpecs.isEmpty()) return emptyList()

    val totalSpacing = max(0, spacing) * max(0, trackSpecs.size - 1)
    val availableForCells = max(0.0, availableSize.toDouble() - totalSpacing)
    val minimumSizes = trackSpecs.map { spec ->
        when (spec) {
            is GridTrackSpec.Fixed -> spec.width * density
            is GridTrackSpec.Flexible -> spec.minWidth * density
            is GridTrackSpec.Adaptive -> spec.minWidth * density
        }
    }.toMutableList()
    val maximumSizes = trackSpecs.map { spec ->
        when (spec) {
            is GridTrackSpec.Fixed -> spec.width * density
            is GridTrackSpec.Flexible -> spec.maxWidth * density
            // An adaptive entry owns a cross-axis segment that can expand into
            // multiple bounded cells; maxWidth applies to each resulting cell.
            is GridTrackSpec.Adaptive -> Double.POSITIVE_INFINITY
        }
    }
    val expandable = trackSpecs.indices
        .filter { trackSpecs[it] !is GridTrackSpec.Fixed }
        .toMutableSet()
    var remaining = max(0.0, availableForCells - minimumSizes.sum())

    while (remaining > 0.5 && expandable.isNotEmpty()) {
        val share = remaining / expandable.size
        var distributed = 0.0
        val saturated = mutableListOf<Int>()

        for (index in expandable) {
            val capacity = maximumSizes[index] - minimumSizes[index]
            val addition = min(share, capacity)
            minimumSizes[index] += addition
            distributed += addition
            if (capacity <= share + 0.5) saturated += index
        }

        remaining -= distributed
        expandable.removeAll(saturated.toSet())
        if (distributed <= 0.5) break
    }

    return buildList {
        trackSpecs.forEachIndexed { index, spec ->
            val allocatedSize = minimumSizes[index]
            if (spec !is GridTrackSpec.Adaptive) {
                add(max(0, allocatedSize.toInt()))
                return@forEachIndexed
            }

            val minimumCellSize = spec.minWidth * density
            val maximumCellSize = spec.maxWidth * density
            if (minimumCellSize <= 0.0) {
                add(max(0, min(allocatedSize, maximumCellSize).toInt()))
                return@forEachIndexed
            }

            val adaptiveCount = max(
                1,
                ((allocatedSize + spacing) / (minimumCellSize + max(0, spacing))).toInt(),
            )
            val adaptiveSpacing = max(0, spacing) * max(0, adaptiveCount - 1)
            val adaptiveCellSize = min(
                maximumCellSize,
                max(0.0, (allocatedSize - adaptiveSpacing) / adaptiveCount),
            )
            repeat(adaptiveCount) {
                add(max(0, adaptiveCellSize.toInt()))
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current
    val listModifier = modifier.getViewportContainerModifier(
        viewContent,
        context,
        customContentDataProvider,
    )
    val listBehavior = resolveListStyleBehavior(viewContent)
    val contentPadding = PaddingValues(
        horizontal = listBehavior.horizontalInset.dp,
        vertical = listBehavior.verticalInset.dp,
    )

    if (viewContent.has(keyName = "sections")) {

        val sections = viewContent.getJSONArray(keyName = "sections")

        LazyColumn(
            modifier = listModifier,
            contentPadding = contentPadding,
        ) {

            (0 until sections.length()).forEach { sectionIndex ->

                val sectionObject = sections.getJSONObject(sectionIndex)
                val text = sectionObject.optString(keyName = "title", fallback = "")
                stickyHeader {
                    Text(
                        text = text,
                        style = LegacyTypography.subtitle2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = listBehavior.headerVerticalPadding.dp),
                    )
                }

                val views = sectionObject.getJSONArray(keyName = "views")

                (0 until views.length()).forEach { viewIndex ->

                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AppstentView(viewContent = views.getJSONObject(viewIndex), Modifier, navController, customContentDataProvider, actionHandlerProvider)
                            if (listBehavior.showsDividers && viewIndex < views.length() - 1) {
                                PixelCompatibleDivider()
                            }
                        }
                    }
                }

                if (listBehavior.sectionSpacing > 0 && sectionIndex < sections.length() - 1) {
                    item { Spacer(Modifier.height(listBehavior.sectionSpacing.dp)) }
                }
            }
        }
    } else {
        val views = viewContent.getJSONArray(keyName = "views")

        LazyColumn(
            modifier = listModifier,
            contentPadding = contentPadding,
        ) {
            (0 until views.length()).forEach { viewIndex ->
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AppstentView(viewContent = views.getJSONObject(viewIndex), Modifier, navController, customContentDataProvider, actionHandlerProvider)
                        if (listBehavior.showsDividers && viewIndex < views.length() - 1) {
                            PixelCompatibleDivider()
                        }
                    }
                }
            }
        }
    }
}

internal enum class ListStyleKind {
    Plain,
    Sidebar,
    Inset,
    Grouped,
    InsetGrouped,
}

internal data class ListStyleBehavior(
    val kind: ListStyleKind,
    val horizontalInset: Int,
    val verticalInset: Int,
    val headerVerticalPadding: Int,
    val sectionSpacing: Int,
    val showsDividers: Boolean,
)

internal fun resolveListStyleBehavior(viewContent: JSONObject): ListStyleBehavior =
    when (viewContent.optString("listStyle", fallback = "plainListStyle")) {
        "sidebar", "sidebarListStyle" -> ListStyleBehavior(ListStyleKind.Sidebar, 12, 8, 8, 8, false)
        "inset", "insetListStyle" -> ListStyleBehavior(ListStyleKind.Inset, 16, 8, 6, 0, true)
        "grouped", "groupedListStyle" -> ListStyleBehavior(ListStyleKind.Grouped, 0, 12, 8, 20, false)
        "insetGrouped", "insetGroupedListStyle" -> ListStyleBehavior(ListStyleKind.InsetGrouped, 16, 12, 8, 20, false)
        else -> ListStyleBehavior(ListStyleKind.Plain, 0, 0, 6, 0, true)
    }

@Composable
fun NavigationApstentView(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current

    val views = viewContent.getJSONArray(keyName = "views")
    val navigationBarBehavior = resolveNavigationBarBehavior(viewContent)
    val toolbarViews = viewContent.getJSONArray(keyName = "toolbarViews")
    val rootRoute = "__appstent_root__"
    val navModifier = modifier.getViewportContainerModifier(
        viewContent,
        context,
        customContentDataProvider,
    )
    val navController = rememberNavController()

    Scaffold(
        modifier = navModifier,
        topBar = {
            if (!navigationBarBehavior.isHidden) {
                LegacyTopAppBar(
                    title = { Text(navigationBarBehavior.title) },
                    actions = {
                        (0 until toolbarViews.length()).forEach { index ->
                            AppstentView(
                                viewContent = toolbarViews.getJSONObject(index),
                                navController = navController,
                                customContentDataProvider = customContentDataProvider,
                                actionHandlerProvider = actionHandlerProvider,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = rootRoute,
        ) {
            composable(rootRoute) {
                CurrentNavScreenContent(
                    views,
                    navController,
                    customContentDataProvider,
                    actionHandlerProvider,
                )
            }

            composable(RemoteIncludedDestinationPattern) { backStackEntry ->
                val source = decodeRemoteIncludedSource(
                    backStackEntry.arguments?.getString(RemoteIncludedSourceArgument),
                )

                if (source != null) {
                    IncludedView(
                        viewContent = JSONObject()
                            .put("type", "included")
                            .put("source", source),
                        navController = navController,
                        customContentDataProvider = customContentDataProvider,
                        actionHandlerProvider = actionHandlerProvider,
                    )
                }
            }

            // Crawl through the view hierarchy to create composables for routes.
            navigationComposable(viewContent, this, customContentDataProvider, actionHandlerProvider)
        }
    }
}

internal data class NavigationBarBehavior(
    val title: String,
    val isHidden: Boolean,
)

internal fun resolveNavigationBarBehavior(viewContent: JSONObject): NavigationBarBehavior {
    val title = viewContent.optString(keyName = "barTitle", fallback = "")
    val hasToolbarContent = (viewContent.optJSONArray("toolbarViews")?.length() ?: 0) > 0
    val isHidden = if (viewContent.has("isBarHidden")) {
        viewContent.optBoolean(keyName = "isBarHidden", fallback = false)
    } else {
        title.isBlank() && !hasToolbarContent
    }

    return NavigationBarBehavior(title = title, isHidden = isHidden)
}

@Composable
private fun CurrentNavScreenContent(
    views: JSONArray,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    (0 until views.length()).forEach {
        val viewContentIt = views.getJSONObject(it)
        AppstentView(
            viewContent = viewContentIt,
            navController = navController,
            customContentDataProvider = customContentDataProvider,
            actionHandlerProvider = actionHandlerProvider,
        )
    }
}

private fun navigationComposable(
    viewContent: JSONObject,
    navGraphBuilder: NavGraphBuilder,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {


    if (!viewContent.has(keyName = "views")) {
        return
    }

    val views = viewContent.getJSONArray(keyName = "views")

    (0 until views.length()).forEach {

        val viewContentIt = views.getJSONObject(it)

        if (viewContentIt.optString(keyName = "type", "") == "navigationLink") {

            if (viewContentIt.optString(keyName = "routeType", "") == "custom" &&
                viewContentIt.has(keyName = "customViewName")) {

                val viewName = viewContentIt.getString(keyName = "customViewName")

                navGraphBuilder.composable(viewName) {
                    ModuleConfigs.customContentViewProvider?.CustomComposable(
                        ModuleConfigs.CustomContentViewContext(
                            viewContent = viewContentIt,
                            viewName = viewName,
                            modifier = Modifier,
                            customContentDataProvider = customContentDataProvider,
                            actionHandlerProvider = actionHandlerProvider,
                        ),
                    )
                }
            }
            else if (viewContentIt.has(keyName = "route")) {
                val route = viewContentIt.getString(keyName = "route")

                navGraphBuilder.composable(route) {
                    IncludedView(
                        viewContent = JSONObject()
                            .put("type", "included")
                            .put("source", route),
                        customContentDataProvider = customContentDataProvider,
                        actionHandlerProvider = actionHandlerProvider
                    )
                }
            }

        } else if (viewContentIt.has(keyName = "views")) {
            navigationComposable(
                viewContentIt,
                navGraphBuilder,
                customContentDataProvider,
                actionHandlerProvider,
            )
        }
    }
}

@Composable
fun NavigationApstentLink(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {

    val context = LocalContext.current
    val triggerView = viewContent.getJSONObject(keyName = "triggerView")
    val actions = LocalAncestorAppstentActions.current

    val isCustomRoute = viewContent.optString(keyName = "routeType", "") == "custom" &&
        viewContent.has(keyName = "customViewName")

    val route: String = if (isCustomRoute) {

        viewContent.getString(keyName = "customViewName")
    }
    else {
        viewContent.optString(keyName = "route", "")
    }

    val navLinkModifier = modifier
        .getModifier(viewContent, context, customContentDataProvider)
        .clickable(
            enabled = viewContent.optBoolean(keyName = "isEnabled", fallback = true),
        ) {
            dispatchAppstentActions(actions, actionHandlerProvider)
            if (route.isNotBlank()) {
                val destination = if (isCustomRoute) {
                    route
                } else {
                    remoteIncludedDestination(route)
                }
                navController?.navigate(destination)
            }
        }

    AppstentView(viewContent = triggerView, navLinkModifier, navController, customContentDataProvider, actionHandlerProvider)
}

private const val RemoteIncludedDestination = "__appstent_remote_included__"
private const val RemoteIncludedSourceArgument = "source"
private const val RemoteIncludedDestinationPattern =
    "$RemoteIncludedDestination/{$RemoteIncludedSourceArgument}"

internal fun remoteIncludedDestination(source: String): String {
    val encodedSource = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(source.toByteArray(Charsets.UTF_8))
    return "$RemoteIncludedDestination/$encodedSource"
}

internal fun decodeRemoteIncludedSource(encodedSource: String?): String? {
    if (encodedSource.isNullOrBlank()) {
        return null
    }

    return runCatching {
        String(Base64.getUrlDecoder().decode(encodedSource), Charsets.UTF_8)
    }.getOrNull()
}

@Composable
fun BottomBar(
    viewContent: JSONObject,
    modifier: Modifier = Modifier,
    customContentDataProvider: CustomContentDataProvider? = null,
    actionHandlerProvider: ActionHandlerProvider? = null
) {
    val context = LocalContext.current

    val tabs = viewContent.getJSONArray(keyName = "tabs")

    if (tabs.length() <= 0) {
        return
    }

    val firstTabRoute = bottomBarRoute(0)
    val navController = rememberNavController()
    val tabContainerModifier = modifier.getViewportContainerModifier(
        viewContent,
        context,
        customContentDataProvider,
    )
    val bottomBarColor = viewContent
        .optString(keyName = "backgroundColor", fallback = "")
        .takeIf { it.isNotEmpty() }
        ?.let { colorString ->
            resolveAppstentColor(colorString, context, customContentDataProvider)
        } ?: MaterialTheme.colorScheme.background

    Scaffold(
        modifier = tabContainerModifier,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = bottomBarColor,
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                (0 until tabs.length()).forEach {

                    val tab = tabs.getJSONObject(it)
                    val title = tab.optString(keyName = "title", fallback = "")
                    val icon = if (tab.has(keyName = "icon")) {
                        tab.getJSONObject(keyName = "icon")
                    } else {
                        null
                    }
                    val route = bottomBarRoute(it)

                    NavigationBarItem(
                        icon = {
                            if (icon != null) {
                                BottomBarIcon(
                                    viewContent = icon,
                                    title = title,
                                    customContentDataProvider = customContentDataProvider,
                                    actionHandlerProvider = actionHandlerProvider,
                                )
                            }
                        },
                        label = { Text(title) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = firstTabRoute, Modifier.padding(innerPadding)) {

            (0 until tabs.length()).forEach {

                val tab = tabs.getJSONObject(it)
                val tabContent = tab.getJSONObject(keyName = "tabContent")

                composable(bottomBarRoute(it)) {
                    AppstentView(
                        viewContent = tabContent,
                        navController = navController,
                        customContentDataProvider = customContentDataProvider,
                        actionHandlerProvider = actionHandlerProvider,
                    )
                }
            }
        }
    }
}

private fun bottomBarRoute(index: Int): String = "__appstent_tab_$index"

private val BottomBarIconSize = 24.dp

@Composable
private fun BottomBarIcon(
    viewContent: JSONObject,
    title: String,
    customContentDataProvider: CustomContentDataProvider?,
    actionHandlerProvider: ActionHandlerProvider?,
) {
    val context = LocalContext.current
    val resolvedSource = resolveImageSource(
        viewContent = viewContent,
        previewMode = ModuleConfigs.inPreviewMode,
        dynamicImageSource = null,
    )

    if (resolvedSource is ResolvedImageSource.System) {
        val imageVector = iconFromOutlinedPack(resolvedSource.name) ?: Icons.Outlined.Warning
        val tint = viewContent
            .optString(keyName = "foregroundColor", fallback = "")
            .takeIf(String::isNotBlank)
            ?.let { resolveAppstentColor(it, context, customContentDataProvider) }
            ?: LocalContentColor.current

        LegacyIcon(
            imageVector = imageVector,
            contentDescription = title,
            modifier = Modifier.size(BottomBarIconSize),
            tint = tint,
        )
        return
    }

    Box(
        modifier = Modifier.size(BottomBarIconSize),
        contentAlignment = Alignment.Center,
    ) {
        AppstentView(
            viewContent = viewContent,
            modifier = Modifier.size(BottomBarIconSize),
            customContentDataProvider = customContentDataProvider,
            actionHandlerProvider = actionHandlerProvider,
        )
    }
}

private fun Modifier.getViewportContainerModifier(
    viewContent: JSONObject,
    context: android.content.Context,
    customContentDataProvider: CustomContentDataProvider?,
): Modifier = getModifier(viewContent, context, customContentDataProvider).fillMaxSize()
