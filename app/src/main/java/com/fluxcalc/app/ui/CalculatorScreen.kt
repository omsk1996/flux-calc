package com.fluxcalc.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.fluxcalc.app.CalcEvent
import com.fluxcalc.app.CalcState
import com.fluxcalc.app.CalculatorViewModel
import com.fluxcalc.app.engine.AngleMode
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(vm: CalculatorViewModel) {
    val state = vm.state
    val time = rememberFluidTime()
    val pulse = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val wipe = remember { Animatable(1f) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSci by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                CalcEvent.Equals -> launch {
                    pulse.animateTo(1f, tween(170))
                    pulse.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 38f))
                }
                CalcEvent.Error -> launch {
                    shake.snapTo(0f)
                    shake.animateTo(0f, spring(dampingRatio = 0.18f, stiffness = 700f), initialVelocity = 1500f)
                }
                CalcEvent.Clear -> launch {
                    wipe.snapTo(0f)
                    wipe.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    val mainRows = remember(vm) { mainKeys(vm) }
    val sciRows = remember(vm, state.second) { sciKeys(vm, state.second) }

    CompositionLocalProvider(LocalFluidTime provides time) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenW = maxWidth
            val screenH = maxHeight
            val landscape = screenW > screenH

            FluidBackground(pulse = { pulse.value })

            if (!landscape) {
                val cell = (screenW - 20.dp) / 4
                val mainHeight = min(cell * 0.94f * 5, screenH * 0.47f)
                val sciHeight = (screenH * 0.175f).coerceIn(124.dp, 160.dp)
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconKey("History", { showHistory = true }) { drawHistoryIcon(it) }
                        Spacer(Modifier.weight(1f))
                        AngleToggle(state.angleMode, vm::toggleAngle)
                        Spacer(Modifier.width(8.dp))
                        val turn by animateFloatAsState(
                            if (showSci) 0f else 180f,
                            spring(dampingRatio = 0.5f, stiffness = 300f),
                            label = "chevron",
                        )
                        IconKey(
                            if (showSci) "Hide scientific keys" else "Show scientific keys",
                            { showSci = !showSci },
                        ) { color -> rotate(turn) { drawChevronIcon(color) } }
                    }
                    Display(
                        state = state,
                        shake = { shake.value },
                        wipe = { wipe.value },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                    AnimatedVisibility(
                        visible = showSci,
                        enter = expandVertically(
                            spring(dampingRatio = 0.72f, stiffness = 300f), expandFrom = Alignment.Top
                        ) + fadeIn(tween(200)),
                        exit = shrinkVertically(
                            spring(dampingRatio = 1f, stiffness = 380f), shrinkTowards = Alignment.Top
                        ) + fadeOut(tween(140)),
                    ) {
                        KeyGrid(sciRows, gap = 8.dp, fontSize = 17.sp, Modifier.fillMaxWidth().height(sciHeight))
                    }
                    KeyGrid(mainRows, gap = 10.dp, fontSize = 28.sp, Modifier.fillMaxWidth().height(mainHeight))
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconKey("History", { showHistory = true }) { drawHistoryIcon(it) }
                        Spacer(Modifier.width(8.dp))
                        AngleToggle(state.angleMode, vm::toggleAngle)
                        Display(
                            state = state,
                            shake = { shake.value },
                            wipe = { wipe.value },
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp),
                            compact = true,
                        )
                    }
                    Row(Modifier.fillMaxWidth().height(screenH * 0.62f)) {
                        KeyGrid(sciRows, gap = 7.dp, fontSize = 16.sp, Modifier.weight(1f).fillMaxHeight())
                        Spacer(Modifier.width(6.dp))
                        KeyGrid(mainRows, gap = 7.dp, fontSize = 20.sp, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }

            HistoryOverlay(
                visible = showHistory,
                items = state.history,
                onPick = {
                    vm.recall(it)
                    showHistory = false
                },
                onClear = vm::clearHistory,
                onDismiss = { showHistory = false },
            )
        }
    }
}

