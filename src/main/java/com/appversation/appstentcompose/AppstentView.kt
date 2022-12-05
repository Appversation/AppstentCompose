package com.appversation.appstentcompose

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
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


@Composable
fun AppstentView(viewContent: JSONObject, modifier: Modifier = Modifier) {
    AppstentTheme {
        if (viewContent.has("type")) {
            when (viewContent.getString("type")) {
                "spacer"    -> SpacerView(viewContent, modifier)
                "divider"   -> DividerView(viewContent, modifier)
                "gradientView" -> GradientView(viewContent, modifier)
                "text"      -> TextView(viewContent, modifier)
                "image"     -> ImageView(viewContent, modifier)
                "video"     -> VideoView(viewContent, modifier)
                "tabbedView"-> PagerView(viewContent, modifier)
                "hStack"    -> StackView(viewContent = viewContent, direction = Direction.x, modifier)
                "vStack"    -> StackView(viewContent = viewContent, direction = Direction.y, modifier)
                "zStack"    -> StackView(viewContent = viewContent, direction = Direction.z, modifier)
                "included"  -> IncludedView(viewContent, modifier)
                "grid"      -> GridView(viewContent = viewContent, modifier = modifier)
                "list"      -> ListView(viewContent = viewContent, modifier = modifier)

                else -> { }
            }
        }
    }
}

@Composable
fun SpacerView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    if (viewContent.has("minLength")) {
        val minLength = viewContent.getDouble("minLength").toFloat()
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

    val colorJSONArray = viewContent.getJSONArray("colors")
    val colors = arrayListOf<Color>()
    (0 until colorJSONArray.length()).forEach {
        val colorString = colorJSONArray.getString(it)
        colors.add(it, Color(android.graphics.Color.parseColor(colorString)))
    }

    val gradientModifier = modifier.getModifier(viewContent)

    val heightModifier = if (viewContent.has("height")) {
        val viewHeight = viewContent.getInt("height")

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
        modifier = modifier.getModifier(viewContent))
}

@Composable
fun ImageView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    val sourceType = viewContent.getString("sourceType")
    val imageSource = viewContent.getString("source")

    when (sourceType) {
        "remote"    -> AsyncImage(imageSource, null,
            contentScale = ContentScale.FillWidth,
            modifier = modifier
                .getModifier(viewContent)
                .fillMaxWidth()
        )
        "system"    -> Icon(imageSource, viewContent)
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

    return Icon(imageVector = icon, "",
        modifier = modifier
            .getModifier(viewContent)
            .fillMaxWidth())
}

