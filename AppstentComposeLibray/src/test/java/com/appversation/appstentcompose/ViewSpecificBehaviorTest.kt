package com.appversation.appstentcompose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.Modifier
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewSpecificBehaviorTest {
    @Test
    fun actionDispatchInvokesLocalAndGlobalHandlersExactlyOnce() {
        val local = RecordingActionHandler()
        val global = RecordingActionHandler()
        val data = JSONObject().put("origin", "test")

        dispatchAppstentAction(
            handlerName = "saveAction",
            data = data,
            localHandler = local,
            globalHandler = global,
        )

        assertEquals(listOf("saveAction"), local.names)
        assertEquals(listOf("saveAction"), global.names)
        assertEquals("test", local.data.single()?.getString("origin"))
        assertEquals("test", global.data.single()?.getString("origin"))
    }

    @Test
    fun customContextExposesDescriptorAndDispatchesRegisteredHandlers() {
        val local = RecordingActionHandler()
        val global = RecordingActionHandler()
        val previousGlobal = ModuleConfigs.globalActionHandlerProvider
        ModuleConfigs.globalActionHandlerProvider = global

        try {
            val descriptor = JSONObject()
                .put("type", "custom")
                .put("customViewName", "accountCard")
                .put("customPayload", JSONObject().put("accountID", "example"))
            val context = ModuleConfigs.CustomContentViewContext(
                viewContent = descriptor,
                viewName = "accountCard",
                modifier = Modifier,
                customContentDataProvider = null,
                actionHandlerProvider = local,
            )

            context.performAction("customAction")

            assertEquals("example", context.viewContent.getJSONObject("customPayload").getString("accountID"))
            assertEquals(listOf("customAction"), local.names)
            assertEquals(listOf("customAction"), global.names)
        } finally {
            ModuleConfigs.globalActionHandlerProvider = previousGlobal
        }
    }

    @Test
    fun accessibilityConfigurationUsesAuthoredValuesAndEnabledDefaults() {
        val configured = resolveAccessibilityConfiguration(
            JSONObject()
                .put("accessibilityLabel", "Account card")
                .put("accessibilityHint", "Opens the account")
                .put("accessibilityIdentifier", "account.card")
                .put("accessibilityRole", "button")
                .put("accessibilityTraits", JSONArray().put("selected"))
                .put("isEnabled", false)
                .put("isDecorative", true),
        )
        val defaults = resolveAccessibilityConfiguration(JSONObject())

        assertEquals("Account card. Opens the account", configured.spokenDescription)
        assertEquals("account.card", configured.identifier)
        assertEquals("button", configured.role)
        assertEquals(listOf("selected"), configured.traits)
        assertFalse(configured.isEnabled)
        assertTrue(configured.isDecorative)
        assertTrue(defaults.isEnabled)
        assertFalse(defaults.isDecorative)
    }

    @Test
    fun previewImageOverridesTheAuthoredSource() {
        val descriptor = JSONObject()
            .put("sourceType", "local")
            .put("source", "authored_asset")
            .put("previewImageSource", "https://example.com/preview.png")

        assertEquals(
            ResolvedImageSource.Remote("https://example.com/preview.png"),
            resolveImageSource(descriptor, previewMode = true, dynamicImageSource = null),
        )
        assertEquals(
            ResolvedImageSource.Local("authored_asset"),
            resolveImageSource(descriptor, previewMode = false, dynamicImageSource = null),
        )
    }

    @Test
    fun dynamicImagesResolveRemoteLocalAndSystemSources() {
        val dynamicDescriptor = JSONObject()
            .put("sourceType", "dynamic")
            .put("source", "avatar")
        val dynamicSystemDescriptor = JSONObject()
            .put("sourceType", "dynamic_system")
            .put("source", "statusIcon")

        assertEquals(
            ResolvedImageSource.Remote("https://example.com/avatar.png"),
            resolveImageSource(
                dynamicDescriptor,
                previewMode = false,
                dynamicImageSource = "https://example.com/avatar.png",
            ),
        )
        assertEquals(
            ResolvedImageSource.Local("profile_avatar"),
            resolveImageSource(
                dynamicDescriptor,
                previewMode = false,
                dynamicImageSource = "profile_avatar",
            ),
        )
        assertEquals(
            ResolvedImageSource.System("CheckCircle"),
            resolveImageSource(
                dynamicSystemDescriptor,
                previewMode = false,
                dynamicImageSource = "CheckCircle",
            ),
        )
    }

    @Test
    fun localImagesKeepNamedAndNumericResourceIdentifiers() {
        val namedDescriptor = JSONObject()
            .put("sourceType", "local")
            .put("source", "brand_mark")
        val numericDescriptor = JSONObject()
            .put("source", "1234")

        assertEquals(
            ResolvedImageSource.Local("brand_mark"),
            resolveImageSource(namedDescriptor, previewMode = false, dynamicImageSource = null),
        )
        assertEquals(
            ResolvedImageSource.Local("1234"),
            resolveImageSource(numericDescriptor, previewMode = false, dynamicImageSource = null),
        )
    }

    @Test
    fun videoPlaybackConfigHonorsAllSchemaFlags() {
        val descriptor = JSONObject()
            .put("autoplay", true)
            .put("isMuted", true)
            .put("autoLoop", true)
            .put("showPlaybackControls", true)

        assertEquals(
            VideoPlaybackConfig(
                autoplay = true,
                isMuted = true,
                autoLoop = true,
                showPlaybackControls = true,
            ),
            resolveVideoPlaybackConfig(descriptor),
        )
    }

    @Test
    fun videoPlaybackConfigUsesNonPlayingDefaults() {
        assertEquals(
            VideoPlaybackConfig(
                autoplay = false,
                isMuted = false,
                autoLoop = false,
                showPlaybackControls = false,
            ),
            resolveVideoPlaybackConfig(JSONObject()),
        )
    }

    @Test
    fun pagerBehaviorHonorsAxisIndexAutorotationAndEffectFields() {
        val descriptor = JSONObject()
            .put("pagerAxis", "vertical")
            .put("pageIndexMode", "hide")
            .put("autorotate", true)
            .put("effects", "3DRotate")
            .put("rotationAngle", 24)

        val behavior = resolvePagerBehavior(descriptor)

        assertEquals(PagerAxis.Vertical, behavior.axis)
        assertEquals(PageIndexMode.Hide, behavior.pageIndexMode)
        assertTrue(behavior.autorotate)
        assertTrue(behavior.usesThreeDimensionalRotation)
        assertEquals(24f, behavior.rotationAngle)
        assertFalse(behavior.showsPageIndex(pageCount = 3))
    }

    @Test
    fun pagerBehaviorUsesHorizontalAutomaticDefaults() {
        val behavior = resolvePagerBehavior(JSONObject())

        assertEquals(PagerAxis.Horizontal, behavior.axis)
        assertEquals(PageIndexMode.Automatic, behavior.pageIndexMode)
        assertFalse(behavior.autorotate)
        assertFalse(behavior.usesThreeDimensionalRotation)
        assertFalse(behavior.showsPageIndex(pageCount = 1))
        assertTrue(behavior.showsPageIndex(pageCount = 2))
    }

    @Test
    fun navigationBarBehaviorHonorsTitleAndVisibility() {
        val visible = resolveNavigationBarBehavior(
            JSONObject()
                .put("barTitle", "Conformance")
                .put("isBarHidden", false),
        )
        val hidden = resolveNavigationBarBehavior(
            JSONObject()
                .put("barTitle", "Hidden")
                .put("isBarHidden", true),
        )

        assertEquals(NavigationBarBehavior("Conformance", isHidden = false), visible)
        assertEquals(NavigationBarBehavior("Hidden", isHidden = true), hidden)
    }

    @Test
    fun navigationBarWithoutAuthoredChromeStaysHidden() {
        assertEquals(
            NavigationBarBehavior(title = "", isHidden = true),
            resolveNavigationBarBehavior(JSONObject()),
        )
    }

    @Test
    fun remoteNavigationDestinationsSupportRuntimeIncludedRoutes() {
        val source = "Showcase/Examples/AppFlows/Launch Home?variant=featured"
        val destination = remoteIncludedDestination(source)
        val encodedSource = destination.substringAfterLast('/')

        assertTrue(destination.startsWith("__appstent_remote_included__/"))
        assertFalse(encodedSource.contains('/'))
        assertEquals(source, decodeRemoteIncludedSource(encodedSource))
        assertEquals(null, decodeRemoteIncludedSource("not valid base64"))
    }

    @Test
    fun sfSymbolAliasesResolveToAndroidSystemIcons() {
        assertEquals(Icons.Outlined.Home, iconFromOutlinedPack("house"))
        assertEquals(Icons.Outlined.QrCodeScanner, iconFromOutlinedPack("qrcode.viewfinder"))
        assertEquals(Icons.Outlined.PersonAdd, iconFromOutlinedPack("person.crop.circle.badge.plus"))
        assertEquals(Icons.Outlined.Tune, iconFromOutlinedPack("slider.horizontal.3"))
        assertEquals(Icons.Outlined.Code, iconFromOutlinedPack("curlybraces.square.fill"))
    }

    @Test
    fun customGridTracksHonorFixedFlexibleAndAdaptiveBounds() {
        val configs = JSONArray()
            .put(JSONObject().put("itemType", "fixed").put("width", 40))
            .put(
                JSONObject()
                    .put("itemType", "flexible")
                    .put("minWidth", 60)
                    .put("maxWidth", 100),
            )
            .put(
                JSONObject()
                    .put("itemType", "adaptive")
                    .put("minWidth", 50)
                    .put("maxWidth", 80),
            )

        val specs = resolveGridTrackSpecs(configs)

        assertEquals(
            listOf(
                GridTrackSpec.Fixed(40.0),
                GridTrackSpec.Flexible(60.0, 100.0),
                GridTrackSpec.Adaptive(50.0, 80.0),
            ),
            specs,
        )
        assertEquals(
            listOf(40, 95, 80),
            calculateGridTrackSizes(specs, availableSize = 240, spacing = 10, density = 1f),
        )
    }

    @Test
    fun customGridTracksRetainMinimumBoundsWhenSpaceIsTight() {
        val specs = listOf(
            GridTrackSpec.Fixed(50.0),
            GridTrackSpec.Flexible(100.0, 120.0),
            GridTrackSpec.Adaptive(90.0, 110.0),
        )

        assertEquals(
            listOf(50, 100, 90),
            calculateGridTrackSizes(specs, availableSize = 180, spacing = 10, density = 1f),
        )
    }

    @Test
    fun adaptiveGridTracksRepeatCellsWithinAuthoredBounds() {
        val sizes = calculateGridTrackSizes(
            trackSpecs = listOf(GridTrackSpec.Adaptive(64.0, 96.0)),
            availableSize = 320,
            spacing = 8,
            density = 1f,
        )

        assertEquals(listOf(74, 74, 74, 74), sizes)
        assertTrue(sizes.all { it in 64..96 })
    }

    @Test
    fun intrinsicVerticalGridUsesFiniteTracksInsideScrollableParents() {
        val descriptor = JSONObject()
            .put("type", "grid")
            .put("numColumns", 2)

        assertEquals(
            listOf(164, 164),
            calculateIntrinsicVerticalGridTrackSizes(
                viewContent = descriptor,
                availableSize = 340,
                spacing = 12,
                density = 1f,
            ),
        )
    }

    @Test
    fun unboundedNestedVerticalStacksShareTheAncestorScrollViewport() {
        val unboundedScrollable = JSONObject().put("scrollable", true)
        val boundedScrollable = JSONObject()
            .put("scrollable", true)
            .put("height", 240)

        assertTrue(
            shouldApplyVerticalScroll(
                viewContent = unboundedScrollable,
                isInsideVerticalScroller = false,
            ),
        )
        assertFalse(
            shouldApplyVerticalScroll(
                viewContent = unboundedScrollable,
                isInsideVerticalScroller = true,
            ),
        )
        assertTrue(
            shouldApplyVerticalScroll(
                viewContent = boundedScrollable,
                isInsideVerticalScroller = true,
            ),
        )
    }

    @Test
    fun listStylesResolveToDistinctNativeInspiredBehaviors() {
        val expectedKinds = mapOf(
            "plainListStyle" to ListStyleKind.Plain,
            "sidebarListStyle" to ListStyleKind.Sidebar,
            "insetListStyle" to ListStyleKind.Inset,
            "groupedListStyle" to ListStyleKind.Grouped,
            "insetGroupedListStyle" to ListStyleKind.InsetGrouped,
        )

        expectedKinds.forEach { (authoredStyle, expectedKind) ->
            assertEquals(
                expectedKind,
                resolveListStyleBehavior(JSONObject().put("listStyle", authoredStyle)).kind,
            )
        }

        assertTrue(resolveListStyleBehavior(JSONObject().put("listStyle", "plainListStyle")).showsDividers)
        assertFalse(resolveListStyleBehavior(JSONObject().put("listStyle", "groupedListStyle")).showsDividers)
        assertEquals(
            ListStyleKind.Grouped,
            resolveListStyleBehavior(JSONObject().put("listStyle", "grouped")).kind,
        )
        assertEquals(
            16,
            resolveListStyleBehavior(JSONObject().put("listStyle", "insetGroupedListStyle")).horizontalInset,
        )
    }

    @Test
    fun includedContentStateDistinguishesLoadedAndFailedRequests() {
        val loaded = resolveIncludedContentState(
            baseContent = JSONObject().put("type", "text").put("text", "Base"),
            wrapperContent = JSONObject()
                .put("type", "included")
                .put("source", "remote")
                .put("text", "Override"),
            error = null,
        )
        val failed = resolveIncludedContentState(
            baseContent = null,
            wrapperContent = JSONObject().put("type", "included"),
            error = IllegalStateException("network unavailable"),
        )

        assertTrue(loaded is IncludedContentState.Loaded)
        assertEquals("text", (loaded as IncludedContentState.Loaded).content.getString("type"))
        assertEquals("Override", loaded.content.getString("text"))
        assertEquals(IncludedContentState.Error, failed)
    }
}

private class RecordingActionHandler : ModuleConfigs.ActionHandlerProvider {
    val names = mutableListOf<String>()
    val data = mutableListOf<JSONObject?>()

    override fun handleTap(handlerName: String, data: JSONObject?) {
        names += handlerName
        this.data += data
    }
}
