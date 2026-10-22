package com.calculadora.consumocij.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.calculadora.consumocij.ui.theme.LocalExtraColors
import com.calculadora.consumocij.util.Fmt

/**
 * Gráficos dibujados con Canvas (sin librerías externas): se escalan
 * automáticamente a la resolución y densidad de cualquier pantalla.
 */

@Composable
fun TempCurveChart(
    points: List<Pair<Double, Double>>,
    currentTemp: Double,
    heightDp: Int = 220,
    modifier: Modifier = Modifier
) {
    val extra = LocalExtraColors.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    ) {
        if (points.size < 2) return@Canvas
        val padL = 58f; val padR = 18f; val padT = 18f; val padB = 40f
        val iw = size.width - padL - padR
        val ih = size.height - padT - padB
        val minX = points.first().first
        val maxX = points.last().first
        val maxY = (points.maxOf { it.second }).coerceAtLeast(0.001) * 1.15
        fun px(x: Double) = padL + ((x - minX) / (maxX - minX)) * iw
        fun py(y: Double) = padT + ih - (y / maxY) * ih

        // rejilla
        val grid = extra.chartGrid
        val textPaint = android.graphics.Paint().apply {
            textSize = 11f * density
            isAntiAlias = true
        }
        textPaint.color = android.graphics.Color.argb(
            (labelColor.alpha * 255).toInt(),
            (labelColor.red * 255).toInt(),
            (labelColor.green * 255).toInt(),
            (labelColor.blue * 255).toInt()
        )
        for (i in 0..5) {
            val v = maxY / 5 * i
            val y = py(v).toFloat()
            drawLine(grid, Offset(padL, y), Offset(padL + iw, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                Fmt.n(v, 1), (padL - 8f), y + 4f * density, textPaint
            )
        }
        for (t in intArrayOf(10, 20, 30, 40)) {
            if (t.toDouble() < minX || t.toDouble() > maxX) continue
            drawContext.canvas.nativeCanvas.drawText(
                "$t°", px(t.toDouble()).toFloat(), (padT + ih + 18f * density), textPaint
            )
        }

        // área + curva
        val path = Path()
        points.forEachIndexed { idx, (x, y) ->
            val p = Offset(px(x).toFloat(), py(y).toFloat())
            if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(px(maxX).toFloat(), (padT + ih).toFloat())
            lineTo(px(minX).toFloat(), (padT + ih).toFloat())
            close()
        }
        drawPath(fill, extra.accent.copy(alpha = 0.18f))
        drawPath(path, extra.accent, style = Stroke(width = 4f))

        // referencia 20 °C
        if (minX <= 20.0 && maxX >= 20.0) {
            val x20 = px(20.0).toFloat()
            drawLine(
                extra.accent2, Offset(x20, padT), Offset(x20, padT + ih),
                strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // temperatura actual
        val tc = currentTemp.coerceIn(minX, maxX)
        val yv = points.minByOrNull { kotlin.math.abs(it.first - tc) }?.second ?: 0.0
        val cx = px(tc).toFloat(); val cy = py(yv).toFloat()
        drawLine(
            extra.warning, Offset(cx, padT), Offset(cx, padT + ih),
            strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
        drawCircle(extra.warning, radius = 9f, center = Offset(cx, cy))
        drawCircle(surfaceColor, radius = 4f, center = Offset(cx, cy))
    }
}

@Composable
fun BarsChart(
    solventMonth: Double,
    inkMonth: Double,
    heightDp: Int = 220,
    modifier: Modifier = Modifier
) {
    val extra = LocalExtraColors.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    ) {
        val padL = 58f; val padR = 18f; val padT = 26f; val padB = 52f
        val iw = size.width - padL - padR
        val ih = size.height - padT - padB
        val maxV = maxOf(solventMonth, inkMonth, 1.0) * 1.2
        val textPaint = android.graphics.Paint().apply {
            textSize = 11f * density
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
        }
        val boldPaint = android.graphics.Paint(textPaint).apply {
            textSize = 12f * density
            isFakeBoldText = true
        }
        for (i in 0..5) {
            val v = maxV / 5 * i
            val y = (padT + ih - (v / maxV) * ih).toFloat()
            drawLine(extra.chartGrid, Offset(padL, y), Offset(padL + iw, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(Fmt.i(v), padL - 8f, y + 4f * density, textPaint)
        }
        val bw = minOf(150f, iw / 3.2f)
        val data = listOf(
            Triple("Solvente", solventMonth, extra.accent),
            Triple("Tinta", inkMonth, extra.accent2)
        )
        data.forEachIndexed { idx, (lab, v, color) ->
            val cx = padL + iw * (if (idx == 0) 0.30f else 0.72f)
            val bh = ((v / maxV) * ih).toFloat().coerceAtLeast(2f)
            val top = (padT + ih - bh)
            drawRect(color.copy(alpha = 0.85f), Offset(cx - bw / 2, top), androidx.compose.ui.geometry.Size(bw, bh))
            drawContext.canvas.nativeCanvas.drawText(
                "${Fmt.i(v)} ml", cx - bw / 2, top - 8f * density, boldPaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                lab, cx - bw / 2, (padT + ih + 18f * density), textPaint
            )
        }
        val ratio = if (inkMonth > 0) solventMonth / inkMonth else Double.POSITIVE_INFINITY
        drawContext.canvas.nativeCanvas.drawText(
            "${com.calculadora.consumocij.i18n.I18n.t("Relación solvente : tinta")} = ${Fmt.n(ratio, 2)} : 1  ·  ${com.calculadora.consumocij.i18n.I18n.t("Regla práctica 1 : 4")}",
            padL, (padT + ih + 42f * density), textPaint
        )
    }
}

@Composable
fun TimelineChart(
    muPerHour: Double,
    inkPerHour: Double,
    hoursDay: Double,
    cartMl: Double,
    heightDp: Int = 220,
    modifier: Modifier = Modifier
) {
    val extra = LocalExtraColors.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    ) {
        val padL = 58f; val padR = 18f; val padT = 20f; val padB = 46f
        val iw = size.width - padL - padR
        val ih = size.height - padT - padB
        val hrs = maxOf(hoursDay, 1.0)
        val muEnd = muPerHour * hrs
        val inkEnd = inkPerHour * hrs
        val maxV = maxOf(muEnd, inkEnd, cartMl, 1.0) * 1.18
        val textPaint = android.graphics.Paint().apply {
            textSize = 11f * density
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
        }
        fun px(h: Double) = padL + (h / hrs) * iw
        fun py(v: Double) = padT + ih - (v / maxV) * ih

        for (i in 0..5) {
            val v = maxV / 5 * i
            val y = py(v).toFloat()
            drawLine(extra.chartGrid, Offset(padL, y), Offset(padL + iw, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(Fmt.i(v), padL - 8f, y + 4f * density, textPaint)
        }
        for (i in 0..4) {
            val h = hrs / 4 * i
            drawContext.canvas.nativeCanvas.drawText(
                "${Fmt.n(h, 0)}h", px(h).toFloat(), (padT + ih + 18f * density), textPaint
            )
        }

        if (cartMl > 0 && cartMl < maxV) {
            val y = py(cartMl).toFloat()
            drawLine(
                extra.warning, Offset(padL, y), Offset(padL + iw, y),
                strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }
        listOf(
            Triple("Solvente", muEnd, extra.accent),
            Triple("Tinta", inkEnd, extra.accent2)
        ).forEach { (lab, end, lineColor) ->
            if (end <= 0.0) return@forEach
            val fill = Path().apply {
                moveTo(px(0.0).toFloat(), py(0.0).toFloat())
                lineTo(px(hrs).toFloat(), py(end).toFloat())
                lineTo(px(hrs).toFloat(), (padT + ih).toFloat())
                lineTo(px(0.0).toFloat(), (padT + ih).toFloat())
                close()
            }
            drawPath(fill, lineColor.copy(alpha = 0.16f))
            drawLine(
                lineColor, Offset(px(0.0).toFloat(), py(0.0).toFloat()),
                Offset(px(hrs).toFloat(), py(end).toFloat()), strokeWidth = 4f
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$lab: ${Fmt.i(end)} ml",
                (px(hrs) - 120f * density).toFloat(),
                (py(end) - 10f * density).toFloat(),
                android.graphics.Paint().apply {
                    textSize = 11f * density
                    isFakeBoldText = true
                    isAntiAlias = true
                    this.color = android.graphics.Color.argb(
                        (lineColor.alpha * 255).toInt(),
                        (lineColor.red * 255).toInt(),
                        (lineColor.green * 255).toInt(),
                        (lineColor.blue * 255).toInt()
                    )
                }
            )
        }
    }
}
