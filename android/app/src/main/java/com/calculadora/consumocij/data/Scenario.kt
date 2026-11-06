package com.calculadora.consumocij.data

import com.calculadora.consumocij.i18n.I18n
import com.calculadora.consumocij.util.Fmt

/**
 * Escenarios guardados en el comparador de equipos (2.5.0).
 * Se guardan en el dispositivo, igual que el cálculo actual.
 */
sealed interface Scenario {
    val id: String
    val name: String

    /** Cálculo completo de un equipo Videojet. */
    data class Videojet(
        override val id: String,
        override val name: String,
        val input: CalcInput
    ) : Scenario

    /** Datos mensuales declarados por el usuario para otro equipo o la competencia. */
    data class Competitor(
        override val id: String,
        override val name: String,
        val inkMlMonth: Double,
        val muMlMonth: Double,
        val inkPriceL: Double,
        val muPriceL: Double,
        val printsMonth: Double,
        val currency: String
    ) : Scenario
}

/** Una columna del comparador, ya calculada y lista para mostrar. */
data class CompareColumn(
    val id: String,
    val kind: String,
    val name: String,
    val currency: String,
    val configured: Boolean,
    val model: String,
    val ink: String,
    val hoursMonth: Double,
    val printsMonth: Double,
    val inkMonth: Double,
    val muMonth: Double,
    val inkCartMonth: Double,
    val muCartMonth: Double,
    val inkCostMonth: Double,
    val muCostMonth: Double,
    val totalMonth: Double,
    val totalYear: Double,
    val costPer1000: Double,
    val years: Int
)

object Comparator {

    const val MAX = 4

    fun columns(
        currentInput: CalcInput,
        currentResult: CalcResult,
        currentName: String,
        scenarios: List<Scenario>
    ): List<CompareColumn> {
        val out = ArrayList<CompareColumn>()
        out.add(fromVideojet("current", "current", currentName, currentInput, currentResult))
        scenarios.forEach { sc ->
            when (sc) {
                is Scenario.Videojet -> {
                    val r = CalcEngine.compute(sc.input)
                    out.add(fromVideojet(sc.id, "videojet", sc.name, sc.input, r))
                }
                is Scenario.Competitor -> out.add(fromCompetitor(sc))
            }
        }
        return out
    }

    private fun fromVideojet(
        id: String,
        kind: String,
        name: String,
        i: CalcInput,
        r: CalcResult
    ): CompareColumn {
        val c = CalcEngine.costs(i, r)
        return CompareColumn(
            id = id, kind = kind, name = name, currency = i.currency,
            configured = c.configured, model = r.model.id, ink = i.ink,
            hoursMonth = r.hoursMonth,
            printsMonth = r.printsPerHour * r.hoursMonth,
            inkMonth = r.inkMonth, muMonth = r.muMonth,
            inkCartMonth = r.inkCartMonth, muCartMonth = r.muCartMonth,
            inkCostMonth = c.inkMonth, muCostMonth = c.muMonth,
            totalMonth = c.totalMonth, totalYear = c.totalYear,
            costPer1000 = c.costPer1000, years = c.years
        )
    }

    private fun fromCompetitor(sc: Scenario.Competitor): CompareColumn {
        val inkMl = sc.inkMlMonth.coerceAtLeast(0.0)
        val muMl = sc.muMlMonth.coerceAtLeast(0.0)
        val inkPriceL = sc.inkPriceL.coerceAtLeast(0.0)
        val muPriceL = sc.muPriceL.coerceAtLeast(0.0)
        val inkCost = inkMl / 1000.0 * inkPriceL
        val muCost = muMl / 1000.0 * muPriceL
        val total = inkCost + muCost
        val prints = sc.printsMonth.coerceAtLeast(0.0)
        return CompareColumn(
            id = sc.id, kind = "competitor", name = sc.name, currency = sc.currency,
            configured = inkMl > 0.0 || muMl > 0.0 || total > 0.0,
            model = sc.name, ink = "—",
            hoursMonth = 0.0, printsMonth = prints,
            inkMonth = inkMl, muMonth = muMl,
            inkCartMonth = 0.0, muCartMonth = 0.0,
            inkCostMonth = inkCost, muCostMonth = muCost,
            totalMonth = total, totalYear = total * 12.0,
            costPer1000 = if (prints > 0.0) total / prints * 1000.0 else Double.POSITIVE_INFINITY,
            years = 0
        )
    }

    /**
     * Exporta las columnas del comparador en CSV con el mismo formato que
     * `exportComparison` de la web: una fila por concepto y una columna por
     * escenario, separador `;`, campos entre comillas y fin de línea CRLF.
     * La primera columna es siempre el cálculo actual, de modo que la fila
     * «Diferencia mensual» mide cada escenario contra ese cálculo.
     */
    fun csv(columns: List<CompareColumn>): String {
        val base = columns.firstOrNull { it.id == "current" }
        val rows = ArrayList<List<String>>()
        rows.add(listOf(I18n.t("Concepto")) + columns.map { "${it.name} (${it.currency})" })

        fun add(label: String, fn: (CompareColumn) -> String) {
            rows.add(listOf(label) + columns.map(fn))
        }

        add(I18n.t("Equipo")) { it.model }
        add(I18n.t("Tinta")) { it.ink }
        add(I18n.t("Horas al mes")) { if (it.hoursMonth > 0) Fmt.i(it.hoursMonth) else "" }
        add(I18n.t("Tinta al mes")) { if (it.inkMonth > 0) Fmt.i(it.inkMonth) else "" }
        add(I18n.t("Solvente al mes")) { if (it.muMonth > 0) Fmt.i(it.muMonth) else "" }
        add(I18n.t("Costo de tinta")) { if (it.configured) Fmt.n(it.inkCostMonth, 2) else "" }
        add(I18n.t("Costo de solvente")) { if (it.configured) Fmt.n(it.muCostMonth, 2) else "" }
        add(I18n.t("Costo total al mes")) { if (it.configured) Fmt.n(it.totalMonth, 2) else "" }
        add(I18n.t("Costo total al año")) { if (it.configured) Fmt.n(it.totalYear, 2) else "" }
        add(I18n.t("Costo por 1.000 códigos")) {
            if (it.configured && it.costPer1000.isFinite()) Fmt.n(it.costPer1000, 2) else ""
        }
        add(I18n.t("Diferencia mensual")) {
            if (it.configured && base != null && base.configured)
                Fmt.n(it.totalMonth - base.totalMonth, 2) else ""
        }

        return rows.joinToString("\r\n") { row ->
            row.joinToString(";") { "\"${it.replace("\"", "\"\"")}\"" }
        }
    }
}
