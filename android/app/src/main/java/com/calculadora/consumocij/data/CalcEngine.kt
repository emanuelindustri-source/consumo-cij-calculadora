package com.calculadora.consumocij.data

import kotlin.math.exp
import kotlin.math.floor

/**
 * Motor de cálculo — puerto exacto de los modelos oficiales Videojet:
 *  · Solvente: CIJ Makeup Usage Model v14
 *  · Tinta:    CIJ TCO Calculator v35
 *  · Velocidad: CIJ Line Speed Calculator v9.7
 *
 * Contrastado con 734 fichas oficiales de solvente y 535 de velocidad de línea.
 */
enum class TemperatureModel { PUBLISHED, EQUATION }

data class CalcInput(
    val modelId: String = "VJ 1880+",
    val nozzle: String = "60",
    val ink: String = "V4201",
    val inkBase: String = "MEK",
    val font: String = "5x7 Single",
    val chars: Int = 12,
    val lines: Int = 1,
    val stroke: Int = 1,
    val widthMm: Double = 11.0,
    val tempC: Double = 25.0,
    val ppm: Double = 60.0,
    val hoursDay: Double = 16.0,
    val daysWeek: Int = 5,
    val daysMonth: Int = 22,
    val efficiencyPct: Double = 90.0,
    val lineSpeed: Double = 50.0,
    val productWidthMm: Double = 120.0,
    val gapMm: Double = 30.0,
    val inkCartMl: Double = 750.0,
    val muCartMl: Double = 750.0,
    val shelfLifeMonths: Double = 12.0,
    val coreHours: Double = 0.0,
    val coreDays: Double = 0.0,
    val temperatureModel: TemperatureModel = TemperatureModel.PUBLISHED,
    val envFactor: Double = 1.0
)

data class CalcResult(
    val model: VideojetData.ModelEntry,
    val variant: Variant,
    val base: InkBaseInfo,
    val fontInfo: FontInfo,
    val hrsL: Double,
    val dropsPerChar: Double,
    val dropsPerMessage: Double,
    val dropsPerLiter: Double,
    val inkMlPerPrint: Double,
    val muPerHour: Double,
    val inkPerHour: Double,
    val printsPerHour: Double,
    val muDay: Double, val inkDay: Double,
    val muMonth: Double, val inkMonth: Double,
    val muYear: Double, val inkYear: Double,
    val hoursMonth: Double, val hoursYear: Double,
    val ratio: Double,
    val hoursPerMuCart: Double,
    val hoursPerInkCart: Double,
    val printsPerInkCart: Double,
    val muCartMonth: Double,
    val inkCartMonth: Double,
    val cps: Double?, val nominalSpeed: Double?,
    val strokes: Int, val cpi: Double, val dpi: Double,
    val maxLineSpeed: Double?, val minPrintWidth: Double?, val maxChars: Double?, val maxPpm: Double?,
    /** false cuando la base de la tinta no tiene datos oficiales (V4238 / V4251). */
    val officialData: Boolean = true
)

object CalcEngine {

    /** Modelo seleccionado (con fallback al primero). */
    fun model(id: String): VideojetData.ModelEntry =
        VideojetData.models.firstOrNull { it.id == id } ?: VideojetData.models.first()

    /** Datos de la boquilla elegida (VJ 1880/1880+ cambian de consumo entre 60 y 70 µm). */
    fun variant(m: VideojetData.ModelEntry, nozzle: String): Variant =
        m.variants[nozzle] ?: m.defaultVariant

    fun dropsPerCharOf(font: String): Double {
        if (font.contains("QR", ignoreCase = true)) return VideojetData.DROPS_QR_ESTIMATE
        val matrix = font.split(" ").firstOrNull().orEmpty()
        return when (matrix) {
            "4x5" -> VideojetData.DROPS_4X5_ESTIMATE
            else -> VideojetData.dropsChar[matrix] ?: 13.0
        }
    }

    fun dropsLiterOf(nozzle: String, ref: String): Double =
        VideojetData.dropsLiter["$nozzle|$ref"]
            ?: VideojetData.dropsLiter["60|VJ1000"]
            ?: 1_232_240_697.15

    /** Consumo de solvente (make-up) en ml/hora según el modelo oficial. */
    fun makeupPerHour(
        hrsL: Double, base: InkBaseInfo, variant: Variant,
        tempC: Double, envFactor: Double, model: TemperatureModel
    ): Double {
        val k = if (model == TemperatureModel.EQUATION) base.k.getOrElse(variant.eq - 1) { base.k[0] } else base.k[0]
        val kk = if (model == TemperatureModel.EQUATION) variant.kconst else VideojetData.K_PUBLISHED
        return (1000.0 / hrsL) * base.cons * kk * exp(k * tempC) * envFactor
    }

