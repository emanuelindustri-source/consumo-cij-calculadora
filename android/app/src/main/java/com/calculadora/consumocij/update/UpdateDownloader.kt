package com.calculadora.consumocij.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.calculadora.consumocij.i18n.I18n
import java.io.File

/**
 * Descarga del APK con DownloadManager (servicio del sistema).
 *
 * El archivo se guarda en el almacenamiento privado de la app
 * (`Android/data/<paquete>/files/Download`), se comprueba su SHA-256 y solo
 * entonces se ofrece el instalador del sistema.
 */
object UpdateDownloader {

    private const val FILE_NAME = "ConsumoCIJ-update.apk"

    fun start(ctx: Context, info: UpdateInfo): Boolean {
        val dm = ctx.getSystemService(DownloadManager::class.java) ?: return false
        val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
        if (!dir.exists() && !dir.mkdirs()) return false
        val file = File(dir, FILE_NAME)
        if (file.exists()) file.delete()
        UpdateStore.clearApk(ctx)

        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle(I18n.t("Actualización de Consumo CIJ"))
            .setDescription(I18n.tf("Descargando la versión {0}…", info.version))
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, FILE_NAME)

        return try {
            val id = dm.enqueue(request)
            UpdateStore.startDownload(ctx, id, file.absolutePath, info.apkSha256, info.versionCode, info.version)
            true
        } catch (_: Exception) {
            false
        }
    }
}
