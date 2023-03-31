package com.appversation.appstentcompose

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.appversation.appstentcompose.ui.theme.AppstentTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun AppstentView(viewContent: JSONObject, modifier: Modifier = Modifier, navController: NavHostController? = null) {
    AppstentTheme {
        if (viewContent.has(keyName = "type") && isVisible(viewContent)) {

            when (viewContent.getString(keyName = "type")) {
                "spacer"    -> SpacerView(viewContent, modifier)
                "divider"   -> DividerView(viewContent, modifier)
                "gradientView" -> GradientView(viewContent, modifier)
                "text"      -> TextView(viewContent, modifier)
                "image"     -> ImageView(viewContent, modifier)
                "video"     -> VideoView(viewContent, modifier)
                "webView"   -> WebView(viewContent = viewContent, modifier = modifier)
                "tabbedView"-> {
                    val tabStyle = viewContent.getString(keyName = "tabStyle")
                    if (tabStyle == "pageStyle") {
                        PagerView(viewContent, modifier, navController)
                    } else {
                        BottomBar(viewContent, modifier)
                    }
                }
                "hStack"    -> StackView(viewContent = viewContent, direction = Direction.x, modifier, navController)
                "vStack"    -> StackView(viewContent = viewContent, direction = Direction.y, modifier, navController)
                "zStack"    -> StackView(viewContent = viewContent, direction = Direction.z, modifier, navController)
                "included"  -> IncludedView(viewContent.optString("source", ""), modifier, navController)
                "grid"      -> GridView(viewContent = viewContent, modifier = modifier, navController)
                "list"      -> ListView(viewContent = viewContent, modifier = modifier, navController)
                "custom"    -> ModuleConfigs.customContentDataProvider?.CustomComposable(viewContent.getString(keyName = "customViewName"))
                "navigationView" -> NavigationApstentView(viewContent = viewContent, modifier)
                "navigationLink" -> NavigationApstentLink(viewContent = viewContent, modifier, navController)

                else -> { }
            }
        }
    }
}

private fun isVisible(viewContent: JSONObject) : Boolean {

    var isVisible = true

    if (!viewContent.has(keyName = "visibility")) {
        return isVisible
    }

    val visibilityRules = viewContent.getJSONArray(keyName = "visibility")

    (0 until visibilityRules.length()).forEach {
        val visibilityRule = visibilityRules.getJSONObject(it)

        val ruleName = visibilityRule.getString(keyName = "ruleName")
        val ruleValue = visibilityRule.optString(keyName = "ruleValue", "")

        isVisible = isVisible && ModuleConfigs.customContentDataProvider?.visibility(ruleName, ruleValue) ?: true

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

                    val scheduledStartTimeInMinutes = (scheduledStartTimeComponents.first ?: 0) * 60 + (scheduledStartTimeComponents.second ?: 0)
                    val currentTimeComponentsInMinutes = (currentTimeComponents.first ?: 0) * 60 + (currentTimeComponents.second ?: 0)
                    val scheduledEndTimeInMinutes = (scheduledEndTimeComponents.first ?: 0) * 60 + (scheduledEndTimeComponents.second ?: 0)

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

    if (viewContent.has(keyName = "minLength")) {
        val minLength = viewContent.getDouble(keyName = "minLength").toFloat()
        Spacer(modifier = modifier.defaultMinSize(Dp(minLength), Dp(minLength)))
    } else {
        Spacer(modifier = modifier)
    }
}

@Composable
fun DividerView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    Divider(
        modifier = modifier.getModifier(viewContent)
    )
}

@Composable
fun GradientView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    val colorJSONArray = viewContent.getJSONArray(keyName = "colors")
    val colors = arrayListOf<Color>()
    (0 until colorJSONArray.length()).forEach {
        val colorString = colorJSONArray.getString(it)
        colors.add(it, Color(android.graphics.Color.parseColor(colorString)))
    }

    val gradientModifier = modifier.getModifier(viewContent)

    val heightModifier = if (viewContent.has(keyName = "height")) {
        val viewHeight = viewContent.getInt(keyName = "height")

        gradientModifier.height(viewHeight.dp)
    } else {
        gradientModifier.fillMaxHeight()
    }

    Spacer(
        modifier = gradientModifier
            .background(Brush.verticalGradient(colors))
            .fillMaxWidth()
            .then(heightModifier)
    )
}

