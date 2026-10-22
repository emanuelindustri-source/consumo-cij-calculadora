package com.calculadora.consumocij.i18n

import android.content.Context
import org.json.JSONObject

/**
 * Traducciones de la interfaz. El catálogo vive en `assets/i18n/<idioma>.json`
 * (generado desde `i18n/catalog.json` del repositorio) y se elige así:
 *
 *  1. Si el usuario ha elegido idioma dentro de la app, esa preferencia.
 *  2. Si no, el idioma por aplicación de Android 13+ o el del sistema.
 *
 * Español es el idioma de origen: si no hay traducción se devuelve el texto
 * original, de modo que la app nunca muestra una clave vacía.
 */
object I18n {

    /** Idiomas disponibles, en el orden del selector. */
    val LANGUAGES = listOf("", "es", "en", "pt-BR", "pt-PT", "it", "zh")

    /** Nombre de cada idioma en su propia lengua (no se traduce). */
    private val NATIVE = mapOf(
        "es" to "Español",
        "en" to "English",
        "pt-BR" to "Português (BR)",
        "pt-PT" to "Português (PT)",
        "it" to "Italiano",
        "zh" to "中文"
    )

    private const val PREFS = "vj_calc_prefs"
    private const val KEY_LANG = "app_lang"

    /** Idioma activo: es, en, pt-BR, pt-PT, it o zh. */
    var lang: String = "es"
        private set

    /** Preferencia del usuario; cadena vacía = seguir el idioma del sistema. */
    var preference: String = ""
        private set

    private var map: Map<String, String> = emptyMap()

    fun init(context: Context) {
        preference = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, "") ?: ""
        }.getOrDefault("")
        lang = resolve(context)
        map = if (lang == "es") emptyMap() else load(context, lang)
    }

    /** Cambia el idioma de la aplicación y lo recuerda para los próximos arranques. */
    fun setLanguage(context: Context, tag: String) {
        preference = tag
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LANG, tag).apply()
        }
        lang = resolve(context)
        map = if (lang == "es") emptyMap() else load(context, lang)
    }

    /** Etiqueta del selector para una preferencia dada ("" = idioma del sistema). */
    fun labelOf(tag: String): String = if (tag.isEmpty()) "Idioma del sistema" else (NATIVE[tag] ?: tag)

    private fun resolve(context: Context): String {
        if (preference.isNotEmpty()) return preference
        val locale = context.resources.configuration.locales[0]
        return when (locale.language) {
            "pt" -> if (locale.country.equals("BR", ignoreCase = true)) "pt-BR" else "pt-PT"
            "zh" -> "zh"
            "it" -> "it"
            "en" -> "en"
            else -> "es"
        }
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

    /** Locale para dar formato a números y fechas. */
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
