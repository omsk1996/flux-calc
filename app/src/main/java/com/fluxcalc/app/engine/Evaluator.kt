package com.fluxcalc.app.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class CalcException(message: String) : Exception(message)

enum class AngleMode { DEG, RAD }

/**
 * Evaluates expressions written with the same glyphs the keypad shows
 * (× ÷ − π √ …). Unclosed parentheses are closed automatically.
 */
object Evaluator {
    fun evaluate(expression: String, mode: AngleMode, ans: Double = 0.0): Double {
        val result = try {
            Parser(expression.replace(" ", ""), mode, ans).parseAll()
        } catch (e: StackOverflowError) {
            throw CalcException("Too complex")
        } catch (e: RuntimeException) {
            throw CalcException("Syntax error")
        }
        if (result.isNaN()) throw CalcException("Undefined")
        if (result.isInfinite()) throw CalcException("Too large")
        return result
    }
}

private class Parser(private val s: String, private val mode: AngleMode, private val ans: Double) {
    private var pos = 0
    private var depth = 0

    private val functions: List<Pair<String, (Double) -> Double>> = listOf(
        "sin⁻¹(" to { x -> fromRadians(asin(x)) },
        "cos⁻¹(" to { x -> fromRadians(acos(x)) },
        "tan⁻¹(" to { x -> fromRadians(atan(x)) },
        "sin(" to ::sine,
        "cos(" to ::cosine,
        "tan(" to ::tangent,
        "ln(" to { x -> if (x <= 0.0) throw CalcException("Undefined") else ln(x) },
        "log₂(" to { x -> if (x <= 0.0) throw CalcException("Undefined") else log2(x) },
        "log(" to { x -> if (x <= 0.0) throw CalcException("Undefined") else log10(x) },
        "³√(" to { x -> cbrt(x) },
        "√(" to { x -> sqrt(x) },
        "abs(" to { x -> abs(x) },
    )

    fun parseAll(): Double {
        if (s.isEmpty()) throw CalcException("Syntax error")
        val value = expression()
        if (pos < s.length) throw CalcException("Syntax error")
        return value
    }

    private fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'

    private fun eat(c: Char): Boolean {
        if (peek() == c) {
            pos++
            return true
        }
        return false
    }

    private fun eat(text: String): Boolean {
        if (s.startsWith(text, pos)) {
            pos += text.length
            return true
        }
        return false
    }

    private fun eatMinus() = eat('−') || eat('-')

    private fun expression(): Double {
        if (++depth > 150) throw CalcException("Too complex")
        var value = term()
        while (true) {
            value = when {
                eat('+') -> value + term()
                eatMinus() -> value - term()
                else -> {
                    depth--
                    return value
                }
            }
        }
    }

    private fun term(): Double {
        var value = unary()
        while (true) {
            value = when {
                eat('×') || eat('*') -> value * unary()
                eat('÷') || eat('/') -> {
                    val divisor = unary()
                    if (divisor == 0.0) throw CalcException("Can't divide by 0")
                    value / divisor
                }
                eat("mod") -> {
                    val divisor = unary()
                    if (divisor == 0.0) throw CalcException("Can't divide by 0")
                    value.mod(divisor)
                }
                startsPrimary() -> value * power() // implicit multiplication: 2π, 3(4), 2sin(…)
                else -> return value
            }
        }
    }

    private fun startsPrimary(): Boolean {
        val c = peek()
        return c.isDigit() || c == '.' || c == '(' || c == '√' || c == '³' ||
            (c.isLetter() && !s.startsWith("mod", pos))
    }

    private fun unary(): Double = when {
        eatMinus() -> -unary()
        eat('+') -> unary()
        else -> power()
    }

    private fun power(): Double {
        val base = postfix()
        return if (eat('^')) base.pow(unary()) else base
    }

    private fun postfix(): Double {
        var value = primary()
        while (true) {
            value = when {
                eat('!') -> factorial(value)
                eat('%') -> value / 100.0
                else -> return value
            }
        }
    }

    private fun primary(): Double {
        val c = peek()
        if (c.isDigit() || c == '.') return number()
        if (eat('(')) {
            val value = expression()
            closeParen()
            return value
        }
        if (eat('π')) return PI
        if (eat("Ans")) return ans
        for ((name, fn) in functions) {
            if (eat(name)) {
                val argument = expression()
                closeParen()
                return fn(argument)
            }
        }
        if (eat('e')) return Math.E
        throw CalcException("Syntax error")
    }

