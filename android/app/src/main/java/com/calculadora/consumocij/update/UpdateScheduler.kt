package com.calculadora.consumocij.update

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

/**
 * Comprobacion diaria en segundo plano con JobScheduler (API del sistema, sin
 * dependencias externas). El trabajo se conserva tras reiniciar el dispositivo
 * (`setPersisted`) y solo se programa si el usuario activo los avisos.
 */
object UpdateScheduler {

    private const val JOB_ID = 4701
    private const val INTERVAL_MS = 24L * 60L * 60L * 1000L

    fun schedule(ctx: Context) {
        val js = ctx.getSystemService(JobScheduler::class.java) ?: return
        val component = ComponentName(ctx, UpdateJobService::class.java)
        val job = JobInfo.Builder(JOB_ID, component)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(INTERVAL_MS)
            .build()
        runCatching { js.schedule(job) }
    }

    fun cancel(ctx: Context) {
        runCatching { ctx.getSystemService(JobScheduler::class.java)?.cancel(JOB_ID) }
    }

    /** Programa o cancela segun la preferencia del usuario. */
    fun sync(ctx: Context) {
        if (UpdateStore.notify(ctx)) schedule(ctx) else cancel(ctx)
    }
}
