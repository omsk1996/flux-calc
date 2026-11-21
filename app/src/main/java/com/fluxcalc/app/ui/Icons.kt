package com.fluxcalc.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// Small line icons drawn on a 24×24 grid, so no icon library is needed.

fun DrawScope.drawBackspaceIcon(color: Color) {
    val s = size.minDimension / 24f
    val w = 1.9f * s
    val body = Path().apply {
        moveTo(9f * s, 5f * s)
        lineTo(19.5f * s, 5f * s)
        quadraticTo(22f * s, 5f * s, 22f * s, 7.5f * s)
        lineTo(22f * s, 16.5f * s)
        quadraticTo(22f * s, 19f * s, 19.5f * s, 19f * s)
        lineTo(9f * s, 19f * s)
        lineTo(2.5f * s, 12f * s)
        close()
    }
    drawPath(body, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(color, Offset(12f * s, 9.3f * s), Offset(17.4f * s, 14.7f * s), w, StrokeCap.Round)
    drawLine(color, Offset(17.4f * s, 9.3f * s), Offset(12f * s, 14.7f * s), w, StrokeCap.Round)
}

fun DrawScope.drawHistoryIcon(color: Color) {
    val s = size.minDimension / 24f
    val w = 1.9f * s
    drawCircle(color, radius = 9f * s, center = Offset(12f * s, 12f * s), style = Stroke(w))
    drawLine(color, Offset(12f * s, 7.2f * s), Offset(12f * s, 12f * s), w, StrokeCap.Round)
    drawLine(color, Offset(12f * s, 12f * s), Offset(15.4f * s, 14f * s), w, StrokeCap.Round)
}

fun DrawScope.drawChevronIcon(color: Color) {
    val s = size.minDimension / 24f
    val w = 2.1f * s
    drawLine(color, Offset(6f * s, 9.5f * s), Offset(12f * s, 15.5f * s), w, StrokeCap.Round)
    drawLine(color, Offset(12f * s, 15.5f * s), Offset(18f * s, 9.5f * s), w, StrokeCap.Round)
}
