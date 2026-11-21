package com.fluxcalc.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin

private class Blob(
    val color: Color,
    val x: Float, val y: Float,       // resting centre, as a fraction of the screen
    val driftX: Float, val driftY: Float,
    val speedX: Float, val speedY: Float,
    val phase: Float,
    val radius: Float,                // fraction of the shorter screen edge
    val alpha: Float,
)

private val blobs = listOf(
    Blob(Color(0xFF6C3BFF), 0.18f, 0.16f, 0.24f, 0.12f, 0.23f, 0.31f, 0.0f, 0.95f, 0.60f),
    Blob(Color(0xFF00C8FF), 0.86f, 0.30f, 0.22f, 0.16f, 0.19f, 0.27f, 1.7f, 0.85f, 0.45f),
    Blob(Color(0xFFFF2E97), 0.72f, 0.80f, 0.26f, 0.14f, 0.29f, 0.21f, 3.1f, 0.90f, 0.40f),
    Blob(Color(0xFF2D5BFF), 0.14f, 0.70f, 0.20f, 0.18f, 0.17f, 0.25f, 4.4f, 1.00f, 0.48f),
    Blob(Color(0xFF00FFB2), 0.50f, 0.48f, 0.32f, 0.22f, 0.13f, 0.18f, 5.6f, 0.60f, 0.16f),
)

/**
 * Slow-drifting pools of colour that blend like ink in water.
 * [pulse] (0..1) swells and brightens them — fired when a result lands.
 */
@Composable
fun FluidBackground(pulse: () -> Float, modifier: Modifier = Modifier) {
    val time = LocalFluidTime.current
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val unit = size.minDimension
                val base = Brush.verticalGradient(listOf(Palette.BgTop, Palette.BgBottom))
                val shade = Brush.verticalGradient(
                    listOf(Color(0x59000000), Color(0x1A000000), Color(0x80000000))
                )
                val brushes = blobs.map {
                    Brush.radialGradient(
                        colors = listOf(it.color, it.color.copy(alpha = 0f)),
                        center = Offset.Zero,
                        radius = unit * it.radius,
                    )
                }
                onDrawBehind {
                    val t = time.value
                    val p = pulse()
                    drawRect(base)
                    blobs.forEachIndexed { i, b ->
                        val cx = size.width * (b.x + b.driftX * sin(t * b.speedX + b.phase))
                        val cy = size.height * (b.y + b.driftY * cos(t * b.speedY + b.phase * 1.3f))
                        val swell = 1f + 0.10f * sin(t * 0.6f + b.phase) + 0.28f * p
                        translate(cx, cy) {
                            scale(swell, swell, pivot = Offset.Zero) {
                                drawCircle(
                                    brush = brushes[i],
                                    radius = unit * b.radius,
                                    center = Offset.Zero,
                                    alpha = (b.alpha * (1f + 0.7f * p)).coerceIn(0f, 1f),
                                    blendMode = BlendMode.Screen,
                                )
                            }
                        }
                    }
                    drawRect(shade) // keeps text legible over the brightest pools
                }
            }
    )
}
