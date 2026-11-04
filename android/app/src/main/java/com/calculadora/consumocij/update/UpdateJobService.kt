package com.calculadora.consumocij.update

import android.app.job.JobParameters
import android.app.job.JobService
import com.calculadora.consumocij.BuildConfig
import com.calculadora.consumocij.i18n.I18n

/**
 * Trabajo periodico (una vez al dia) que consulta `version.json` y publica una
 * notificacion si hay una version nueva. Nunca descarga el APK por si solo: el
 * usuario decide desde la notificacion o desde la pantalla de la app.
 */
class UpdateJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        val app = applicationContext
        Thread {
            try {
                I18n.init(app)
                if (UpdateStore.autoCheck(app) && UpdateStore.notify(app)) {
                    when (val r = UpdateChecker.check(app)) {
                        is UpdateChecker.Result.Newer ->
                            if (UpdateStore.shouldRemind(app, r.info.versionCode) || r.info.mandatory) {
                                UpdateNotifier.notifyAvailable(app, r.info)
                            }
                        else -> Unit
                    }
                }
            } catch (_: Exception) {
                // Un fallo de red no debe romper el trabajo: se reintenta manana.
            } finally {
                jobFinished(params, false)
            }
        }.apply { isDaemon = true }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true

    companion object {
        /** Version instalada, expuesta para pruebas y para el informe tecnico. */
        val installedVersion: String get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }
}
