package com.fluxcalc.app

import com.fluxcalc.app.engine.AngleMode
import com.fluxcalc.app.engine.CalcException
import com.fluxcalc.app.engine.Evaluator
import com.fluxcalc.app.engine.Formatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EvaluatorTest {
    private fun deg(expr: String) = Formatter.format(Evaluator.evaluate(expr, AngleMode.DEG))
    private fun rad(expr: String) = Formatter.format(Evaluator.evaluate(expr, AngleMode.RAD))

    @Test
    fun precedenceAndParentheses() {
        assertEquals("14", deg("2+3×4"))
        assertEquals("20", deg("(2+3)×4"))
        assertEquals("512", deg("2^3^2"))
        assertEquals("−4", deg("−2^2"))
        assertEquals("0.25", deg("2^−2"))
        assertEquals("−6", deg("2×−3"))
        assertEquals("2.5", deg("10÷4"))
    }

    @Test
    fun implicitMultiplicationAndAutoClose() {
        assertEquals("14", deg("2(3+4)"))
        assertEquals("21", deg("(1+2)(3+4)"))
        assertEquals("6.28318530718", deg("2π"))
        assertEquals("4", deg("√(16"))
        assertEquals("1", deg("2sin(30"))
    }

    @Test
    fun trigonometry() {
        assertEquals("0.5", deg("sin(30)"))
        assertEquals("0", deg("cos(90)"))
        assertEquals("0", deg("sin(180)"))
        assertEquals("1", deg("tan(45)"))
        assertEquals("90", deg("sin⁻¹(1)"))
        assertEquals("0", rad("sin(π)"))
        assertEquals("−1", rad("cos(π)"))
        assertEquals("0.785398163397", rad("tan⁻¹(1)"))
        assertThrows(CalcException::class.java) { deg("tan(90)") }
    }

    @Test
    fun functionsAndConstants() {
        assertEquals("3", deg("log(1000)"))
        assertEquals("1", deg("ln(e)"))
        assertEquals("10", deg("log₂(1024)"))
        assertEquals("3", deg("³√(27)"))
        assertEquals("−2", deg("³√(−8)"))
        assertEquals("5", deg("abs(−5)"))
        assertEquals("120", deg("5!"))
        assertEquals("1", deg("0!"))
        assertEquals("0.886226925453", deg("0.5!"))
        assertEquals("0.5", deg("50%"))
        assertEquals("1", deg("10 mod 3"))
        assertEquals("7.38905609893", deg("e^(2)"))
    }

    @Test
    fun ansAndScientificLiterals() {
        assertEquals("12", Formatter.format(Evaluator.evaluate("Ans×2", AngleMode.DEG, 6.0)))
        assertEquals("2000", deg("2E3"))
        assertEquals("0.003", deg("1.5E−3×2"))
    }

    @Test
    fun errors() {
        assertThrows(CalcException::class.java) { deg("1÷0") }
        assertThrows(CalcException::class.java) { deg("2+") }
        assertThrows(CalcException::class.java) { deg("√(−4)") }
        assertThrows(CalcException::class.java) { deg("ln(0)") }
        assertThrows(CalcException::class.java) { deg("171!") }
        assertThrows(CalcException::class.java) { deg("2E") }
        assertThrows(CalcException::class.java) { deg("()") }
    }

    @Test
    fun formatting() {
        assertEquals("0.3", deg("0.1+0.2"))
        assertEquals("1E15", Formatter.format(1e15))
        assertEquals("1.23456789012E14", Formatter.format(123456789012345.0))
        assertEquals("−1.234E−8", Formatter.format(-0.00000001234))
        assertEquals("999999999999", Formatter.format(999999999999.0))
        assertEquals("1,234,567.891", Formatter.group("1234567.891"))
        assertEquals("−1,234", Formatter.group("−1234"))
        assertEquals("1E15", Formatter.group("1E15"))
        assertEquals("0.5", Formatter.group("0.5"))
    }
}
