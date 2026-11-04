package com.calculadora.consumocij.update

import android.app.DownloadManager
import android.content.Context
import com.calculadora.consumocij.i18n.I18n
import java.io.File

/**
 * Cierra una descarga cuando DownloadManager termina: comprueba el SHA-256
 * publicado y deja el APK listo para instalar (o lo borra si no coincide).
 *
 * Se usa desde dos sitios para que no dependa de un unico broadcast:
 *  · [UpdateDownloadReceiver], cuando el sistema avisa de la descarga.
 *  · La app, al volver a primer plano o mientras muestra el dialogo, por si el
 *    aviso se perdio (Android puede diferir broadcasts en segundo plano).
 */
object UpdateDownloadProcessor {

    /** true si la descarga ya termino (bien o mal) y se proceso aqui. */
    @Synchronized
    fun process(ctx: Context, id: Long): Boolean {
        if (id <= 0L) return false
        val dm = ctx.getSystemService(DownloadManager::class.java) ?: return false
        val status = runCatching {
            dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                if (!c.moveToFirst()) -1
                else c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            } ?: -1
        }.getOrDefault(-1)

        return when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                complete(ctx)
                true
            }
            DownloadManager.STATUS_FAILED -> {
                fail(ctx, I18n.t("No se pudo descargar la actualización."))
                true
            }
            else -> false
        }
    }

    private fun complete(ctx: Context) {
        val file = UpdateStore.apkFile(ctx)
        if (file == null || !file.exists()) {
            fail(ctx, I18n.t("No se pudo descargar la actualización."))
            return
        }
        val expected = UpdateStore.apkSha256(ctx).lowercase()
        if (expected.isNotEmpty()) {
            val actual = UpdateInstaller.sha256(file).lowercase()
            if (actual != expected) {
                file.delete()
                UpdateStore.clearApk(ctx)
                fail(
                    ctx,
                    I18n.t("El archivo descargado no coincide con la huella publicada. Se canceló la instalación.")
                )
                return
            }
        }
        UpdateStore.clearDownloadId(ctx)
        UpdateStore.setLastError(ctx, null)
        UpdateStore.setApkReady(ctx, true)
        UpdateNotifier.notifyReady(ctx)
    }

    private fun fail(ctx: Context, message: String) {
        UpdateStore.clearDownloadId(ctx)
        UpdateStore.setLastError(ctx, message)
        runCatching { UpdateStore.apkFile(ctx)?.let { File(it.path).delete() } }
        UpdateStore.setApkReady(ctx, false)
        UpdateNotifier.notifyError(ctx)
    }
}
