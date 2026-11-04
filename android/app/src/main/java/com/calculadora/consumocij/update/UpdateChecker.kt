package com.calculadora.consumocij.update

import android.content.Context
import com.calculadora.consumocij.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Comprobacion de versiones contra `version.json` del proyecto.
 *
 * La aplicacion sigue siendo totalmente funcional sin internet: esta consulta
 * solo se usa para avisar de una version nueva. Se prueba primero el archivo del
 * repositorio y, si falla, la copia publicada en GitHub Pages y el CDN de
 * jsDelivr (util para redes donde GitHub no esta disponible). No se envia
 * ningun dato del usuario (solo una peticion GET anonima).
 */
object UpdateChecker {

    private const val TIMEOUT_MS = 10_000

    sealed interface Result {
        data class Newer(val info: UpdateInfo) : Result
        data object UpToDate : Result
        data class Failed(val message: String) : Result
    }

    /** Debe llamarse desde un hilo de fondo (no bloquea la interfaz). */
    fun check(ctx: Context): Result {
        val urls = listOf(
            BuildConfig.UPDATE_URL,
            BuildConfig.UPDATE_URL_FALLBACK,
            BuildConfig.UPDATE_URL_CDN
        )
        var lastError = ""
        for (base in urls) {
            val url = base + (if (base.contains("?")) "&" else "?") + "t=" + System.currentTimeMillis()
            try {
                val text = httpGet(url)
                val info = UpdateInfo.parse(text)
                if (info == null) {
                    lastError = "respuesta no valida"
                    continue
                }
                UpdateStore.setLastCheck(ctx, System.currentTimeMillis())
                UpdateStore.setLatestKnownCode(ctx, info.versionCode)
                return if (info.versionCode > BuildConfig.VERSION_CODE) {
                    Result.Newer(info)
                } else {
                    Result.UpToDate
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
            }
        }
        return Result.Failed(lastError.ifBlank { "sin conexion" })
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ConsumoCIJ/${BuildConfig.VERSION_NAME}")
            useCaches = false
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (text.length > 64 * 1024) throw IOException("respuesta demasiado grande")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