    fun compute(i: CalcInput): CalcResult {
        val m = model(i.modelId)
        val v = variant(m, i.nozzle)
        val base = VideojetData.inkBases[i.inkBase] ?: VideojetData.inkBases["MEK"]!!
        val font = VideojetData.fonts[i.font] ?: FontInfo(1, 5, 7)

        /* ---------- Solvente ---------- */
        val muPerHour = makeupPerHour(v.hrsL, base, v, i.tempC, i.envFactor, i.temperatureModel)

        /* ---------- Tinta ---------- */
        val dpc = dropsPerCharOf(i.font)
        val dropsMsg = dpc * i.chars * i.lines * i.stroke
        val dpl = dropsLiterOf(i.nozzle, m.ref)
        val inkMlPerPrint = (dropsMsg / dpl) * 1000.0
        val printsPerHour = i.ppm * 60.0 * (i.efficiencyPct / 100.0)
        val inkPerHour = inkMlPerPrint * printsPerHour

        /* ---------- Escalado temporal ---------- */
        val hoursMonth = i.hoursDay * i.daysMonth
        val hoursYear = i.hoursDay * i.daysWeek * 52.0
        val muDay = muPerHour * i.hoursDay
        val inkDay = inkPerHour * i.hoursDay
        val muMonth = muPerHour * hoursMonth
        val inkMonth = inkPerHour * hoursMonth
        val muYear = muPerHour * hoursYear
        val inkYear = inkPerHour * hoursYear
        val ratio = if (inkMonth > 0.0) muMonth / inkMonth else Double.POSITIVE_INFINITY

        /* ---------- Cartuchos ---------- */
        val hoursPerMuCart = if (muPerHour > 0.0) i.muCartMl / muPerHour else Double.POSITIVE_INFINITY
        val hoursPerInkCart = if (inkPerHour > 0.0) i.inkCartMl / inkPerHour else Double.POSITIVE_INFINITY
        val printsPerInkCart = if (inkMlPerPrint > 0.0) i.inkCartMl / inkMlPerPrint else Double.POSITIVE_INFINITY

        /* ---------- Velocidad (CIJ Line Speed Calculator v9.7) ---------- */
        val set = if (i.nozzle == "70") VideojetData.speedSet2 else VideojetData.speedSet1
        val entry = set[i.font]?.get(m.speedCol)
        val cps = entry?.cps
        val nominal = entry?.nom
        val strokesPerChar = font.w + 1
        val strokes = (i.chars * strokesPerChar * i.lines)
        val printWidthIn = i.widthMm / 25.4
        val cpi = if (printWidthIn > 0.0) i.chars / printWidthIn else 0.0
        val dpi = if (printWidthIn > 0.0) (strokes.toDouble() / i.lines) / printWidthIn else 0.0
        val maxLineSpeed = cps?.let { (it / i.chars) * i.widthMm * 0.06 }
        val minPrintWidth = if (cps != null && i.lineSpeed > 0.0) (i.lineSpeed / 0.06) / (cps / i.chars) else null
        val maxChars = if (cps != null && i.lineSpeed > 0.0) (cps * i.widthMm) / (i.lineSpeed / 0.06) else null
        val maxPpm = if (i.lineSpeed > 0.0 && (i.productWidthMm + i.gapMm) > 0.0)
            i.lineSpeed / ((i.productWidthMm + i.gapMm) / 1000.0) else null

        return CalcResult(
            model = m, variant = v, base = base, fontInfo = font, hrsL = v.hrsL,
            dropsPerChar = dpc, dropsPerMessage = dropsMsg, dropsPerLiter = dpl,
            inkMlPerPrint = inkMlPerPrint,
            muPerHour = muPerHour, inkPerHour = inkPerHour, printsPerHour = printsPerHour,
            muDay = muDay, inkDay = inkDay,
            muMonth = muMonth, inkMonth = inkMonth,
            muYear = muYear, inkYear = inkYear,
            hoursMonth = hoursMonth, hoursYear = hoursYear,
            ratio = ratio,
            hoursPerMuCart = hoursPerMuCart, hoursPerInkCart = hoursPerInkCart,
            printsPerInkCart = printsPerInkCart,
            muCartMonth = if (i.muCartMl > 0) muMonth / i.muCartMl else 0.0,
            inkCartMonth = if (i.inkCartMl > 0) inkMonth / i.inkCartMl else 0.0,
            cps = cps, nominalSpeed = nominal,
            strokes = strokes, cpi = cpi, dpi = dpi,
            maxLineSpeed = maxLineSpeed, minPrintWidth = minPrintWidth,
            maxChars = maxChars, maxPpm = maxPpm,
            officialData = VideojetData.hasOfficialBase(i.ink)
        )
    }

    /** Curva de consumo de solvente entre dos temperaturas (para los gráficos). */
    fun makeupCurve(
        i: CalcInput, from: Int = 10, to: Int = 40, step: Int = 1
    ): List<Pair<Double, Double>> {
        val m = model(i.modelId)
        val v = variant(m, i.nozzle)
        val base = VideojetData.inkBases[i.inkBase] ?: VideojetData.inkBases["MEK"]!!
        val out = ArrayList<Pair<Double, Double>>()
        var t = from
        while (t <= to) {
            val td = t.toDouble()
            out.add(td to makeupPerHour(v.hrsL, base, v, td, i.envFactor, i.temperatureModel))
            t += step
        }
        return out
    }

    /** Intervalo de mantenimiento en texto legible. */
    fun formatInterval(hours: Double): String = when {
        hours <= 0.0 -> "—"
        hours == 8760.0 -> com.calculadora.consumocij.i18n.I18n.t("12 meses (~ 8.760 h)")
        hours >= 45360.0 -> com.calculadora.consumocij.i18n.I18n.t("63 meses (~ 45.360 h)")
        else -> com.calculadora.consumocij.i18n.I18n.tf("{0} horas", java.text.NumberFormat.getIntegerInstance(com.calculadora.consumocij.i18n.I18n.activeLocale()).format(floor(hours)))
    }
}
