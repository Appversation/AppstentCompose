package com.appversation.appstentcompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing as ComposeEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * Supply a new value to replay mounted entrance effects while retaining the
 * rendered document and its loaded content.
 */
val LocalAppstentMotionReplayVersion = compositionLocalOf { 0 }

internal data class EntranceMotion(
    val preset: Preset,
    val durationMs: Int,
    val delayMs: Int = 0,
    val direction: Direction = Direction.Bottom,
    val distance: Int = 12,
    val easing: Easing = Easing.EaseOut,
) {
    enum class Preset(val slides: Boolean = false, val fades: Boolean = false) {
        FadeIn(fades = true), SlideIn(slides = true), ScaleIn, FadeSlideIn(slides = true, fades = true)
    }

    enum class Direction(val wireName: String, val x: Int, val y: Int) {
        Top("top", 0, -1), Bottom("bottom", 0, 1), Left("left", -1, 0), Right("right", 1, 0)
    }

    enum class Easing(val wireName: String, val curve: ComposeEasing) {
        Linear("linear", LinearEasing),
        EaseIn("easeIn", CubicBezierEasing(0.42f, 0f, 1f, 1f)),
        EaseOut("easeOut", CubicBezierEasing(0f, 0f, 0.58f, 1f)),
        EaseInOut("easeInOut", CubicBezierEasing(0.42f, 0f, 0.58f, 1f))
    }

    companion object {
        private val keys = setOf("preset", "durationMs", "delayMs", "direction", "distance", "easing")

        fun parse(descriptor: Any?): EntranceMotion? {
            val obj = descriptor as? JSONObject ?: return null
            if (obj.keys().asSequence().any { it !in keys }) return null
            val preset = when (obj.opt("preset")) {
                "fadeIn" -> Preset.FadeIn
                "slideIn" -> Preset.SlideIn
                "scaleIn" -> Preset.ScaleIn
                "fadeSlideIn" -> Preset.FadeSlideIn
                else -> return null
            }
            if (!preset.slides && (obj.has("direction") || obj.has("distance"))) return null
            val duration = obj.integer("durationMs", 250, 1..10_000) ?: return null
            val delay = obj.integer("delayMs", 0, 0..10_000) ?: return null
            val distance = obj.integer("distance", 12, 1..256) ?: return null
            val direction = Direction.entries.firstOrNull { it.wireName == (obj.opt("direction") ?: "bottom") }
                ?: return null
            val easing = Easing.entries.firstOrNull { it.wireName == (obj.opt("easing") ?: "easeOut") }
                ?: return null
            return EntranceMotion(preset, duration, delay, direction, distance, easing)
        }

        private fun JSONObject.integer(key: String, fallback: Int, range: IntRange): Int? {
            if (!has(key)) return fallback
            val value = (opt(key) as? Number)?.toDouble() ?: return null
            val integer = value.toInt()
            return integer.takeIf { it in range && it.toDouble() == value }
        }
    }
}

@Composable
internal fun entranceMotionModifier(type: String, content: JSONObject): Modifier {
    if (type !in entranceMotionTypes) return Modifier

    val nodeId = content.optString(keyName = "nodeId", fallback = "")
    val replayVersion = LocalAppstentMotionReplayVersion.current
    val motion = remember(type, nodeId, replayVersion) {
        EntranceMotion.parse(content.optPlatformValue("motion"))
    } ?: return Modifier

    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val reduceMotion = durationScale?.scaleFactor == 0f
    val progress = remember(type, nodeId, replayVersion) {
        Animatable(if (reduceMotion) 1f else 0f)
    }
    LaunchedEffect(progress, reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else if (progress.value < 1f) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(motion.durationMs, motion.delayMs, motion.easing.curve),
            )
        }
    }

    return Modifier.graphicsLayer {
        val value = if (reduceMotion) 1f else progress.value
        if (motion.preset.fades) {
            // Keep overflow such as shadows when the layer is translucent.
            compositingStrategy = CompositingStrategy.ModulateAlpha
            alpha = value
        }
        if (motion.preset.slides) {
            val remaining = motion.distance.dp.toPx() * (1f - value)
            translationX = motion.direction.x * remaining
            translationY = motion.direction.y * remaining
        }
        if (motion.preset == EntranceMotion.Preset.ScaleIn) {
            scaleX = 0.95f + 0.05f * value
            scaleY = scaleX
        }
    }
}

private val entranceMotionTypes = setOf("text", "image", "hStack", "vStack", "zStack")