    private fun closeParen() {
        if (pos >= s.length) return // auto-close at the end of input
        if (!eat(')')) throw CalcException("Syntax error")
    }

    private fun number(): Double {
        val start = pos
        while (peek().isDigit()) pos++
        if (peek() == '.') {
            pos++
            while (peek().isDigit()) pos++
        }
        if (peek() == 'E') {
            val mark = pos
            pos++
            if (peek() == '−' || peek() == '-' || peek() == '+') pos++
            if (peek().isDigit()) {
                while (peek().isDigit()) pos++
            } else {
                pos = mark
            }
        }
        return s.substring(start, pos).replace('−', '-').toDoubleOrNull()
            ?: throw CalcException("Syntax error")
    }

    private fun toRadians(x: Double) = if (mode == AngleMode.DEG) Math.toRadians(x) else x

    private fun fromRadians(x: Double) = if (mode == AngleMode.DEG) Math.toDegrees(x) else x

    private fun snap(x: Double) = if (abs(x) < 1e-15) 0.0 else x

    private fun sine(x: Double): Double {
        if (mode == AngleMode.DEG && x % 180.0 == 0.0) return 0.0
        return snap(sin(toRadians(x)))
    }

    private fun cosine(x: Double): Double {
        if (mode == AngleMode.DEG && abs(x % 180.0) == 90.0) return 0.0
        return snap(cos(toRadians(x)))
    }

    private fun tangent(x: Double): Double {
        if (mode == AngleMode.DEG) {
            if (x % 180.0 == 0.0) return 0.0
            if (abs(x % 180.0) == 90.0) throw CalcException("Undefined")
        }
        val r = toRadians(x)
        if (abs(cos(r)) < 1e-15) throw CalcException("Undefined")
        return snap(sin(r) / cos(r))
    }

    private fun factorial(x: Double): Double {
        if (x < 0.0) throw CalcException("Undefined")
        if (x != floor(x)) return gamma(x + 1.0)
        if (x > 170.0) throw CalcException("Too large")
        var result = 1.0
        for (i in 2..x.toInt()) result *= i
        return result
    }

    private fun gamma(z: Double): Double {
        if (z < 0.5) return PI / (sin(PI * z) * gamma(1.0 - z))
        val x = z - 1.0
        var a = LANCZOS[0]
        for (i in 1 until LANCZOS.size) a += LANCZOS[i] / (x + i)
        val t = x + 7.5
        return sqrt(2.0 * PI) * t.pow(x + 0.5) * exp(-t) * a
    }

    private companion object {
        val LANCZOS = doubleArrayOf(
            0.99999999999980993,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7,
        )
    }
}

object Formatter {
    private val SCI_HIGH = BigDecimal("1E12")
    private val SCI_LOW = BigDecimal("1E-7")

    /** 12 significant digits, plain notation where readable, `E` notation otherwise. */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        val rounded = BigDecimal(value).round(MathContext(12, RoundingMode.HALF_UP))
        val magnitude = rounded.abs()
        val text = if (magnitude >= SCI_HIGH || magnitude < SCI_LOW) {
            val stripped = rounded.stripTrailingZeros()
            val digits = stripped.unscaledValue().abs().toString()
            val exponent = digits.length - 1 - stripped.scale()
            val mantissa = if (digits.length == 1) digits else digits[0] + "." + digits.substring(1)
            (if (rounded.signum() < 0) "-" else "") + mantissa + "E" + exponent
        } else {
            rounded.stripTrailingZeros().toPlainString()
        }
        return text.replace('-', '−')
    }

    /** Adds thousands separators to the integer part, for display only. */
    fun group(text: String): String {
        if (text.isEmpty() || 'E' in text) return text
        val negative = text.startsWith("−")
        val body = if (negative) text.substring(1) else text
        val dot = body.indexOf('.')
        val whole = if (dot >= 0) body.substring(0, dot) else body
        val fraction = if (dot >= 0) body.substring(dot) else ""
        if (whole.isEmpty() || !whole.all { it.isDigit() }) return text
        val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
        return (if (negative) "−" else "") + grouped + fraction
    }
}
