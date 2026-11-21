package com.fluxcalc.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class KeyKind { Digit, Operator, Utility, Danger, Equals, Sci, SciActive }

private class KeyColors(
    val fillTop: Color,
    val fillBottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val content: Color,
)

private fun glass(tint: Color, strength: Float, content: Color) = KeyColors(
    fillTop = tint.copy(alpha = 0.26f * strength),
    fillBottom = tint.copy(alpha = 0.09f * strength),
    borderTop = tint.copy(alpha = 0.55f * strength),
    borderBottom = tint.copy(alpha = 0.06f * strength),
    content = content,
)

private val keyColors = mapOf(
    KeyKind.Digit to glass(Color.White, 0.55f, Color.White),
    KeyKind.Operator to glass(Palette.Cyan, 1f, Color(0xFF9BEDFF)),
    KeyKind.Utility to glass(Palette.Violet, 1.15f, Color(0xFFDDD2FF)),
    KeyKind.Danger to glass(Palette.Coral, 1f, Color(0xFFFFA3B2)),
    KeyKind.Equals to glass(Color.White, 1f, Color.White),
    KeyKind.Sci to glass(Color.White, 0.32f, Color(0xFFD6D0F7)),
    KeyKind.SciActive to KeyColors(
        fillTop = Palette.Violet.copy(alpha = 0.85f),
        fillBottom = Palette.Pink.copy(alpha = 0.55f),
        borderTop = Color.White.copy(alpha = 0.55f),
        borderBottom = Color.White.copy(alpha = 0.08f),
        content = Color.White,
    ),
)

/**
 * A glass key that behaves like a droplet: it pops in with a spring, squashes
 * from a pill towards a squircle while held, and wobbles back on release.
 */
@Composable
fun FluidKey(
    kind: KeyKind,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enterDelayMs: Int = 0,
    content: @Composable BoxScope.(Color) -> Unit,
) {
    val press = remember { Animatable(0f) }
    val entrance = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val time = LocalFluidTime.current
    val click by rememberUpdatedState(onClick)
    val colors = keyColors.getValue(kind)

    LaunchedEffect(Unit) {
        delay(enterDelayMs.toLong())
        entrance.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 260f))
    }

    Box(
        modifier
            .graphicsLayer {
                val e = entrance.value
                val s = (1f - 0.12f * press.value) * (0.55f + 0.45f * e)
                scaleX = s
                scaleY = s
                alpha = e.coerceIn(0f, 1f)
                translationY = (1f - e) * 36.dp.toPx()
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .drawBehind {
                val p = press.value.coerceIn(0f, 1f)
                val minDim = size.minDimension
                val radius = lerp(minDim / 2f, minDim * 0.26f, p)
                val corner = CornerRadius(radius)

                if (kind == KeyKind.Equals) {
                    val glowRadius = size.maxDimension * 0.9f
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Palette.Violet.copy(alpha = 0.50f), Palette.Violet.copy(alpha = 0f)),
                            center = center,
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                    )
                    // Slide a mirrored gradient along its own axis: one full
                    // mirror period (2 lengths) loops seamlessly.
                    val vx = size.width * 1.4f
                    val vy = size.height * 0.6f
                    val f = (time.value * 0.22f) % 2f
                    drawRoundRect(
                        Brush.linearGradient(
                            Palette.Accent,
                            start = Offset(-vx * f, -vy * f),
                            end = Offset(vx * (1f - f), vy * (1f - f)),
                            tileMode = TileMode.Mirror,
                        ),
                        cornerRadius = corner,
                    )
                } else {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(colors.fillTop, colors.fillBottom)),
                        cornerRadius = corner,
                    )
                }

                val sw = 1.dp.toPx()
                drawRoundRect(
                    Brush.verticalGradient(listOf(colors.borderTop, colors.borderBottom)),
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius((radius - sw / 2f).coerceAtLeast(0f)),
                    style = Stroke(sw),
                )
                if (p > 0f) {
                    drawRoundRect(Color.White.copy(alpha = 0.18f * p), cornerRadius = corner)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        scope.launch { press.animateTo(1f, tween(70)) }
                        tryAwaitRelease()
                        scope.launch {
                            // Even the quickest tap gets a visible squash before the wobble.
                            if (press.value < 0.7f) press.animateTo(1f, tween(50))
                            press.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = 380f))
                        }
                    },
                    onTap = { click() },
                )
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick {
                    click()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(colors.content)
    }
}