@Composable
fun TextView(viewContent: JSONObject, modifier: Modifier = Modifier) {
    var textString = ""

    if (viewContent.has(keyName = "text")) {
        textString = viewContent.getString(keyName = "text")
    }

    if (viewContent.has(keyName = "dynamicText")) {
        val dynamicTextFieldName = viewContent.getString(keyName = "dynamicText")
        textString = ModuleConfigs.customContentDataProvider?.getStringFor(dynamicTextFieldName) ?: textString
    }

    //foreground color
    var color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black
    if (viewContent.has(keyName = "foregroundColor")) {
        val fgColor = viewContent.getString(keyName = "foregroundColor")
        color = Color(android.graphics.Color.parseColor(fgColor))
    }

    var textStyle = TextStyle.Default
    if (viewContent.has(keyName = "font")) {
        val fontString = viewContent.getString(keyName = "font")
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
        modifier = modifier.getModifier(viewContent))
}

@Composable
fun ImageView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    val sourceType = viewContent.getString(keyName = "sourceType")
    val imageSource = viewContent.getString(keyName = "source")

    val scalingMode = if (viewContent.optString(keyName = "scalingMode", "") == "scaledToFill") {
        ContentScale.Crop
    } else {
        ContentScale.FillWidth
    }

    when (sourceType) {
        "remote"    -> AsyncImage(imageSource, null,
            contentScale = scalingMode,
            modifier = modifier
                .getModifier(viewContent)
                .fillMaxWidth()
        )
        "system"    -> Icon(imageSource, viewContent)
        "dynamic"   -> ModuleConfigs.customContentDataProvider?.CustomComposable(imageSource)
        else        -> Image(painterResource(id = imageSource.toInt()),null, modifier = modifier.getModifier(viewContent))
    }
}

@Composable
fun Icon(name: String, viewContent: JSONObject, modifier: Modifier = Modifier) {

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

    //foreground color
    var color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black
    if (viewContent.has(keyName = "foregroundColor")) {
        val fgColor = viewContent.getString(keyName = "foregroundColor")
        color = Color(android.graphics.Color.parseColor(fgColor))
    }

    return Icon(imageVector = icon, "",
        modifier = modifier
            .getModifier(viewContent)
            .fillMaxWidth(),
        tint = color)
}

@Composable
fun VideoView(viewContent: JSONObject, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val videoUri = viewContent.getString(keyName = "source")

    val exoPlayer = ExoPlayer.Builder(LocalContext.current)
        .build()
        .also { exoPlayer ->
            val mediaItem = MediaItem.Builder()
                .setUri(videoUri)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }

    DisposableEffect(
        AndroidView(factory = {
            StyledPlayerView(context).apply {
                useController = false
                player = exoPlayer
            }
        })
    ) {
        onDispose { exoPlayer.release() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    val sourceType = viewContent.optString(keyName = "sourceType", "")
    val source = viewContent.getString(keyName = "source")

    var backEnabled by remember { mutableStateOf(false) }
    var webView: WebView? = null

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
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

                if (sourceType == "embedded") {
                    loadData(source, "text/html; charset=UTF-8", null)
                } else {
                    loadUrl(source)
                }

                webView = this
            }
        }, update = {
            webView = it
        })

    BackHandler(enabled = backEnabled) {
        webView?.goBack()
    }
}

