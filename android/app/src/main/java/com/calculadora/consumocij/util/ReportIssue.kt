package com.calculadora.consumocij.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.calculadora.consumocij.i18n.I18n
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reporte de errores SIN red: redacta un informe con los datos técnicos y lo
 * entrega a la aplicación de correo del usuario (ACTION_SENDTO mailto:), por lo
 * que la app sigue sin necesitar el permiso INTERNET. Si no hay cliente de
 * correo, se ofrece compartir el texto o copiarlo al portapapeles.
 */
object ReportIssue {

    const val MAIL = "emanuel.industri@gmail.com"
    private const val CRASH_FILE = "last_crash.txt"

    fun build(context: Context, detail: String): String {
        val info = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val version = info?.versionName ?: "2.2.0"
        val build = info?.longVersionCode?.toString() ?: "-"
        return buildString {
            appendLine(I18n.t("Informe de error · Consumo CIJ (Android)"))
            appendLine()
            appendLine(I18n.t("Descripción:"))
            appendLine(detail.ifBlank { I18n.t("(sin descripción)") })
            appendLine()
            appendLine(I18n.t("— Datos técnicos —"))
            appendLine(I18n.tf("Versión: {0} (compilación {1})", version, build))
            appendLine(I18n.tf("Idioma: {0} · Zona: {1}", I18n.lang,
                java.util.TimeZone.getDefault().id))
            appendLine(I18n.tf("Dispositivo: {0} · Android {1} (API {2})",
                Build.MANUFACTURER + " " + Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
            appendLine(I18n.t("Fecha") + ": " +
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            val crash = lastCrash(context)
            if (!crash.isNullOrBlank()) {
                appendLine()
                appendLine(I18n.t("— Último fallo registrado —"))
                appendLine(crash.take(4000))
            }
        }
    }

    fun send(context: Context, body: String) {
        val subject = I18n.t("Informe de error · Consumo CIJ (Android)")
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + MAIL)).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(mail)
            return
        } catch (_: Exception) {
            // sin cliente de correo: se intenta compartir
        }
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(Intent.createChooser(share, null))
        } catch (_: Exception) {
            copy(context, body)
        }
    }

    fun copy(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(I18n.t("Consumo de Tinta y Solvente"), text))
    }

    fun saveCrash(context: Context, throwable: Throwable) {
        runCatching {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            File(context.filesDir, CRASH_FILE).writeText(sw.toString())
        }
    }

    fun lastCrash(context: Context): String? = runCatching {
        val f = File(context.filesDir, CRASH_FILE)
        if (f.exists()) f.readText() else null
    }.getOrNull()

    fun clearCrash(context: Context) {
        runCatching { File(context.filesDir, CRASH_FILE).delete() }
    }
}
