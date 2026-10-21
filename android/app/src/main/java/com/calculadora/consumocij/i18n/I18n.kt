package com.calculadora.consumocij.i18n

import android.content.Context
import org.json.JSONObject

/**
 * Traducciones de la interfaz. El catálogo vive en assets/i18n/<idioma>.json
 * (generado desde i18n/catalog.json del repositorio) y se elige según el
 * idioma del sistema o el idioma por aplicación de Android 13+.
 *
 * Español es el idioma de origen: si no hay traducción se devuelve el texto
 * original, de modo que la app nunca muestra una clave vacía.
 */
object I18n {

    /** Idioma activo: es, en, pt-BR, pt-PT, it o zh. */
    var lang: String = "es"
        private set

    private var map: Map<String, String> = emptyMap()

    fun init(context: Context) {
        val locale = context.resources.configuration.locales[0]
        lang = when (locale.language) {
            "pt" -> if (locale.country.equals("BR", ignoreCase = true)) "pt-BR" else "pt-PT"
            "zh" -> "zh"
            "it" -> "it"
            "en" -> "en"
            else -> "es"
        }
        map = if (lang == "es") emptyMap() else load(context, lang)
    }

    private fun load(context: Context, tag: String): Map<String, String> = runCatching {
        val txt = context.assets.open("i18n/$tag.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(txt)
        val out = HashMap<String, String>(obj.length())
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            out[k] = obj.getString(k)
        }
        out
    }.getOrDefault(emptyMap())

    /** Locale para dar formato a numeros y fechas. */
    fun activeLocale(): java.util.Locale =
        java.util.Locale.forLanguageTag(if (lang == "es") "es-ES" else lang)

    fun t(text: String): String = if (lang == "es") text else map[text] ?: text

    /** Plantillas con marcadores {0}, {1}... */
    fun tf(template: String, vararg args: Any?): String {
        var out = t(template)
        args.forEachIndexed { i, a -> out = out.replace("{$i}", a?.toString() ?: "") }
        return out
    }
}

/** Atajo para traducir literales de la interfaz. */
fun String.tr(): String = I18n.t(this)
