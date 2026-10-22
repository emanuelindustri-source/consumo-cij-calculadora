package com.calculadora.consumocij.util

import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.CalcEngine
import com.calculadora.consumocij.data.CalcResult
import com.calculadora.consumocij.data.VideojetData
import com.calculadora.consumocij.i18n.I18n
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generación de informes (CSV y TXT) 100 % locales: se escriben con el
 * selector de archivos del sistema (Storage Access Framework), sin internet.
 *
 * Las etiquetas y las cabeceras están traducidas a los seis idiomas de la app,
 * igual que en la versión web (`buildReport` de `web/calculadora-offline.html`).
 */
object Report {

    fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())

    /** «Modelo............: valor», con la etiqueta alineada en todos los idiomas. */
    private fun padRow(label: String, value: String, width: Int): String {
        val s = StringBuilder(label)
        while (s.length < width) s.append('.')
        return s.append(": ").append(value).toString()
    }

    fun csv(i: CalcInput, r: CalcResult): String {
        val rows = listOf(
            listOf(I18n.t("Concepto"), I18n.t("Valor"), I18n.t("Unidad")),
            listOf(I18n.t("Modelo de impresora"), r.model.id, ""),
            listOf(I18n.t("Familia"), if (r.model.platform == "simplicity") "Simplicity" else I18n.t("Serie 1000VJ"), ""),
            listOf(I18n.t("Boquilla"), i.nozzle, "µm"),
            listOf(I18n.t("Duración 1 L de solvente a 20°C"), Fmt.n(r.hrsL, 0), "h"),
            listOf(I18n.t("Tinta"), i.ink, ""),
            listOf(I18n.t("Base de solvente"), i.inkBase, ""),
            listOf(I18n.t("Tipo de fuente"), i.font, ""),
            listOf(I18n.t("Caracteres por línea"), i.chars.toString(), ""),
            listOf(I18n.t("Líneas"), i.lines.toString(), ""),
            listOf(I18n.t("Stroke rate"), i.stroke.toString(), ""),
            listOf(I18n.t("Ancho de impresión"), Fmt.n(i.widthMm, 1), "mm"),
            listOf(I18n.t("Temperatura"), Fmt.n(i.tempC, 1), "°C"),
            listOf(I18n.t("Impresiones por minuto"), Fmt.n(i.ppm, 0), ""),
            listOf(I18n.t("Horas por día"), Fmt.n(i.hoursDay, 1), "h"),
            listOf(I18n.t("Días por mes"), i.daysMonth.toString(), ""),
            listOf(I18n.t("Eficiencia"), Fmt.n(i.efficiencyPct, 0), "%"),
            listOf(I18n.t("Gotas por mensaje"), Fmt.i(r.dropsPerMessage), ""),
            listOf(I18n.t("Gotas por litro"), Fmt.i(r.dropsPerLiter), ""),
            listOf(I18n.t("Consumo solvente"), Fmt.n(r.muPerHour, 3), "ml/h"),
            listOf(I18n.t("Consumo solvente día"), Fmt.n(r.muDay, 1), I18n.t("ml/día")),
            listOf(I18n.t("Consumo solvente mes"), Fmt.n(r.muMonth, 0), I18n.t("ml/mes")),
            listOf(I18n.t("Consumo solvente año"), Fmt.n(r.muYear / 1000.0, 2), I18n.t("L/año")),
            listOf(I18n.t("Consumo tinta"), Fmt.n(r.inkPerHour, 3), "ml/h"),
            listOf(I18n.t("Consumo tinta día"), Fmt.n(r.inkDay, 1), I18n.t("ml/día")),
            listOf(I18n.t("Consumo tinta mes"), Fmt.n(r.inkMonth, 0), I18n.t("ml/mes")),
            listOf(I18n.t("Consumo tinta año"), Fmt.n(r.inkYear / 1000.0, 2), I18n.t("L/año")),
            listOf(I18n.t("Relación solvente:tinta"), if (r.ratio.isFinite()) Fmt.n(r.ratio, 2) else "", ":1"),
            listOf(I18n.t("Cartuchos solvente mes"), Fmt.n(r.muCartMonth, 1), ""),
            listOf(I18n.t("Cartuchos tinta mes"), Fmt.n(r.inkCartMonth, 1), ""),
            listOf(I18n.t("Horas por cartucho solvente"), Fmt.n(r.hoursPerMuCart, 0), "h"),
            listOf(I18n.t("Impresiones por cartucho tinta"), Fmt.i(r.printsPerInkCart), ""),
            listOf(I18n.t("Velocidad máxima de línea"), r.maxLineSpeed?.let { Fmt.n(it, 1) } ?: "", "m/min"),
            listOf(I18n.t("Vida núcleo (horas)"), if (i.coreHours > 0) Fmt.n(i.coreHours, 0) else "", "h"),
            listOf(I18n.t("Vida núcleo (días)"), if (i.coreDays > 0) Fmt.n(i.coreDays, 0) else "", I18n.t("días")),
            listOf(I18n.t("Vida útil en almacén"), if (i.shelfLifeMonths > 0) Fmt.n(i.shelfLifeMonths, 0) else "", I18n.t("meses"))
        )
        return rows.joinToString("\r\n") { row ->
            row.joinToString(";") { "\"${it.replace("\"", "\"\"")}\"" }
        }
    }

    fun txt(i: CalcInput, r: CalcResult): String {
        val labels = linkedMapOf(
            "model" to I18n.t("Modelo"),
            "nozzle" to I18n.t("Boquilla"),
            "ink" to I18n.t("Tinta"),
            "carts" to I18n.t("Cartuchos"),
            "font" to I18n.t("Fuente"),
            "chars" to I18n.t("Caracteres/línea"),
            "lines" to I18n.t("Líneas"),
            "stroke" to I18n.t("Stroke rate"),
            "width" to I18n.t("Ancho de impresión"),
            "drops" to I18n.t("Gotas/mensaje"),
            "temp" to I18n.t("Temperatura"),
            "ppm" to I18n.t("Impresiones/min"),
            "hrs" to I18n.t("Horas/día"),
            "part" to I18n.t("Pieza oficial"),
            "coreH" to I18n.t("Núcleo por horas"),
            "coreD" to I18n.t("Núcleo por días"),
            "shelf" to I18n.t("Vida útil en almacén"),
            "cps" to I18n.t("Caracteres/seg"),
            "cpi" to I18n.t("cpi / dpi"),
            "vmax" to I18n.t("Velocidad máxima de línea"),
            "wmin" to I18n.t("Ancho mínimo de impresión"),
            "solvent" to I18n.t("Solvente"),
            "ratio" to I18n.t("Relación")
        )
        val w = (labels.values.maxOf { it.length }) + 2
        fun row(k: String, v: String) = padRow(labels.getValue(k), v, w)

        val L = ArrayList<String>()
        L += I18n.t("CALCULADORA DE CONSUMO DE TINTA Y SOLVENTE — VIDEOJET CIJ")
        L += I18n.t("Modelos: CIJ Makeup Usage Model v14 · CIJ Line Speed Calculator v9.7 · CIJ TCO Calculator v35")
        L += I18n.t("Fecha") + ": " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        L += ""
        L += "== " + I18n.t("EQUIPO") + " =="
        L += row("model", r.model.id + " (" + (if (r.model.platform == "simplicity") "Simplicity" else I18n.t("Serie 1000VJ")) + ")")
        L += row("nozzle", i.nozzle + " µm")
        L += row("ink", i.ink + " (" + I18n.t("Base de solvente") + " " + i.inkBase + ")")
        L += row("carts", I18n.t("Tinta") + " " + Fmt.n(i.inkCartMl, 0) + " ml · " + I18n.t("Solvente") + " " + Fmt.n(i.muCartMl, 0) + " ml")
        L += ""
        L += "== " + I18n.t("MENSAJE") + " =="
        L += row("font", i.font)
        L += row("chars", i.chars.toString())
        L += row("lines", i.lines.toString())
        L += row("stroke", i.stroke.toString())
        L += row("width", Fmt.n(i.widthMm, 1) + " mm")
        L += row("drops", Fmt.i(r.dropsPerMessage) + " (" + Fmt.i(r.dropsPerChar) + " " + I18n.t("gotas/car.") + ")")
        L += ""
        L += "== " + I18n.t("PRODUCCIÓN") + " =="
        L += row("temp", Fmt.n(i.tempC, 1) + " °C")
        L += row("ppm", Fmt.n(i.ppm, 0) + " (" + I18n.t("Eficiencia") + " " + Fmt.n(i.efficiencyPct, 0) + " %)")
        L += row("hrs", Fmt.n(i.hoursDay, 1) + " · " + I18n.t("Días de producción / semana") + " " + i.daysWeek +
            " · " + I18n.t("Días de producción / mes") + " " + i.daysMonth)
        L += ""
        L += "== " + I18n.t("RESULTADOS") + " =="
        L += row("solvent", Fmt.n(r.muPerHour, 3) + " ml/h · " + Fmt.n(r.muDay, 1) + " " + I18n.t("ml/día") +
            " · " + Fmt.n(r.muMonth, 0) + " " + I18n.t("ml/mes") + " · " + Fmt.n(r.muYear / 1000.0, 2) + " " + I18n.t("L/año"))
        L += row("ink", Fmt.n(r.inkPerHour, 3) + " ml/h · " + Fmt.n(r.inkDay, 1) + " " + I18n.t("ml/día") +
            " · " + Fmt.n(r.inkMonth, 0) + " " + I18n.t("ml/mes") + " · " + Fmt.n(r.inkYear / 1000.0, 2) + " " + I18n.t("L/año"))
        L += row("ratio", (if (r.ratio.isFinite()) Fmt.n(r.ratio, 2) else "—") + " : 1 " + I18n.t("(regla práctica 4 : 1)"))
        L += row("carts", I18n.t("Solvente") + " " + Fmt.n(r.muCartMonth, 1) + "/" + I18n.t("mes") +
            " (" + Fmt.n(r.hoursPerMuCart, 0) + " " + I18n.t("h c/u") + ") · " + I18n.t("Tinta") + " " +
            Fmt.n(r.inkCartMonth, 1) + "/" + I18n.t("mes") + " (" + Fmt.i(r.printsPerInkCart) + " " + I18n.t("impresiones c/u") + ")")
        L += ""
        L += "== " + I18n.t("MANTENIMIENTO (NÚCLEO / KIT)") + " =="
        val mnt = VideojetData.maintenance[r.model.id]
        if (mnt != null && mnt.isNotEmpty()) {
            mnt.forEach { item ->
                val est = if (item.pnEstimated) " (" + I18n.t("orientativo") + ")" else ""
                val pnTxt = if (item.pn.isNotEmpty()) " · P/N ${item.pn}$est" else ""
                L += row("part", item.part + " · " + CalcEngine.formatInterval(item.hours) + pnTxt)
            }
        } else {
            L += I18n.t("Sin datos de mantenimiento para este modelo.")
        }
        if (i.coreHours > 0 && r.hoursMonth > 0) {
            L += row("coreH", I18n.t("vida") + " " + Fmt.i(i.coreHours) + " h · " + I18n.t("uso") + " " +
                Fmt.n(r.hoursYear, 0) + " " + I18n.t("h/año") + " → " + I18n.t("cambio cada") + " " +
                Fmt.n(i.coreHours / r.hoursMonth, 1) + " " + I18n.t("meses"))
        }
        if (i.coreDays > 0 && i.daysWeek > 0) {
            L += row("coreD", I18n.t("vida") + " " + Fmt.i(i.coreDays) + " " + I18n.t("días") + " · " +
                I18n.t("uso") + " " + Fmt.n(i.daysWeek * 52.0, 0) + " " + I18n.t("días/año") + " → " +
                I18n.t("cambio cada") + " " + Fmt.n(i.coreDays / (i.daysWeek * 4.345), 1) + " " + I18n.t("meses"))
        }
        if (i.shelfLifeMonths > 0) {
            L += row("shelf", Fmt.i(i.shelfLifeMonths) + " " + I18n.t("meses"))
        }
        L += ""
        L += "== " + I18n.t("VELOCIDAD DE LÍNEA") + " =="
        if (r.cps != null) {
            L += row("cps", Fmt.n(r.cps, 1) + " · " + I18n.t("velocidad nominal") + " " + Fmt.n(r.nominalSpeed ?: 0.0, 1) + " m/min")
            L += row("cpi", Fmt.n(r.cpi, 1) + " / " + Fmt.n(r.dpi, 0) + " · " + I18n.t("golpes/tira") + " " + r.strokes)
            L += row("vmax", Fmt.n(r.maxLineSpeed ?: 0.0, 1) + " m/min (" + I18n.t("real") + " " + Fmt.n(i.lineSpeed, 0) + " m/min)")
            L += row("wmin", Fmt.n(r.minPrintWidth ?: 0.0, 2) + " mm · " + I18n.t("máx. caracteres") + " " + Fmt.n(r.maxChars ?: 0.0, 0))
        } else {
            L += I18n.t("Sin datos de velocidad para esta combinación equipo/fuente.")
        }
        L += ""
        L += "== " + I18n.t("MODELO DE CÁLCULO") + " =="
        L += I18n.tf(
            "Solvente: ml/h = (1000 / {0} h/L) × {1} × {2} × e^({3} × {4} °C) × {5}",
            Fmt.n(r.hrsL, 0), Fmt.n(r.base.cons, 3), Fmt.n(VideojetData.K_PUBLISHED, 5),
            Fmt.n(r.base.k.first(), 5), Fmt.n(i.tempC, 1), Fmt.n(i.envFactor, 2)
        )
        L += I18n.tf("Tinta: ml/impresión = ({0} gotas ÷ {1} gotas/L) × 1000", Fmt.i(r.dropsPerMessage), Fmt.i(r.dropsPerLiter))
        L += ""
        L += I18n.t("Herramienta de estimación. THIS CALCULATOR IS FOR ESTIMATION PURPOSES ONLY.")
        return L.joinToString("\n")
    }

    fun fileName(prefix: String, ext: String): String =
        "${prefix}_${stamp()}.$ext"
}
