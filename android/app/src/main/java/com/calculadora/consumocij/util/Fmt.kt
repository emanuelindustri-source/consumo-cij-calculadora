package com.calculadora.consumocij.util

import com.calculadora.consumocij.i18n.I18n
import java.text.NumberFormat
import java.util.Locale

/** Formato numérico según el idioma activo de la app (es-ES / en-GB / pt-BR / zh-CN...). */
object Fmt {
    private val locale: Locale get() = I18n.activeLocale()

    fun n(v: Double, dec: Int = 1): String {
        if (!v.isFinite()) return "—"
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = dec
        nf.maximumFractionDigits = dec
        return nf.format(v)
    }

    fun i(v: Double): String {
        if (!v.isFinite()) return "—"
        return NumberFormat.getIntegerInstance(locale).format(kotlin.math.round(v).toLong())
    }

    fun pct(v: Double, dec: Int = 0): String = n(v, dec) + " %"
}
