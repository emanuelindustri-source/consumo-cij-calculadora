package com.calculadora.consumocij.update

import org.json.JSONObject

/**
 * Datos publicados en `version.json` (raiz del repositorio y GitHub Pages).
 *
 * El archivo es la unica fuente de verdad del sistema de actualizaciones: la app
 * lo descarga, compara [versionCode] con el suyo y, si hay una version nueva,
 * avisa al usuario y ofrece la descarga del APK firmado.
 */
data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val date: String,
    val apkUrl: String,
    val apkSha256: String,
    val apkSizeBytes: Long,
    val notesUrl: String,
    val mandatory: Boolean,
    /** Puntos destacados por idioma (es, en, pt-BR, pt-PT, it, zh). */
    val highlights: Map<String, List<String>> = emptyMap()
) {
    /** Puntos destacados para el idioma activo, con español como respaldo. */
    fun highlightsFor(lang: String): List<String> =
        highlights[lang] ?: highlights["es"] ?: emptyList()

    /** Tamanio legible para el dialogo (p. ej. «1,9 MB»). */
    fun sizeLabel(): String {
        if (apkSizeBytes <= 0L) return ""
        val mb = apkSizeBytes / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }

    companion object {
        /** Acepta el JSON completo o un objeto con los mismos campos. */
        fun parse(text: String): UpdateInfo? = runCatching {
            val o = JSONObject(text)
            val code = o.optInt("versionCode", 0)
            val version = o.optString("version", "").trim()
            val apk = o.optString("apkUrl", "").trim()
            if (code <= 0 || version.isEmpty() || !apk.startsWith("https://")) return null
            val highlights = LinkedHashMap<String, List<String>>()
            o.optJSONObject("highlights")?.let { h ->
                for (lang in h.keys()) {
                    val arr = h.optJSONArray(lang) ?: continue
                    val list = ArrayList<String>(arr.length())
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i, "").trim()
                        if (s.isNotEmpty()) list.add(s)
                    }
                    if (list.isNotEmpty()) highlights[lang] = list
                }
            }
            UpdateInfo(
                version = version,
                versionCode = code,
                date = o.optString("date", "").trim(),
                apkUrl = apk,
                apkSha256 = o.optString("apkSha256", "").trim(),
                apkSizeBytes = o.optLong("apkSizeBytes", 0L),
                notesUrl = o.optString("notesUrl", "").trim(),
                mandatory = o.optBoolean("mandatory", false),
                highlights = highlights
            )
        }.getOrNull()
    }
}
