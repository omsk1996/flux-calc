package com.fluxcalc.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxcalc.app.HistoryItem
import com.fluxcalc.app.engine.Formatter
import kotlinx.coroutines.delay

/** A sheet that pours down from the top; tap a line to bring it back. */
@Composable
fun HistoryOverlay(
    visible: Boolean,
    items: List<HistoryItem>,
    onPick: (HistoryItem) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(220))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xA6000000))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
    }

    AnimatedVisibility(
        visible,
        enter = slideInVertically(spring(dampingRatio = 1f, stiffness = 280f)) { -it },
        exit = slideOutVertically(tween(240)) { -it } + fadeOut(tween(240)),
    ) {
        val shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF191D42), Color(0xFF0D1029))))
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                .pointerInput(Unit) { detectTapGestures { } } // swallow taps so they don't reach the scrim
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "History",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (items.isNotEmpty()) {
                    Text(
                        "Clear",
                        color = Palette.Coral,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onClear)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            if (items.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Your calculations will appear here",
                        color = Palette.TextDim,
                        fontSize = 15.sp,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(items) { index, item ->
                        HistoryRow(item, index, onClick = { onPick(item) })
                    }
                }
            }

            // Grab handle: tap or flick up to close.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount -> if (dragAmount < -4f) onDismiss() }
                    }
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, index: Int, onClick: () -> Unit) {
    // The first screenful ripples in one after another.
    val enter = remember { Animatable(if (index < 10) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enter.value < 1f) {
            delay(60L + index * 38L)
            enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 300f))
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                translationY = (1f - enter.value) * -28.dp.toPx()
            }
            .clickable(role = Role.Button, onClickLabel = "Reuse this calculation", onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            item.expression,
            color = Palette.TextDim,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "= " + Formatter.group(item.result),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
