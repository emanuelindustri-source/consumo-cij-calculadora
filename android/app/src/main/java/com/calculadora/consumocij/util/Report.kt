package com.calculadora.consumocij.util

import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.CalcEngine
import com.calculadora.consumocij.data.CalcResult
import com.calculadora.consumocij.data.VideojetData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generación de informes (CSV y TXT) 100 % locales: se escriben con el
 * selector de archivos del sistema (Storage Access Framework), sin internet.
 */
object Report {

    fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())

    fun csv(i: CalcInput, r: CalcResult): String {
        val rows = listOf(
            listOf("Concepto", "Valor", "Unidad"),
            listOf("Modelo de impresora", r.model.id, ""),
            listOf("Familia", if (r.model.platform == "simplicity") "Simplicity" else "Serie 1000VJ", ""),
            listOf("Boquilla", i.nozzle, "µm"),
            listOf("Duración 1 L de solvente a 20°C", Fmt.n(r.hrsL, 0), "h"),
            listOf("Tinta", i.ink, ""),
            listOf("Base de solvente", i.inkBase, ""),
            listOf("Tipo de fuente", i.font, ""),
            listOf("Caracteres por línea", i.chars.toString(), ""),
            listOf("Líneas", i.lines.toString(), ""),
            listOf("Stroke rate", i.stroke.toString(), ""),
            listOf("Ancho de impresión", Fmt.n(i.widthMm, 1), "mm"),
            listOf("Temperatura", Fmt.n(i.tempC, 1), "°C"),
            listOf("Impresiones por minuto", Fmt.n(i.ppm, 0), ""),
            listOf("Horas por día", Fmt.n(i.hoursDay, 1), "h"),
            listOf("Días por mes", i.daysMonth.toString(), ""),
            listOf("Eficiencia", Fmt.n(i.efficiencyPct, 0), "%"),
            listOf("Gotas por mensaje", Fmt.i(r.dropsPerMessage), ""),
            listOf("Gotas por litro", Fmt.i(r.dropsPerLiter), ""),
            listOf("Consumo solvente", Fmt.n(r.muPerHour, 3), "ml/h"),
            listOf("Consumo solvente día", Fmt.n(r.muDay, 1), "ml/día"),
            listOf("Consumo solvente mes", Fmt.n(r.muMonth, 0), "ml/mes"),
            listOf("Consumo solvente año", Fmt.n(r.muYear / 1000.0, 2), "L/año"),
            listOf("Consumo tinta", Fmt.n(r.inkPerHour, 3), "ml/h"),
            listOf("Consumo tinta día", Fmt.n(r.inkDay, 1), "ml/día"),
            listOf("Consumo tinta mes", Fmt.n(r.inkMonth, 0), "ml/mes"),
            listOf("Consumo tinta año", Fmt.n(r.inkYear / 1000.0, 2), "L/año"),
            listOf("Relación solvente:tinta", if (r.ratio.isFinite()) Fmt.n(r.ratio, 2) else "", ":1"),
            listOf("Cartuchos solvente mes", Fmt.n(r.muCartMonth, 1), ""),
            listOf("Cartuchos tinta mes", Fmt.n(r.inkCartMonth, 1), ""),
            listOf("Horas por cartucho solvente", Fmt.n(r.hoursPerMuCart, 0), "h"),
            listOf("Impresiones por cartucho tinta", Fmt.i(r.printsPerInkCart), ""),
            listOf("Velocidad máxima de línea", r.maxLineSpeed?.let { Fmt.n(it, 1) } ?: "", "m/min"),
            listOf("Vida núcleo (horas)", if (i.coreHours > 0) Fmt.n(i.coreHours, 0) else "", "h"),
            listOf("Vida núcleo (días)", if (i.coreDays > 0) Fmt.n(i.coreDays, 0) else "", "días"),
            listOf("Vida útil en almacén", if (i.shelfLifeMonths > 0) Fmt.n(i.shelfLifeMonths, 0) else "", "meses")
        )
        return rows.joinToString("\r\n") { row ->
            row.joinToString(";") { "\"${it.replace("\"", "\"\"")}\"" }
        }
    }

    fun txt(i: CalcInput, r: CalcResult): String {
        val L = ArrayList<String>()
        L += "CALCULADORA DE CONSUMO DE TINTA Y SOLVENTE — VIDEOJET CIJ"
        L += "Modelos: CIJ Makeup Usage Model v14 · CIJ Line Speed Calculator v9.7 · CIJ TCO Calculator v35"
        L += "Fecha: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        L += ""
        L += "== EQUIPO =="
        L += "Modelo............: ${r.model.id} (${if (r.model.platform == "simplicity") "Simplicity" else "Serie 1000VJ"})"
        L += "Boquilla..........: ${i.nozzle} µm"
        L += "Tinta.............: ${i.ink} (base ${i.inkBase})"
        L += "Cartuchos.........: tinta ${Fmt.n(i.inkCartMl, 0)} ml · solvente ${Fmt.n(i.muCartMl, 0)} ml"
        L += ""
        L += "== MENSAJE =="
        L += "Fuente............: ${i.font}"
        L += "Caracteres/línea..: ${i.chars}"
        L += "Líneas............: ${i.lines}"
        L += "Stroke rate.......: ${i.stroke}"
        L += "Ancho impresión...: ${Fmt.n(i.widthMm, 1)} mm"
        L += "Gotas/mensaje.....: ${Fmt.i(r.dropsPerMessage)} (${Fmt.i(r.dropsPerChar)} gotas/car.)"
        L += ""
        L += "== PRODUCCIÓN =="
        L += "Temperatura.......: ${Fmt.n(i.tempC, 1)} °C"
        L += "Impresiones/min...: ${Fmt.n(i.ppm, 0)} (eficiencia ${Fmt.n(i.efficiencyPct, 0)} %)"
        L += "Horas/día.........: ${Fmt.n(i.hoursDay, 1)} · días/semana ${i.daysWeek} · días/mes ${i.daysMonth}"
        L += ""
        L += "== RESULTADOS =="
        L += "SOLVENTE: ${Fmt.n(r.muPerHour, 3)} ml/h · ${Fmt.n(r.muDay, 1)} ml/día · ${Fmt.n(r.muMonth, 0)} ml/mes · ${Fmt.n(r.muYear / 1000.0, 2)} L/año"
        L += "TINTA....: ${Fmt.n(r.inkPerHour, 3)} ml/h · ${Fmt.n(r.inkDay, 1)} ml/día · ${Fmt.n(r.inkMonth, 0)} ml/mes · ${Fmt.n(r.inkYear / 1000.0, 2)} L/año"
        L += "RELACIÓN.: ${if (r.ratio.isFinite()) Fmt.n(r.ratio, 2) else "—"} : 1 (regla práctica 4 : 1)"
        L += "CARTUCHOS: solvente ${Fmt.n(r.muCartMonth, 1)}/mes (${Fmt.n(r.hoursPerMuCart, 0)} h c/u) · tinta ${Fmt.n(r.inkCartMonth, 1)}/mes (${Fmt.i(r.printsPerInkCart)} impresiones c/u)"
        L += ""
        L += "== VELOCIDAD DE LÍNEA =="
        if (r.cps != null) {
            L += "Caracteres/seg....: ${Fmt.n(r.cps, 1)} · velocidad nominal ${Fmt.n(r.nominalSpeed ?: 0.0, 1)} m/min"
            L += "cpi / dpi.........: ${Fmt.n(r.cpi, 1)} / ${Fmt.n(r.dpi, 0)} · golpos/tira ${r.strokes}"
            L += "Velocidad máx.....: ${Fmt.n(r.maxLineSpeed ?: 0.0, 1)} m/min (real ${Fmt.n(i.lineSpeed, 0)} m/min)"
            L += "Ancho mín. imp....: ${Fmt.n(r.minPrintWidth ?: 0.0, 2)} mm · máx. caracteres ${Fmt.n(r.maxChars ?: 0.0, 0)}"
        } else {
            L += "Sin datos de velocidad para esta combinación equipo/fuente."
        }
        L += ""
        L += "== MANTENIMIENTO (NÚCLEO / KIT) =="
        val mnt = VideojetData.maintenance[r.model.id]
        if (mnt != null && mnt.isNotEmpty()) {
            mnt.forEach { item ->
                val est = if (item.pnEstimated) " (orientativo)" else ""
                val pnTxt = if (item.pn.isNotEmpty()) " · P/N ${item.pn}$est" else ""
                L += "Pieza oficial.....: ${item.part} · ${CalcEngine.formatInterval(item.hours)}$pnTxt"
            }
        } else {
            L += "Sin datos de mantenimiento para este modelo."
        }
        if (i.coreHours > 0 && r.hoursMonth > 0) {
            L += "Núcleo por horas..: vida ${Fmt.i(i.coreHours)} h → cambio cada ${Fmt.n(i.coreHours / r.hoursMonth, 1)} meses"
        }
        if (i.coreDays > 0 && i.daysWeek > 0) {
            L += "Núcleo por días...: vida ${Fmt.i(i.coreDays)} días → cambio cada ${Fmt.n(i.coreDays / (i.daysWeek * 4.345), 1)} meses"
        }
        L += ""
        L += "== MODELO DE CÁLCULO =="
        L += "Solvente: ml/h = (1000 / ${Fmt.n(r.hrsL, 0)} h/L) × ${Fmt.n(r.base.cons, 3)} × ${Fmt.n(VideojetData.K_PUBLISHED, 5)} × e^(k × ${Fmt.n(i.tempC, 1)} °C) × ${Fmt.n(i.envFactor, 2)}"
        L += "Tinta...: ml/impresión = (${Fmt.i(r.dropsPerMessage)} gotas ÷ ${Fmt.i(r.dropsPerLiter)} gotas/L) × 1000"
        L += ""
        L += "Herramienta de estimación. THIS CALCULATOR IS FOR ESTIMATION PURPOSES ONLY."
        return L.joinToString("\n")
    }

    fun fileName(prefix: String, ext: String): String =
        "${prefix}_${stamp()}.$ext"
}
