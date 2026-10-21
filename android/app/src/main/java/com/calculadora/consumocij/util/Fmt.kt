package com.calculadora.consumocij.util

import java.text.NumberFormat
import java.util.Locale

/** Formato numérico según el idioma del dispositivo (es-ES / es-MX / etc.). */
object Fmt {
    private val locale: Locale get() = Locale.getDefault()

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