enum class Direction {
    x, y, z
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerView(viewContent: JSONObject, modifier: Modifier = Modifier, navController: NavHostController? = null) {
    val tabs = viewContent.getJSONArray(keyName = "tabs")

    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxWidth()) {

        HorizontalPager(count = tabs.length(),
            state = pagerState,
            modifier = modifier.getModifier(viewContent)
        ) { currentPage ->

            val tabContent = tabs.getJSONObject(currentPage)

            AppstentView(viewContent = tabContent.getJSONObject(keyName = "tabContent"), modifier, navController)
        }

        Spacer(modifier = Modifier.padding(4.dp))

        Box(modifier = modifier.align(Alignment.CenterHorizontally)) {
            DotsIndicator(totalDots = tabs.length(),
                selectedIndex = pagerState.currentPage,
                selectedColor =  Color(android.graphics.Color.parseColor("#666666")),
                unSelectedColor = Color(android.graphics.Color.parseColor("#E6E6E6"))
            )
        }
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
fun StackView(viewContent: JSONObject, direction: Direction, modifier: Modifier = Modifier, navController: NavHostController? = null) {

    val views = viewContent.getJSONArray(keyName = "views")
    val scrollable = viewContent.optBoolean(keyName ="scrollable", false)

    val appstentModifier = modifier
        .getModifier(viewContent)
        .fillMaxWidth()
        .wrapContentHeight()

    val columnModifier: Modifier = if (scrollable)
                                        appstentModifier
                                            .verticalScroll(rememberScrollState())
                                    else appstentModifier

    val rowModifier: Modifier = if (scrollable)
                                    appstentModifier
                                        .horizontalScroll(rememberScrollState())
                                else appstentModifier

    when (direction) {
        Direction.x -> Row(modifier = rowModifier) {

            var alignmentVal = Alignment.CenterVertically

            if (viewContent.has(keyName = "alignment")) {
                val alignmentString = viewContent.getString(keyName = "alignment")
                alignmentVal = getVerticalAlignment(alignmentString)
            }

            (0 until views.length()).forEach {
                AppstentView(viewContent = views.getJSONObject(it),
                    modifier.align(alignmentVal),
                    navController)
            }
        }

        Direction.y -> {

            var alignmentVal = Alignment.CenterHorizontally

            if (viewContent.has(keyName = "alignment")) {
                val alignmentString = viewContent.getString(keyName = "alignment")
                alignmentVal = getHorizontalAlignment(alignmentString)
            }

            Column(modifier = columnModifier) {
                (0 until views.length()).forEach {
                    AppstentView(viewContent = views.getJSONObject(it),
                        modifier.align(alignmentVal),
                        navController)
                }
            }
        }

        Direction.z -> {

            var alignmentVal = Alignment.Center

            if (viewContent.has(keyName = "alignment")) {
                val alignmentString = viewContent.getString(keyName = "alignment")
                alignmentVal = getAlignment(alignmentString)
            }

            //FIXME: nested box alignment not working

            Box(modifier = appstentModifier,
                contentAlignment = alignmentVal) {

                (0 until views.length()).forEach {

                    AppstentView(viewContent = views.getJSONObject(it),
                        modifier
                            .align(alignmentVal)
                            .matchParentSize()
                            .padding(5.dp)
                        , navController)
                }
            }
        }
    }
}


@Composable
fun IncludedView(fromSource: String, modifier: Modifier = Modifier, navController: NavHostController? = null) {

    if (fromSource.isNotEmpty()) {

        var includedContent by remember { mutableStateOf(JSONObject())}
        val scope = rememberCoroutineScope()

        LaunchedEffect(scope) {
            includedContent = ViewContentRepository().getContent(fromSource)
        }

        AppstentView(viewContent = includedContent, modifier = modifier, navController)
    }
}

@Composable
fun GridView(viewContent: JSONObject, modifier: Modifier = Modifier, navController: NavHostController? = null) {

    val views = viewContent.getJSONArray(keyName = "views")

    val rowSpacing = viewContent.optInt(keyName = "rowSpacing", 0)
    val colSpacing = viewContent.optInt(keyName = "colSpacing", 0)

    val gridCells: GridCells = getGridCells(viewContent)

    val gridType = viewContent.optString(keyName = "gridType", "vertical")

    if (gridType == "horizontal") {
        val gridModifier: Modifier = modifier.verticalScroll(rememberScrollState())

        LazyHorizontalGrid(rows = gridCells,
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(colSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing.dp)) {

            items(views.length()) {
                AppstentView(viewContent = views.getJSONObject(it), modifier, navController)
            }
        }
    } else {
        LazyVerticalGrid(columns = gridCells,
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(colSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing.dp)) {

            items(views.length()) {
                AppstentView(viewContent = views.getJSONObject(it), modifier, navController)
            }
        }
    }
}

private fun getGridCells(viewContent: JSONObject): GridCells {

    return if (viewContent.has(keyName = "minCellWidth")) {

        GridCells.Adaptive(viewContent.getInt(keyName = "minCellWidth").dp)

    } else if (viewContent.has(keyName = "numColumns") || viewContent.has(keyName = "numRows") ) {

        val rowColCount = viewContent.optInt(keyName = "numColumns", viewContent.optInt(keyName = "numRows", 1))

        GridCells.Fixed(rowColCount)
    }
    else {

        val gridRowColConfigs: JSONArray = if (viewContent.has(keyName = "columns"))
            viewContent.getJSONArray(keyName = "columns")
        else
            viewContent.getJSONArray(keyName = "rows")

        object: GridCells {
            override fun Density.calculateCrossAxisCellSizes(
                availableSize: Int,
                spacing: Int
            ): List<Int> {

                var totalFixedSize = 0
                var totalFlexibleItems = 0

                // calculate totalFixedSize and totalFlexibleItems
                (0 until gridRowColConfigs.length()).forEach {

                    val rowColConfig = gridRowColConfigs.getJSONObject(it)
                    if (rowColConfig.getString(keyName = "itemType") == "fixed") {
                        totalFixedSize += (rowColConfig.getInt(keyName = "width") * density).toInt()
                    } else {
                        totalFlexibleItems += 1
                    }
                }

                return (0 until gridRowColConfigs.length()).map {

                    val rowColConfig = gridRowColConfigs.getJSONObject(it)

                    val size = if (rowColConfig.getString(keyName = "itemType") == "fixed") {

                        (rowColConfig.getInt(keyName = "width") * density).toInt()
                    } else {
                        ((availableSize - spacing) - totalFixedSize) / totalFlexibleItems
                    }

                    return@map size
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListView(viewContent: JSONObject, modifier: Modifier = Modifier, navController: NavHostController? = null) {

    if (viewContent.has(keyName = "sections")) {

        val sections = viewContent.getJSONArray(keyName = "sections")

        LazyColumn(modifier = modifier) {

            (0 until sections.length()).forEach {

                val sectionObject = sections.getJSONObject(it)
                val text = sectionObject.getString(keyName = "title")
                stickyHeader {
                    Text(text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                val views = sectionObject.getJSONArray(keyName = "views")

                (0 until views.length()).forEach {

                    item {
                        AppstentView(viewContent = views.getJSONObject(it), modifier, navController)
                    }
                }
            }
        }
    } else {
        val views = viewContent.getJSONArray(keyName = "views")

        LazyColumn(modifier = modifier) {
            (0 until views.length()).forEach {
                item {
                    AppstentView(viewContent = views.getJSONObject(it), modifier, navController)
                }
            }
        }
    }
}

@Composable
fun NavigationApstentView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    val views = viewContent.getJSONArray(keyName = "views")
    val navTitle = viewContent.optString(keyName = "navLinkDestination", "")

    val navController = rememberNavController()

    if (navTitle.isNotEmpty()) {

        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = navTitle
        ) {

            composable(navTitle) {
                CurrentNavScreenContent(views, modifier, navController)
            }

            //crawl through the view hierarchy to create composable for routes
            navigationComposable(viewContent, modifier, this)
        }
    }
}

@Composable
private fun CurrentNavScreenContent(views: JSONArray, modifier: Modifier = Modifier, navController: NavHostController? = null) {

    //create non navigation views
    (0 until views.length()).forEach {

        val viewContentIt = views.getJSONObject(it)

        AppstentView(viewContent = viewContentIt, modifier, navController)
    }
}

private fun navigationComposable(viewContent: JSONObject, modifier: Modifier = Modifier, navGraphBuilder: NavGraphBuilder) {


    if (!viewContent.has(keyName = "views")) {
        return
    }

    val views = viewContent.getJSONArray(keyName = "views")

    (0 until views.length()).forEach {

        val viewContentIt = views.getJSONObject(it)

        if (viewContentIt.optString(keyName = "type", "") == "navigationLink" &&
            viewContentIt.has(keyName = "route")) {

            val route = viewContentIt.getString(keyName = "route")

            navGraphBuilder.composable(route) {
                IncludedView(fromSource = route, modifier)
            }
        } else if (viewContentIt.has(keyName = "views")) {
            navigationComposable(viewContentIt, modifier, navGraphBuilder)
        }
    }
}

@Composable
fun NavigationApstentLink(viewContent: JSONObject, modifier: Modifier = Modifier, navController: NavHostController? = null) {

    val triggerView = viewContent.getJSONObject(keyName = "triggerView")
    val route = viewContent.getString(keyName = "route")

    val navLinkModifier = modifier
        .clickable {
            navController?.navigate(route)
        }

    AppstentView(viewContent = triggerView, navLinkModifier, navController)
}

@Composable
fun BottomBar(viewContent: JSONObject, modifier: Modifier = Modifier) {

    val tabs = viewContent.getJSONArray(keyName = "tabs")

    if (tabs.length() <= 0) {
        return
    }

    val firstTabTitle = tabs.getJSONObject(0).getString(keyName = "title")
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                (0 until tabs.length()).forEach {

                    val tab = tabs.getJSONObject(it)
                    val title = tab.getString(keyName = "title")
                    val icon = tab.getJSONObject(keyName = "icon")

                    BottomNavigationItem(
                        icon = { AppstentView(viewContent = icon) },
                        label = { Text(title) },
                        selected = currentDestination?.hierarchy?.any { it.route == title } == true,
                        onClick = {
                            navController.navigate(title) {
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
        NavHost(navController, startDestination = firstTabTitle, Modifier.padding(innerPadding)) {

            (0 until tabs.length()).forEach {

                val tab = tabs.getJSONObject(it)
                val title = tab.getString(keyName = "title")
                val tabContent = tab.getJSONObject(keyName = "tabContent")

                composable(title) {
                    AppstentView(viewContent = tabContent, modifier = modifier, navController = navController)
                }
            }
        }
    }
}