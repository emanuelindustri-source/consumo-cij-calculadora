package com.calculadora.consumocij.util

import com.calculadora.consumocij.i18n.I18n
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formato de importes para la calculadora de costos (2.5.0).
 * Se usa el formato monetario del idioma activo y el código ISO elegido.
 */
object Money {

    val CODES = listOf("USD", "EUR", "MXN", "COP", "BRL", "ARS", "CLP", "PEN", "GBP", "CNY")

    fun normalize(code: String): String {
        val c = code.trim().uppercase(Locale.ROOT)
        return if (c in CODES) c else "USD"
    }

    fun format(value: Double, code: String, locale: Locale = I18n.activeLocale()): String {
        if (!value.isFinite()) return "—"
        val nf = NumberFormat.getCurrencyInstance(locale)
        runCatching { nf.currency = Currency.getInstance(normalize(code)) }
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return nf.format(value)
    }

    fun formatOrDash(value: Double, code: String, on: Boolean): String =
        if (on) format(value, code) else "—"
}