// ---- keys -------------------------------------------------------------------

private enum class KeyIcon { Backspace }

private class KeySpec(
    val label: AnnotatedString,
    val kind: KeyKind,
    val description: String,
    val span: Int = 1,
    val icon: KeyIcon? = null,
    val onClick: () -> Unit,
)

private fun key(
    label: String,
    kind: KeyKind,
    description: String = label,
    span: Int = 1,
    icon: KeyIcon? = null,
    onClick: () -> Unit,
) = KeySpec(AnnotatedString(label), kind, description, span, icon, onClick)

private fun key(label: AnnotatedString, description: String, onClick: () -> Unit) =
    KeySpec(label, KeyKind.Sci, description, onClick = onClick)

private fun raised(base: String, exponent: String) = buildAnnotatedString {
    append(base)
    withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.68.em)) { append(exponent) }
}

private fun lowered(base: String, index: String) = buildAnnotatedString {
    append(base)
    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.68.em)) { append(index) }
}

private fun mainKeys(vm: CalculatorViewModel): List<List<KeySpec>> {
    fun digit(d: String) = key(d, KeyKind.Digit) { vm.digit(d) }
    fun op(symbol: String, name: String) = key(symbol, KeyKind.Operator, name) { vm.binaryOp(symbol) }
    return listOf(
        listOf(
            key("AC", KeyKind.Danger, "Clear") { vm.clear() },
            key("", KeyKind.Utility, "Backspace", icon = KeyIcon.Backspace) { vm.backspace() },
            key("%", KeyKind.Utility, "Percent") { vm.postfix("%") },
            op("÷", "Divide"),
        ),
        listOf(digit("7"), digit("8"), digit("9"), op("×", "Multiply")),
        listOf(digit("4"), digit("5"), digit("6"), op("−", "Minus")),
        listOf(digit("1"), digit("2"), digit("3"), op("+", "Plus")),
        listOf(
            key("0", KeyKind.Digit, span = 2) { vm.digit("0") },
            key(".", KeyKind.Digit, "Decimal point") { vm.dot() },
            key("=", KeyKind.Equals, "Equals") { vm.evaluate() },
        ),
    )
}

private fun sciKeys(vm: CalculatorViewModel, second: Boolean): List<List<KeySpec>> {
    fun sci(label: String, description: String = label, onClick: () -> Unit) =
        key(label, KeyKind.Sci, description, onClick = onClick)

    val toggle = key(
        "2nd",
        if (second) KeyKind.SciActive else KeyKind.Sci,
        if (second) "Second functions, on" else "Second functions, off",
    ) { vm.toggleSecond() }
    val open = sci("(", "Open parenthesis") { vm.value("(") }
    val close = sci(")", "Close parenthesis") { vm.closeParen() }

    return if (!second) {
        listOf(
            listOf(
                toggle,
                sci("sin", "Sine") { vm.value("sin(") },
                sci("cos", "Cosine") { vm.value("cos(") },
                sci("tan", "Tangent") { vm.value("tan(") },
                sci("x!", "Factorial") { vm.postfix("!") },
            ),
            listOf(
                key(raised("x", "2"), "Square") { vm.postfix("^", "2") },
                key(raised("x", "y"), "Power") { vm.binaryOp("^") },
                sci("√", "Square root") { vm.value("√(") },
                sci("ln", "Natural logarithm") { vm.value("ln(") },
                sci("log", "Logarithm base 10") { vm.value("log(") },
            ),
            listOf(
                open,
                close,
                sci("π", "Pi") { vm.value("π") },
                sci("e", "Euler's number") { vm.value("e") },
                sci("Ans", "Last answer") { vm.value("Ans") },
            ),
        )
    } else {
        listOf(
            listOf(
                toggle,
                key(raised("sin", "−1"), "Inverse sine") { vm.value("sin⁻¹(") },
                key(raised("cos", "−1"), "Inverse cosine") { vm.value("cos⁻¹(") },
                key(raised("tan", "−1"), "Inverse tangent") { vm.value("tan⁻¹(") },
                sci("mod", "Modulo") { vm.binaryOp(" mod ") },
            ),
            listOf(
                key(raised("x", "3"), "Cube") { vm.postfix("^", "3") },
                key(raised("x", "−1"), "Reciprocal") { vm.postfix("^", "(", "−", "1", ")") },
                sci("³√", "Cube root") { vm.value("³√(") },
                key(raised("e", "x"), "e to the power") { vm.value("e", "^", "(") },
                key(raised("10", "x"), "Ten to the power") { vm.tenPower() },
            ),
            listOf(
                open,
                close,
                sci("|x|", "Absolute value") { vm.value("abs(") },
                key(lowered("log", "2"), "Logarithm base 2") { vm.value("log₂(") },
                sci("EE", "Times ten to the power") { vm.exponent() },
            ),
        )
    }
}

