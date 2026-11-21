package com.fluxcalc.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.fluxcalc.app.engine.AngleMode
import com.fluxcalc.app.engine.CalcException
import com.fluxcalc.app.engine.Evaluator
import com.fluxcalc.app.engine.Formatter
import kotlinx.coroutines.flow.MutableSharedFlow

data class HistoryItem(val tokens: List<String>, val result: String) {
    val expression: String get() = tokens.joinToString("")
}

data class CalcState(
    val tokens: List<String> = emptyList(),
    /** Live preview while typing; the final answer once [evaluated]. */
    val preview: String = "",
    val evaluated: Boolean = false,
    val error: String? = null,
    val angleMode: AngleMode = AngleMode.DEG,
    val second: Boolean = false,
    val history: List<HistoryItem> = emptyList(),
)

enum class CalcEvent { Equals, Error, Clear }

class CalculatorViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("flux_calc", Context.MODE_PRIVATE)

    var state by mutableStateOf(
        CalcState(
            angleMode = if (prefs.getString(KEY_ANGLE, "DEG") == "RAD") AngleMode.RAD else AngleMode.DEG,
            history = loadHistory(),
        )
    )
        private set

    /** One-shot signals the UI turns into animations. */
    val events = MutableSharedFlow<CalcEvent>(extraBufferCapacity = 16)

    private var ans = 0.0
    private var hasAns = false

    // ---- input ---------------------------------------------------------------

    fun digit(d: String) = update(freshTokens() + d)

    fun dot() {
        val t = freshTokens()
        var i = t.size - 1
        while (i >= 0 && isDigit(t[i])) i--
        if (i >= 0 && (t[i] == "." || inExponent(t, i))) return
        update(if (t.isNotEmpty() && isDigit(t.last())) t + "." else t + listOf("0", "."))
    }

    /** Binary operators: + − × ÷ ^ mod. */
    fun binaryOp(op: String) {
        var t = if (state.evaluated) listOf("Ans") else state.tokens
        if (t.isEmpty()) {
            if (op == "−") {
                update(listOf("−"))
                return
            }
            t = listOf(if (hasAns) "Ans" else "0")
        }
        val last = t.last()
        when {
            last == "E" -> if (op == "−") update(t + op)
            last.endsWith("(") -> if (op == "−") update(t + op)
            last in BINARY -> {
                if (op == "−" && last in SIGN_FRIENDLY) {
                    update(t + op) // negative operand, e.g. 5×−2
                    return
                }
                var base = t.dropLast(1)
                if (last == "−" && base.isNotEmpty() && base.last() in SIGN_FRIENDLY) base = base.dropLast(1)
                when {
                    base.isEmpty() || base.last().endsWith("(") -> if (op == "−") update(base + op)
                    else -> update(base + op)
                }
            }
            else -> update(t + op)
        }
    }

    /** Things that attach to the value before them: !, %, ^2 … */
    fun postfix(vararg tokens: String) {
        var t = if (state.evaluated) listOf("Ans") else state.tokens
        if (t.isEmpty()) t = listOf(if (hasAns) "Ans" else "0")
        val last = t.last()
        if (last in BINARY || last.endsWith("(") || last == "E" || last == ".") return
        update(t + tokens)
    }

    /** Things that start a value: functions, constants, "(". */
    fun value(vararg tokens: String) = update(freshTokens() + tokens)

    fun tenPower() {
        val t = freshTokens()
        val needsTimes = t.isNotEmpty() && (isDigit(t.last()) || t.last() == ".")
        update(t + (if (needsTimes) listOf("×") else emptyList()) + listOf("1", "0", "^", "("))
    }

    fun exponent() {
        val t = state.tokens
        if (state.evaluated || t.isEmpty() || !isDigit(t.last())) return
        var i = t.size - 1
        while (i >= 0 && (isDigit(t[i]) || t[i] == ".")) i--
        if (i >= 0 && inExponent(t, i)) return
        update(t + "E")
    }

    fun closeParen() {
        val t = state.tokens
        if (state.evaluated || t.isEmpty()) return
        val last = t.last()
        if (openParens(t) <= 0 || last.endsWith("(") || last in BINARY) return
        update(t + ")")
    }

    fun backspace() {
        if (state.evaluated) {
            update(state.tokens) // step back into editing the expression
        } else if (state.tokens.isNotEmpty()) {
            update(state.tokens.dropLast(1))
        }
    }

    fun clear() {
        state = state.copy(tokens = emptyList(), preview = "", evaluated = false, error = null)
        events.tryEmit(CalcEvent.Clear)
    }

    fun evaluate() {
        val t = state.tokens
        if (state.evaluated || t.isEmpty()) return
        try {
            val value = Evaluator.evaluate(t.joinToString(""), state.angleMode, ans)
            val text = Formatter.format(value)
            val closed = t + List(openParens(t).coerceAtLeast(0)) { ")" }
            ans = value
            hasAns = true
            val history = (listOf(HistoryItem(closed, text)) + state.history).take(MAX_HISTORY)
            state = state.copy(tokens = closed, preview = text, evaluated = true, error = null, history = history)
            saveHistory(history)
            events.tryEmit(CalcEvent.Equals)
        } catch (e: CalcException) {
            state = state.copy(error = e.message ?: "Error")
            events.tryEmit(CalcEvent.Error)
        }
    }

    // ---- modes & history -----------------------------------------------------

    fun toggleSecond() {
        state = state.copy(second = !state.second)
    }

    fun toggleAngle() {
        val mode = if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        prefs.edit().putString(KEY_ANGLE, mode.name).apply()
        state = if (state.evaluated) {
            state.copy(angleMode = mode)
        } else {
            state.copy(angleMode = mode, preview = previewOf(state.tokens, mode), error = null)
        }
    }

    fun recall(item: HistoryItem) = update(item.tokens)

    fun clearHistory() {
        state = state.copy(history = emptyList())
        saveHistory(emptyList())
    }

    // ---- internals -----------------------------------------------------------

    private fun freshTokens(): List<String> = if (state.evaluated) emptyList() else state.tokens

    private fun update(tokens: List<String>) {
        state = state.copy(
            tokens = tokens,
            preview = previewOf(tokens, state.angleMode),
            evaluated = false,
            error = null,
        )
    }

    private fun previewOf(tokens: List<String>, mode: AngleMode): String {
        if (tokens.all { isDigit(it) || it == "." }) return ""
        return try {
            Formatter.format(Evaluator.evaluate(tokens.joinToString(""), mode, ans))
        } catch (e: CalcException) {
            ""
        }
    }

    private fun isDigit(token: String) = token.length == 1 && token[0] in '0'..'9'

    /** True when index [i] sits on the `E` (or `E−`) of a number being typed. */
    private fun inExponent(t: List<String>, i: Int) =
        t[i] == "E" || (t[i] == "−" && i > 0 && t[i - 1] == "E")

    private fun openParens(t: List<String>) = t.count { it.endsWith("(") } - t.count { it == ")" }

    private fun loadHistory(): List<HistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split(RESULT_SEP)
            if (parts.size != 2 || parts[0].isEmpty()) null
            else HistoryItem(parts[0].split(TOKEN_SEP), parts[1])
        }
    }

    private fun saveHistory(items: List<HistoryItem>) {
        val raw = items.joinToString("\n") { it.tokens.joinToString(TOKEN_SEP) + RESULT_SEP + it.result }
        prefs.edit().putString(KEY_HISTORY, raw).apply()
    }

    private companion object {
        const val KEY_HISTORY = "history"
        const val KEY_ANGLE = "angle"
        const val TOKEN_SEP = "\u0001"
        const val RESULT_SEP = "\u0002"
        const val MAX_HISTORY = 60
        val BINARY = setOf("+", "−", "×", "÷", "^", " mod ")
        val SIGN_FRIENDLY = setOf("×", "÷", "^", " mod ")
    }
}
