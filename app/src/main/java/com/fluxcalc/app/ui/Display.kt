package com.fluxcalc.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fluxcalc.app.CalcState
import com.fluxcalc.app.engine.Formatter
import kotlin.math.hypot

/**
 * Two lines that trade places: while typing, the expression is large and the
 * live answer sits quietly below; on "=", the answer swells up and the
 * expression settles back.
 */
@Composable
fun Display(
    state: CalcState,
    shake: () -> Float,
    wipe: () -> Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val expression = state.tokens.joinToString("")
    val evaluated = state.evaluated
    val error = state.error
    val resultText = error ?: Formatter.group(state.preview)
    val k = if (compact) 0.68f else 1f

    val expressionTarget = when {
        evaluated -> 24f
        expression.length <= 9 -> 58f
        expression.length <= 13 -> 46f
        expression.length <= 18 -> 36f
        else -> 30f
    } * k
    val resultTarget = when {
        !evaluated -> 26f
        resultText.length <= 9 -> 60f
        resultText.length <= 12 -> 48f
        resultText.length <= 16 -> 38f
        else -> 30f
    } * k

    val expressionSize by animateFloatAsState(
        expressionTarget, spring(dampingRatio = 0.72f, stiffness = 320f), label = "exprSize"
    )
    val resultSize by animateFloatAsState(
        resultTarget, spring(dampingRatio = 0.6f, stiffness = 240f), label = "resultSize"
    )
    val expressionAlpha by animateFloatAsState(
        if (evaluated) 0.6f else if (expression.isEmpty()) 0.35f else 1f, label = "exprAlpha"
    )
    val resultColor by animateColorAsState(
        when {
            error != null -> Palette.Coral
            evaluated -> Palette.TextPrimary
            else -> Palette.TextDim
        },
        label = "resultColor",
    )

    val scroll = rememberScrollState()
    LaunchedEffect(expression) { scroll.animateScrollTo(0) } // 0 = the end, scrolling is reversed

    Box(
        modifier.drawWithContent {
            drawContent()
            val w = wipe()
            if (w < 1f) {
                // A ring of light sweeping out from the AC corner.
                val origin = Offset(0f, size.height)
                val radius = (hypot(size.width, size.height) * w).coerceAtLeast(1f)
                drawCircle(
                    Brush.radialGradient(
                        0.55f to Color.Transparent,
                        0.92f to Palette.Cyan.copy(alpha = 0.55f * (1f - w)),
                        1f to Color.Transparent,
                        center = origin,
                        radius = radius,
                    ),
                    radius = radius,
                    center = origin,
                )
            }
        }
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll, reverseScrolling = true),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    color = Palette.TextPrimary,
                    fontSize = expressionSize.sp,
                    lineHeight = (expressionSize * 1.2f).sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.graphicsLayer { alpha = expressionAlpha },
                )
            }
            AnimatedContent(
                targetState = resultText,
                transitionSpec = {
                    (fadeIn(tween(180)) +
                        slideInVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) { it / 3 }) togetherWith
                        (fadeOut(tween(110)) + slideOutVertically(tween(140)) { -it / 3 }) using
                        SizeTransform(clip = false)
                },
                contentAlignment = Alignment.CenterEnd,
                label = "result",
                modifier = Modifier
                    .graphicsLayer { translationX = shake() }
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .pointerInput(resultText, error) {
                        detectTapGestures(
                            onLongPress = {
                                if (error == null && resultText.isNotEmpty()) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Result", resultText))
                                    if (Build.VERSION.SDK_INT < 33) {
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    },
            ) { text ->
                Text(
                    text = text,
                    color = resultColor,
                    fontSize = resultSize.sp,
                    lineHeight = (resultSize * 1.2f).sp,
                    fontWeight = if (evaluated) FontWeight.Normal else FontWeight.Light,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