/**
 * Rows share the height equally; each cell pads itself by half the gap so a
 * double-width key lines up exactly with the two columns above it.
 */
@Composable
private fun KeyGrid(rows: List<List<KeySpec>>, gap: Dp, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Column(modifier) {
        rows.forEachIndexed { r, row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                row.forEachIndexed { c, spec ->
                    FluidKey(
                        kind = spec.kind,
                        description = spec.description,
                        onClick = spec.onClick,
                        modifier = Modifier
                            .weight(spec.span.toFloat())
                            .fillMaxHeight()
                            .padding(gap / 2),
                        enterDelayMs = (r + c) * 24,
                    ) { color ->
                        if (spec.icon == KeyIcon.Backspace) {
                            Canvas(Modifier.size(fontSize.value.dp)) { drawBackspaceIcon(color) }
                        } else {
                            // Labels roll over like a counter when 2nd is toggled.
                            AnimatedContent(
                                targetState = spec.label,
                                transitionSpec = {
                                    (slideInVertically(spring(dampingRatio = 0.6f, stiffness = 380f)) { it / 2 } +
                                        fadeIn(tween(160))) togetherWith
                                        (slideOutVertically(tween(140)) { -it / 2 } + fadeOut(tween(100))) using
                                        SizeTransform(clip = false)
                                },
                                contentAlignment = Alignment.Center,
                                label = "keyLabel",
                            ) { label ->
                                Text(
                                    text = label,
                                    color = color,
                                    fontSize = if (spec.kind == KeyKind.Operator || spec.kind == KeyKind.Equals) {
                                        fontSize * 1.15f
                                    } else {
                                        fontSize
                                    },
                                    fontWeight = if (spec.kind == KeyKind.Digit) FontWeight.Normal else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- top bar ----------------------------------------------------------------

@Composable
private fun IconKey(description: String, onClick: () -> Unit, draw: DrawScope.(Color) -> Unit) {
    FluidKey(KeyKind.Sci, description, onClick, Modifier.size(46.dp)) { color ->
        Canvas(Modifier.size(22.dp)) { draw(color) }
    }
}

/** DEG / RAD switch with a thumb that springs across like a bead of liquid. */
@Composable
private fun AngleToggle(mode: AngleMode, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val segment = 52.dp
    val thumbX by animateDpAsState(
        if (mode == AngleMode.DEG) 0.dp else segment,
        spring(dampingRatio = 0.58f, stiffness = 380f),
        label = "angleThumb",
    )
    Box(
        modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClickLabel = "Switch angle unit",
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onToggle()
            }
            .semantics { stateDescription = if (mode == AngleMode.DEG) "Degrees" else "Radians" }
            .padding(4.dp)
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), 0) }
                .size(segment, 38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Palette.Violet, Palette.Pink)))
        )
        Row {
            AngleMode.entries.forEach { m ->
                val color by animateColorAsState(
                    if (m == mode) Color.White else Palette.TextDim, label = "angleLabel"
                )
                Box(Modifier.size(segment, 38.dp), contentAlignment = Alignment.Center) {
                    Text(m.name, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
