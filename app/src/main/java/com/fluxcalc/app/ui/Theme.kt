package com.fluxcalc.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color

object Palette {
    val BgTop = Color(0xFF0B0F26)
    val BgBottom = Color(0xFF04050C)

    val Cyan = Color(0xFF22D3EE)
    val Violet = Color(0xFF7C5CFF)
    val Pink = Color(0xFFFF4FA3)
    val Coral = Color(0xFFFF7A90)

    val TextPrimary = Color.White
    val TextDim = Color(0x99FFFFFF)

    /** Seamless when mirrored: used for the flowing "=" key. */
    val Accent = listOf(Cyan, Violet, Pink)
}

/**
 * Seconds since the screen appeared. Everything that flows continuously
 * (background, "=" key) reads this in the draw phase, so it never recomposes.
 */
val LocalFluidTime = compositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

@Composable
fun rememberFluidTime(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    return time
}
