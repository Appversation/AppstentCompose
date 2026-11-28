package com.appversation.appstentcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsPropertiesAndroid
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3InteractionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun resetProviders() {
        ModuleConfigs.customContentViewProvider = null
        ModuleConfigs.globalActionHandlerProvider = null
    }

    @Test
    fun entranceMotionMovesPaintWhileKeepingSiblingLayoutFixed() {
        val initial = JSONObject(
            """{"type":"text","nodeId":"motion","text":"Moving","motion":{"preset":"slideIn","durationMs":400}}""",
        )
        val content = mutableStateOf(initial)
        val replayVersion = mutableStateOf(0)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalAppstentMotionReplayVersion provides replayVersion.value) {
                Column {
                    AppstentView(content.value, modifier = Modifier.testTag("moving"))
                    Text("Static", modifier = Modifier.testTag("sibling"))
                }
            }
        }

        val moving = composeRule.onNodeWithTag("moving")
        val sibling = composeRule.onNodeWithTag("sibling")
        val start = moving.getUnclippedBoundsInRoot()
        val siblingBounds = sibling.getUnclippedBoundsInRoot()
        composeRule.mainClock.advanceTimeBy(200)
        val middle = moving.getUnclippedBoundsInRoot()
        composeRule.mainClock.advanceTimeBy(300)
        val end = moving.getUnclippedBoundsInRoot()

        assertEquals(12f, (start.top - end.top).value, 0.6f)
        assertTrue(middle.top < start.top && middle.top > end.top)
        assertEquals(siblingBounds, sibling.getUnclippedBoundsInRoot())

        composeRule.runOnIdle {
            content.value = JSONObject(initial.toString()).put("text", "Edited")
        }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(end.top, moving.getUnclippedBoundsInRoot().top)

        repeat(2) {
            composeRule.runOnIdle { replayVersion.value++ }
            composeRule.mainClock.advanceTimeByFrame()
            assertEquals(start.top, moving.getUnclippedBoundsInRoot().top)
            assertEquals(siblingBounds, sibling.getUnclippedBoundsInRoot())
            composeRule.mainClock.advanceTimeBy(500)
            assertEquals(end.top, moving.getUnclippedBoundsInRoot().top)
        }
    }

    @Test
    fun fadeAndSlideHoldsDuringDelayAndReplaysEditedMotionInRtl() {
        val initial = JSONObject(
            """{"type":"text","nodeId":"motion","text":"Moving","motion":{"preset":"fadeSlideIn","delayMs":600,"durationMs":1200,"direction":"left","distance":48,"easing":"linear"}}""",
        )
        val content = mutableStateOf(initial)
        val replayVersion = mutableStateOf(0)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAppstentMotionReplayVersion provides replayVersion.value,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Column {
                    AppstentView(content.value, modifier = Modifier.testTag("moving"))
                    Text("Static", modifier = Modifier.testTag("sibling"))
                }
            }
        }

        val moving = composeRule.onNodeWithTag("moving")
        val sibling = composeRule.onNodeWithTag("sibling")
        val start = moving.getUnclippedBoundsInRoot()
        val siblingBounds = sibling.getUnclippedBoundsInRoot()
        composeRule.mainClock.advanceTimeBy(400)
        assertEquals(start, moving.getUnclippedBoundsInRoot())
        composeRule.mainClock.advanceTimeBy(800)
        val middle = moving.getUnclippedBoundsInRoot()
        composeRule.mainClock.advanceTimeBy(650)
        val end = moving.getUnclippedBoundsInRoot()

        assertEquals(-48f, (start.left - end.left).value, 0.6f)
        assertEquals(-24f, (middle.left - end.left).value, 2f)
        assertEquals(siblingBounds, sibling.getUnclippedBoundsInRoot())

        composeRule.runOnIdle {
            content.value = JSONObject(initial.toString()).apply {
                getJSONObject("motion").put("direction", "right").put("distance", 32)
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(end, moving.getUnclippedBoundsInRoot())

        composeRule.runOnIdle { replayVersion.value++ }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(32f, (moving.getUnclippedBoundsInRoot().left - end.left).value, 0.6f)
        composeRule.mainClock.advanceTimeBy(400)
        assertEquals(32f, (moving.getUnclippedBoundsInRoot().left - end.left).value, 0.6f)
        composeRule.mainClock.advanceTimeBy(1450)
        assertEquals(end, moving.getUnclippedBoundsInRoot())
        assertEquals(siblingBounds, sibling.getUnclippedBoundsInRoot())
    }

    @Test
    fun fadeAndSlideMovesTheEntirePaintedRow() {
        val content = JSONObject(
            """
            {"type":"vStack","padding":24,"spacing":12,"views":[
                {"type":"text","text":"Moving row","backgroundColor":"#0044FF","motion":{
                    "preset":"fadeSlideIn","delayMs":600,"durationMs":1200,
                    "direction":"left","distance":48,"easing":"linear"
                }},
                {"type":"text","text":"Static sibling"}
            ]}
            """.trimIndent(),
        )
        val replayVersion = mutableStateOf(0)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Box(
                Modifier
                    .size(width = 280.dp, height = 180.dp)
                    .testTag("motion-canvas")
                    .background(Color.White)
                    .padding(start = 64.dp, top = 24.dp),
            ) {
                CompositionLocalProvider(LocalAppstentMotionReplayVersion provides replayVersion.value) {
                    AppstentView(content)
                }
            }
        }

        fun paintedHorizontalBounds(): IntRange {
            val pixels = composeRule.onNodeWithTag("motion-canvas").captureToImage().toPixelMap()
            var left = pixels.width
            var right = -1
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) {
                    val color = pixels[x, y]
                    if (color.blue - color.red > 0.15f && color.blue - color.green > 0.1f) {
                        left = minOf(left, x)
                        right = maxOf(right, x)
                    }
                }
            }
            assertTrue("The colored row should be visible", left <= right)
            return left..right
        }

        composeRule.mainClock.advanceTimeBy(1900)
        val settled = paintedHorizontalBounds()
        composeRule.runOnIdle { replayVersion.value++ }
        composeRule.mainClock.advanceTimeBy(1200)
        val moving = paintedHorizontalBounds()

        assertTrue("The row should move from the left", moving.first < settled.first)
        assertTrue("The whole painted row should translate, moving=$moving settled=$settled",
            kotlin.math.abs((settled.last - settled.first) - (moving.last - moving.first)) <= 2)
    }

    @Test
    fun authoredLinkExposesLinkRoleAndDispatchesBothHandlersOnce() {
        val local = RecordingUiActionHandler()
        val global = RecordingUiActionHandler()
        val uriHandler = RecordingUiUriHandler()
        ModuleConfigs.globalActionHandlerProvider = global
        val content = JSONObject()
            .put("type", "link")
            .put("text", "Documentation")
            .put("url", "https://appstent.com/docs")
            .put("tapHandlerName", "linkAction")
            .put("accessibilityIdentifier", "documentation.link")
            .put("accessibilityRole", "link")

        composeRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                AppstentView(content, actionHandlerProvider = local)
            }
        }

        composeRule.onNodeWithTag("documentation.link")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsPropertiesAndroid.AccessibilityClassName,
                    "android.widget.TextView",
                ),
            )
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("linkAction"), local.names)
            assertEquals(listOf("linkAction"), global.names)
            assertEquals(listOf("https://appstent.com/docs"), uriHandler.uris)
        }
    }

    @Test
    fun navigationLinkDispatchesOnceAndOpensItsCustomDestination() {
        val local = RecordingUiActionHandler()
        val global = RecordingUiActionHandler()
        val provider = RecordingUiCustomViewProvider()
        ModuleConfigs.globalActionHandlerProvider = global
        ModuleConfigs.customContentViewProvider = provider
        val navigationLink = JSONObject()
            .put("type", "navigationLink")
            .put("routeType", "custom")
            .put("customViewName", "profile")
            .put("tapHandlerName", "navigationAction")
            .put("accessibilityIdentifier", "navigation.profile")
            .put("accessibilityRole", "link")
            .put(
                "triggerView",
                JSONObject()
                    .put("type", "text")
                    .put("text", "Profile"),
            )
        val content = JSONObject()
            .put("type", "navigationView")
            .put("barHidden", true)
            .put("toolbarViews", JSONArray())
            .put("views", JSONArray().put(navigationLink))

        composeRule.setContent {
            AppstentView(content, actionHandlerProvider = local)
        }

        composeRule.onNodeWithTag("navigation.profile", useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsPropertiesAndroid.AccessibilityClassName,
                    "android.widget.TextView",
                ),
            )
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("profile destination").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(listOf("navigationAction"), local.names)
            assertEquals(listOf("navigationAction"), global.names)
            assertEquals("profile", provider.lastContext?.viewName)
        }
    }

    @Test
    fun menuActionUsesOneNativeMenuClick() {
        val local = RecordingUiActionHandler()
        val global = RecordingUiActionHandler()
        ModuleConfigs.globalActionHandlerProvider = global
        val content = JSONObject()
            .put("type", "menu")
            .put("title", "Actions")
            .put(
                "views",
                JSONArray().put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", "Save")
                        .put("tapHandlerName", "saveAction"),
                ),
            )

        composeRule.setContent {
            AppstentView(content, actionHandlerProvider = local)
        }

        composeRule.onNodeWithText("Actions").performClick()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("saveAction"), local.names)
            assertEquals(listOf("saveAction"), global.names)
        }
    }

    @Test
    fun disclosureLabelActionFiresAndTheDisclosureStillExpands() {
        val local = RecordingUiActionHandler()
        val content = JSONObject()
            .put("type", "disclosureGroup")
            .put("isExpanded", false)
            .put(
                "label",
                JSONArray().put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", "Details")
                        .put("tapHandlerName", "disclosureLabelAction"),
                ),
            )
            .put(
                "content",
                JSONArray().put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", "Additional details"),
                ),
            )

        composeRule.setContent {
            AppstentView(content, actionHandlerProvider = local)
        }

        composeRule.onAllNodesWithText("Additional details").assertCountEquals(0)
        composeRule.onNodeWithText("Details", useUnmergedTree = true)
            .performTouchInput { click() }
        composeRule.onNodeWithText("Additional details").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(listOf("disclosureLabelAction"), local.names)
        }
    }

    @Test
    fun childTapPropagatesToEachAuthoredAncestorExactlyOnce() {
        val local = RecordingUiActionHandler()
        val content = JSONObject()
            .put("type", "vStack")
            .put("tapHandlerName", "parentAction")
            .put(
                "views",
                JSONArray()
                    .put(JSONObject().put("type", "text").put("text", "Parent action"))
                    .put(
                        JSONObject()
                            .put("type", "text")
                            .put("text", "Child action")
                            .put("tapHandlerName", "childAction"),
                    ),
            )

        composeRule.setContent {
            AppstentView(content, actionHandlerProvider = local)
        }

        composeRule.onNodeWithText("Child action", useUnmergedTree = true)
            .performTouchInput { click() }

        composeRule.runOnIdle {
            assertEquals(listOf("childAction", "parentAction"), local.names.sorted())
            assertEquals(2, local.names.size)
        }
    }

    @Test
    fun descriptorAwareCustomViewRetainsStylingAndAuthoredAction() {
        val local = RecordingUiActionHandler()
        val provider = RecordingUiCustomViewProvider()
        ModuleConfigs.customContentViewProvider = provider
        val content = JSONObject()
            .put("type", "custom")
            .put("customViewName", "phase3Card")
            .put("customPayload", JSONObject().put("accountID", "example"))
            .put("tapHandlerName", "customAction")
            .put("accessibilityIdentifier", "custom.account")

        composeRule.setContent {
            AppstentView(content, actionHandlerProvider = local)
        }

        composeRule.onNodeWithTag("custom.account").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("customAction"), local.names)
            assertNotNull(provider.lastContext)
            assertEquals(
                "example",
                provider.lastContext?.viewContent
                    ?.getJSONObject("customPayload")
                    ?.getString("accountID"),
            )
        }
    }
}

private class RecordingUiActionHandler : ModuleConfigs.ActionHandlerProvider {
    val names = mutableListOf<String>()

    override fun handleTap(handlerName: String, data: JSONObject?) {
        names += handlerName
    }
}

private class RecordingUiUriHandler : UriHandler {
    val uris = mutableListOf<String>()

    override fun openUri(uri: String) {
        uris += uri
    }
}

private class RecordingUiCustomViewProvider : ModuleConfigs.CustomContentViewProvider {
    var lastContext: ModuleConfigs.CustomContentViewContext? = null

    @Composable
    override fun CustomComposable(viewName: String) {
        Text("$viewName destination")
    }

    @Composable
    override fun CustomComposable(context: ModuleConfigs.CustomContentViewContext) {
        lastContext = context
        Column(modifier = context.modifier) {
            Text("${context.viewName} destination")
            Text(
                context.viewContent
                    .optJSONObject("customPayload")
                    ?.optString("accountID")
                    .orEmpty(),
            )
        }
    }

    override fun getFontFamilyFrom(name: String): FontFamily = FontFamily.Default

    override fun visibility(ruleName: String, ruleValue: String): Boolean = true
}
