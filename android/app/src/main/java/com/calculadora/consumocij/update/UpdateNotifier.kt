package com.calculadora.consumocij.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.calculadora.consumocij.MainActivity
import com.calculadora.consumocij.R
import com.calculadora.consumocij.i18n.I18n

/**
 * Notificaciones locales de actualizacion.
 *
 * · ID_AVAILABLE: hay una version nueva (la abre el dialogo de la app).
 * · ID_READY: el APK ya se descargo y se puede instalar.
 * · ID_ERROR: la descarga o la verificacion fallo.
 *
 * Nunca se pide permiso aqui: el usuario lo concede desde la app y, si lo
 * deniega, el aviso sigue apareciendo dentro de la interfaz.
 */
object UpdateNotifier {

    const val CHANNEL = "updates"
    const val ID_AVAILABLE = 4701
    const val ID_READY = 4702
    const val ID_ERROR = 4703

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL,
            I18n.t("Actualizaciones"),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = I18n.t("Avisos de nuevas versiones de Consumo CIJ")
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun canNotify(ctx: Context): Boolean =
        runCatching { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
            .getOrDefault(false)

    fun notifyAvailable(ctx: Context, info: UpdateInfo) {
        if (!canNotify(ctx)) return
        ensureChannel(ctx)
        val n = base(ctx)
            .setContentTitle(I18n.tf("Nueva versión {0} disponible", info.version))
            .setContentText(I18n.t("Toque para ver las novedades y actualizar."))
            .setStyle(NotificationCompat.BigTextStyle().bigText(I18n.t("Toque para ver las novedades y actualizar.")))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp(ctx, ID_AVAILABLE, openUpdate = true, install = false))
            .setAutoCancel(true)
            .build()
        post(ctx, ID_AVAILABLE, n)
    }

    fun notifyReady(ctx: Context) {
        if (!canNotify(ctx)) return
        ensureChannel(ctx)
        val version = UpdateStore.pendingVersion(ctx)
        val text = if (version.isBlank()) {
            I18n.t("Toque para instalar la actualización.")
        } else {
            I18n.tf("Toque para instalar la versión {0}.", version)
        }
        val n = base(ctx)
            .setContentTitle(I18n.t("Actualización descargada"))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(ctx, ID_READY, openUpdate = true, install = true))
            .setAutoCancel(true)
            .build()
        post(ctx, ID_READY, n)
    }

    fun notifyError(ctx: Context) {
        if (!canNotify(ctx)) return
        ensureChannel(ctx)
        val message = UpdateStore.lastError(ctx) ?: I18n.t("No se pudo descargar la actualización.")
        val n = base(ctx)
            .setContentTitle(I18n.t("Actualización de Consumo CIJ"))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp(ctx, ID_ERROR, openUpdate = true, install = false))
            .setAutoCancel(true)
            .build()
        post(ctx, ID_ERROR, n)
    }

    fun cancel(ctx: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(ctx).cancel(id) }
    }

    private fun base(ctx: Context) = NotificationCompat.Builder(ctx, CHANNEL)
        .setSmallIcon(R.drawable.ic_update)
        .setOnlyAlertOnce(false)

    private fun openApp(ctx: Context, request: Int, openUpdate: Boolean, install: Boolean): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_UPDATE, openUpdate)
            putExtra(MainActivity.EXTRA_INSTALL_UPDATE, install)
        }
        return PendingIntent.getActivity(
            ctx, request, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun post(ctx: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(ctx).notify(id, notification)
        } catch (_: SecurityException) {
            // Permiso POST_NOTIFICATIONS no concedido: el aviso queda en la app.
        }
    }
}
