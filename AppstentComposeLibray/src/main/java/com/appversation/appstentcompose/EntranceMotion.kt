package com.appversation.appstentcompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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

internal data class EntranceMotion(val preset: Preset, val durationMs: Int) {
    enum class Preset { FadeIn, SlideIn, ScaleIn }

    companion object {
        fun parse(descriptor: Any?): EntranceMotion? {
            val obj = descriptor as? JSONObject ?: return null
            if (obj.keys().asSequence().any { it != "preset" && it != "durationMs" }) return null
            val preset = when (obj.opt("preset")) {
                "fadeIn" -> Preset.FadeIn
                "slideIn" -> Preset.SlideIn
                "scaleIn" -> Preset.ScaleIn
                else -> return null
            }
            val duration = if (obj.has("durationMs")) {
                (obj.opt("durationMs") as? Number)?.toDouble() ?: return null
            } else {
                250.0
            }
            if (duration !in 1.0..1000.0 || duration != duration.toInt().toDouble()) return null
            return EntranceMotion(preset, duration.toInt())
        }
    }
}

@Composable
internal fun entranceMotionModifier(type: String, content: JSONObject): Modifier {
    if (type !in entranceMotionTypes) return Modifier

    val nodeId = content.optString(keyName = "nodeId", fallback = "")
    val motion = remember(type, nodeId) {
        EntranceMotion.parse(content.optPlatformValue("motion"))
    } ?: return Modifier

    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val replayVersion = LocalAppstentMotionReplayVersion.current
    val progress = remember(type, nodeId, replayVersion) {
        Animatable(if (durationScale?.scaleFactor == 0f) 1f else 0f)
    }
    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(motion.durationMs, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)),
        )
    }

    return Modifier.graphicsLayer {
        val value = if (durationScale?.scaleFactor == 0f) 1f else progress.value
        when (motion.preset) {
            EntranceMotion.Preset.FadeIn -> {
                // Keep overflow such as shadows when the layer is translucent.
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = value
            }
            EntranceMotion.Preset.SlideIn -> translationY = 12.dp.toPx() * (1f - value)
            EntranceMotion.Preset.ScaleIn -> {
                scaleX = 0.95f + 0.05f * value
                scaleY = scaleX
            }
        }
    }
}

private val entranceMotionTypes = setOf("text", "image", "hStack", "vStack", "zStack")
