package com.calculadora.consumocij.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calculadora.consumocij.i18n.I18n
import java.io.File

/**
 * Recibe el aviso de DownloadManager cuando termina la descarga, comprueba la
 * huella SHA-256 publicada y prepara la instalacion.
 *
 * Si la huella no coincide, el APK se borra y se avisa: nunca se instala un
 * archivo que no sea exactamente el publicado.
 */
class UpdateDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val app = context.applicationContext
        if (id <= 0L || id != UpdateStore.downloadId(app)) return

        val pending = goAsync()
        Thread {
            try {
                I18n.init(app)
                if (!downloadSucceeded(app, id)) {
                    fail(app, I18n.t("No se pudo descargar la actualización."))
                    return@Thread
                }
                UpdateStore.clearDownloadId(app)
                val file = UpdateStore.apkFile(app)
                if (file == null || !file.exists()) {
                    fail(app, I18n.t("No se pudo descargar la actualización."))
                    return@Thread
                }
                val expected = UpdateStore.apkSha256(app).lowercase()
                if (expected.isNotEmpty()) {
                    val actual = UpdateInstaller.sha256(file).lowercase()
                    if (actual != expected) {
                        file.delete()
                        UpdateStore.clearApk(app)
                        fail(
                            app,
                            I18n.t("El archivo descargado no coincide con la huella publicada. Se canceló la instalación.")
                        )
                        return@Thread
                    }
                }
                UpdateStore.setLastError(app, null)
                UpdateStore.setApkReady(app, true)
                UpdateNotifier.notifyReady(app)
            } finally {
                pending.finish()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun downloadSucceeded(ctx: Context, id: Long): Boolean {
        val dm = ctx.getSystemService(DownloadManager::class.java) ?: return false
        val cursor = dm.query(DownloadManager.Query().setFilterById(id)) ?: return false
        return cursor.use {
            if (!it.moveToFirst()) return false
            val col = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            col >= 0 && it.getInt(col) == DownloadManager.STATUS_SUCCESSFUL
        }
    }

    private fun fail(ctx: Context, message: String) {
        UpdateStore.clearDownloadId(ctx)
        UpdateStore.setLastError(ctx, message)
        runCatching { UpdateStore.apkFile(ctx)?.let { File(it.path).delete() } }
        UpdateStore.setApkReady(ctx, false)
        UpdateNotifier.notifyError(ctx)
    }
}
