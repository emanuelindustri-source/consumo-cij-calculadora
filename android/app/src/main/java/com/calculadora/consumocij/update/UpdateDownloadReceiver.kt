package com.calculadora.consumocij.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calculadora.consumocij.i18n.I18n

/**
 * Aviso de DownloadManager: delega en [UpdateDownloadProcessor] la verificacion
 * de la huella y la preparacion de la instalacion.
 *
 * El receptor es exportado porque el aviso lo envia el sistema (DownloadManager);
 * solo actua si el identificador coincide con la descarga que inicio la app.
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
                UpdateDownloadProcessor.process(app, id)
            } finally {
                pending.finish()
            }
        }.apply { isDaemon = true }.start()
    }
}
