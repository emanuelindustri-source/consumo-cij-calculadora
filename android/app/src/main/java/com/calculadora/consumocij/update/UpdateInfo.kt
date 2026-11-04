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
    val mandatory: Boolean
) {
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
            UpdateInfo(
                version = version,
                versionCode = code,
                date = o.optString("date", "").trim(),
                apkUrl = apk,
                apkSha256 = o.optString("apkSha256", "").trim(),
                apkSizeBytes = o.optLong("apkSizeBytes", 0L),
                notesUrl = o.optString("notesUrl", "").trim(),
                mandatory = o.optBoolean("mandatory", false)
            )
        }.getOrNull()
    }
}