@Composable
fun VideoView(viewContent: JSONObject, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    //val sourceType = viewContent.getString("sourceType")
    val videoUri = viewContent.getString("source")

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

enum class Direction {
    x, y, z
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerView(viewContent: JSONObject, modifier: Modifier = Modifier) {
    val tabs = viewContent.getJSONArray("tabs")

    val pagerState = rememberPagerState()

    Column(modifier = Modifier.fillMaxWidth()) {

        HorizontalPager(count = tabs.length(),
            state = pagerState,
            modifier = modifier.getModifier(viewContent)
        ) { currentPage ->

            val tabContent = tabs.getJSONObject(currentPage)

            AppstentView(viewContent = tabContent.getJSONObject("tabContent"), modifier)
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
fun StackView(viewContent: JSONObject, direction: Direction, modifier: Modifier = Modifier) {

    val views = viewContent.getJSONArray("views")
    val scrollable = viewContent.optBoolean("scrollable", false)

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

            if (viewContent.has("alignment")) {
                val alignmentString = viewContent.getString("alignment")
                alignmentVal = getVerticalAlignment(alignmentString)
            }

            (0 until views.length()).forEach {
                AppstentView(viewContent = views.getJSONObject(it),
                    modifier.align(alignmentVal))
            }
        }

        Direction.y -> {

            var alignmentVal = Alignment.CenterHorizontally

            if (viewContent.has("alignment")) {
                val alignmentString = viewContent.getString("alignment")
                alignmentVal = getHorizontalAlignment(alignmentString)
            }

            Column(modifier = columnModifier) {
                (0 until views.length()).forEach {
                    AppstentView(viewContent = views.getJSONObject(it),
                        modifier.align(alignmentVal))
                }
            }
        }

        Direction.z -> Box(modifier = appstentModifier) {

            var alignmentVal = Alignment.Center

            if (viewContent.has("alignment")) {
                val alignmentString = viewContent.getString("alignment")
                alignmentVal = getAlignment(alignmentString)
            }

            (0 until views.length()).forEach {

                AppstentView(viewContent = views.getJSONObject(it),
                    modifier
                        .align(alignmentVal)
                        .matchParentSize()
                        .padding(5.dp))
            }
        }
    }
}


@Composable
fun IncludedView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    if (viewContent.has("source")) {
        val source = viewContent.getString("source")

        var includedContent by remember { mutableStateOf(JSONObject())}
        val scope = rememberCoroutineScope()

        LaunchedEffect(scope) {
            includedContent = ViewContentRepository().getContent(source)
        }

        AppstentView(viewContent = includedContent, modifier = modifier)
    }
}

@Composable
fun GridView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    val views = viewContent.getJSONArray("views")

    val rowSpacing = viewContent.optInt("rowSpacing", 0)
    val colSpacing = viewContent.optInt("colSpacing", 0)

    val gridCells: GridCells = getGridCells(viewContent)

    val gridType = viewContent.optString("gridType", "vertical")

    if (gridType == "horizontal") {
        val gridModifier: Modifier = modifier.verticalScroll(rememberScrollState())

        LazyHorizontalGrid(rows = gridCells,
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(colSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing.dp)) {

            items(views.length()) {
                AppstentView(viewContent = views.getJSONObject(it), modifier)
            }
        }
    } else {
        LazyVerticalGrid(columns = gridCells,
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(colSpacing.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing.dp)) {

            items(views.length()) {
                AppstentView(viewContent = views.getJSONObject(it), modifier)
            }
        }
    }
}

private fun getGridCells(viewContent: JSONObject): GridCells {

    return if (viewContent.has("minCellWidth")) {

        GridCells.Adaptive(viewContent.getInt("minCellWidth").dp)

    } else if (viewContent.has("numColumns") || viewContent.has("numRows") ) {

        val rowColCount = viewContent.optInt("numColumns", viewContent.optInt("numRows", 1))

        GridCells.Fixed(rowColCount)
    }
    else {

        val gridRowColConfigs: JSONArray = if (viewContent.has("columns"))
            viewContent.getJSONArray("columns")
        else
            viewContent.getJSONArray("rows")

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
                    if (rowColConfig.getString("itemType") == "fixed") {
                        totalFixedSize += (rowColConfig.getInt("width") * density).toInt()
                    } else {
                        totalFlexibleItems += 1
                    }
                }

                return (0 until gridRowColConfigs.length()).map {

                    val rowColConfig = gridRowColConfigs.getJSONObject(it)

                    val size = if (rowColConfig.getString("itemType") == "fixed") {

                        (rowColConfig.getInt("width") * density).toInt()
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
fun ListView(viewContent: JSONObject, modifier: Modifier = Modifier) {

    if (viewContent.has("sections")) {

        val sections = viewContent.getJSONArray("sections")

        LazyColumn(modifier = modifier) {

            (0 until sections.length()).forEach {

                val sectionObject = sections.getJSONObject(it)
                val text = sectionObject.getString("title")
                stickyHeader {
                    Text(text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                val views = sectionObject.getJSONArray("views")

                (0 until views.length()).forEach {

                    item {
                        AppstentView(viewContent = views.getJSONObject(it), modifier)
                    }
                }
            }
        }
    } else {
        val views = viewContent.getJSONArray("views")

        LazyColumn(modifier = modifier) {
            (0 until views.length()).forEach {
                item {
                    AppstentView(viewContent = views.getJSONObject(it), modifier)
                }
            }
        }
    }
}