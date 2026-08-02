package com.appversation.appstentcompose

import android.content.res.AssetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import org.json.JSONObject
import java.security.MessageDigest

@PreviewTest
@ConformancePreview
@Composable
fun SchemaParityScreenshot() = renderFixture("schema-parity")

@PreviewTest
@ConformancePreview
@Composable
fun FrameBackgroundOrderScreenshot() = renderFixture("frame-background-order")

@PreviewTest
@ConformancePreview
@Composable
fun TextAlignmentScreenshot() = renderFixture("text-alignment")

@PreviewTest
@ConformancePreview
@Composable
fun StackSpacingScreenshot() = renderFixture("stack-spacing")

@PreviewTest
@ConformancePreview
@Composable
fun RelativeSizingScreenshot() = renderFixture("relative-sizing")

@PreviewTest
@ConformancePreview
@Composable
fun FillModifierSizingScreenshot() = renderFixture("fill-modifier-sizing")

@PreviewTest
@ConformancePreview
@Composable
fun ImageUnframedFitScreenshot() = renderFixture("image-unframed-fit")

@PreviewTest
@ConformancePreview
@Composable
fun ImageBoundedFitFillScreenshot() = renderFixture("image-bounded-fit-fill")

@PreviewTest
@ConformancePreview
@Composable
fun IntrinsicStacksScreenshot() = renderFixture("intrinsic-stacks")

@PreviewTest
@ConformancePreview
@Composable
fun ArrangedStacksFiniteBoundsScreenshot() = renderFixture("arranged-stacks-finite-bounds")

@PreviewTest
@ConformancePreview
@Composable
fun ZStackChildSizingScreenshot() = renderFixture("z-stack-child-sizing")

@PreviewTest
@ConformancePreview
@Composable
fun BoundedVerticalGridScreenshot() = renderFixture("bounded-vertical-grid")

@PreviewTest
@ConformancePreview
@Composable
fun BoundedHorizontalGridScreenshot() = renderFixture("bounded-horizontal-grid")

@PreviewTest
@ConformancePreview
@Composable
fun ListViewportSizingScreenshot() = renderFixture("list-viewport-sizing")

@PreviewTest
@ConformancePreview
@Composable
fun TabsScreenshot() = renderFixture("tabs")

@PreviewTest
@ConformancePreview
@Composable
fun NavigationScreenshot() = renderFixture("navigation")

@Preview(
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    locale = "en-US",
    fontScale = 1f,
)
private annotation class ConformancePreview

@Composable
private fun renderFixture(id: String) {
    val assets = LocalContext.current.assets
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.TopStart,
    ) {
        AppstentView(viewContent = FixtureResources.document(assets, id))
    }
}

private object FixtureResources {
    fun document(assets: AssetManager, id: String): JSONObject {
        val manifest = resourceObject(assets, "manifest.json")
        verifyFixtureLock(assets, manifest)
        val fixtureFiles = buildMap {
            val fixtures = manifest.getJSONArray("fixtures")
            repeat(fixtures.length()) { index ->
                val fixture = fixtures.getJSONObject(index)
                if (fixture.getString("baselinePolicy") != "structural-only") {
                    put(fixture.getString("id"), fixture.getString("file"))
                }
            }
        }
        return resourceObject(
            assets,
            checkNotNull(fixtureFiles[id]) { "Unknown screenshot fixture: $id" },
        )
    }

    private fun verifyFixtureLock(assets: AssetManager, manifest: JSONObject) {
        val fixtureLock = resourceObject(assets, "fixture-lock.json")
        check(fixtureLock.getString("contractVersion") == manifest.getString("contractVersion"))
        check(fixtureLock.getString("algorithm") == "sha256")

        val lockedFiles = fixtureLock.getJSONObject("files")
        val expectedFiles = buildSet {
            add("manifest.json")
            add("capability-matrix.json")
            val fixtures = manifest.getJSONArray("fixtures")
            repeat(fixtures.length()) { index ->
                add(fixtures.getJSONObject(index).getString("file"))
            }
        }
        check(expectedFiles == lockedFiles.keys().asSequence().toSet())
        expectedFiles.forEach { file ->
            check(lockedFiles.getString(file) == sha256(assets, file)) {
                "$file: fixture lock mismatch"
            }
        }
    }

    private fun sha256(assets: AssetManager, file: String): String =
        MessageDigest.getInstance("SHA-256")
        .digest(resourceBytes(assets, file))
        .joinToString("") { "%02x".format(it) }

    private fun resourceObject(assets: AssetManager, file: String): JSONObject =
        JSONObject(resourceBytes(assets, file).toString(Charsets.UTF_8))

    private fun resourceBytes(assets: AssetManager, file: String): ByteArray =
        assets.open("viewcontent-conformance/$file").use { it.readBytes() }
}
