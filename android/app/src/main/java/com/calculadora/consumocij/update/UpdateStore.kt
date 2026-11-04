package com.calculadora.consumocij.update

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * Preferencias del sistema de actualizaciones (100 % locales).
 *
 * No se guarda ningun dato personal: solo la ultima comprobacion, la version
 * omitida y el estado de la descarga en curso.
 */
object UpdateStore {

    private const val PREFS = "vj_update"
    private const val KEY_AUTO = "auto"
    private const val KEY_NOTIFY = "notify"
    private const val KEY_ASKED = "asked"
    private const val KEY_SKIPPED = "skipped"
    private const val KEY_SKIPPED_AT = "skipped_at"
    private const val KEY_PROMPTED = "prompted"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LATEST = "latest_code"
    private const val KEY_DL_ID = "download_id"
    private const val KEY_APK_PATH = "apk_path"
    private const val KEY_APK_SHA = "apk_sha"
    private const val KEY_PENDING_CODE = "pending_code"
    private const val KEY_PENDING_VERSION = "pending_version"
    private const val KEY_READY = "ready"
    private const val KEY_ERROR = "last_error"

    private fun p(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun autoCheck(ctx: Context): Boolean = p(ctx).getBoolean(KEY_AUTO, true)
    fun setAutoCheck(ctx: Context, value: Boolean) =
        p(ctx).edit().putBoolean(KEY_AUTO, value).apply()

    fun notify(ctx: Context): Boolean = p(ctx).getBoolean(KEY_NOTIFY, false)
    fun setNotify(ctx: Context, value: Boolean) =
        p(ctx).edit().putBoolean(KEY_NOTIFY, value).apply()

    fun askedNotifications(ctx: Context): Boolean = p(ctx).getBoolean(KEY_ASKED, false)
    fun setAskedNotifications(ctx: Context, value: Boolean) =
        p(ctx).edit().putBoolean(KEY_ASKED, value).apply()

    fun skippedVersion(ctx: Context): Int = p(ctx).getInt(KEY_SKIPPED, 0)
    fun setSkippedVersion(ctx: Context, code: Int) =
        p(ctx).edit().putInt(KEY_SKIPPED, code).apply()

    fun skippedAt(ctx: Context): Long = p(ctx).getLong(KEY_SKIPPED_AT, 0L)

    /** Guarda la version omitida y el momento, para recordarla a los 14 dias. */
    fun skipVersion(ctx: Context, code: Int) =
        p(ctx).edit().putInt(KEY_SKIPPED, code).putLong(KEY_SKIPPED_AT, System.currentTimeMillis()).apply()

    /** true si toca volver a avisar: no se omitio o ya pasaron 14 dias. */
    fun shouldRemind(ctx: Context, code: Int): Boolean {
        if (skippedVersion(ctx) != code) return true
        val at = skippedAt(ctx)
        return at > 0L && System.currentTimeMillis() - at >= REMIND_AFTER_MS
    }

    private const val REMIND_AFTER_MS = 14L * 24L * 60L * 60L * 1000L

    fun promptedVersion(ctx: Context): Int = p(ctx).getInt(KEY_PROMPTED, 0)
    fun setPromptedVersion(ctx: Context, code: Int) =
        p(ctx).edit().putInt(KEY_PROMPTED, code).apply()

    fun lastCheck(ctx: Context): Long = p(ctx).getLong(KEY_LAST_CHECK, 0L)
    fun setLastCheck(ctx: Context, millis: Long) =
        p(ctx).edit().putLong(KEY_LAST_CHECK, millis).apply()

    fun latestKnownCode(ctx: Context): Int = p(ctx).getInt(KEY_LATEST, 0)
    fun setLatestKnownCode(ctx: Context, code: Int) =
        p(ctx).edit().putInt(KEY_LATEST, code).apply()

    /* -------- descarga en curso -------- */

    fun downloadId(ctx: Context): Long = p(ctx).getLong(KEY_DL_ID, -1L)

    fun startDownload(
        ctx: Context,
        id: Long,
        apkPath: String,
        sha256: String,
        versionCode: Int,
        version: String
    ) {
        p(ctx).edit()
            .putLong(KEY_DL_ID, id)
            .putString(KEY_APK_PATH, apkPath)
            .putString(KEY_APK_SHA, sha256)
            .putInt(KEY_PENDING_CODE, versionCode)
            .putString(KEY_PENDING_VERSION, version)
            .putBoolean(KEY_READY, false)
            .remove(KEY_ERROR)
            .apply()
    }

    fun clearDownloadId(ctx: Context) = p(ctx).edit().remove(KEY_DL_ID).apply()

    fun apkPath(ctx: Context): String? = p(ctx).getString(KEY_APK_PATH, null)
    fun apkSha256(ctx: Context): String = p(ctx).getString(KEY_APK_SHA, "").orEmpty()
    fun pendingVersionCode(ctx: Context): Int = p(ctx).getInt(KEY_PENDING_CODE, 0)
    fun pendingVersion(ctx: Context): String = p(ctx).getString(KEY_PENDING_VERSION, "").orEmpty()

    fun apkReady(ctx: Context): Boolean =
        p(ctx).getBoolean(KEY_READY, false) && apkFile(ctx)?.exists() == true

    fun setApkReady(ctx: Context, ready: Boolean) =
        p(ctx).edit().putBoolean(KEY_READY, ready).apply()

    fun apkFile(ctx: Context): File? =
        apkPath(ctx)?.let { File(it) }?.takeIf { it.isFile && it.length() > 0 }

    fun lastError(ctx: Context): String? = p(ctx).getString(KEY_ERROR, null)
    fun setLastError(ctx: Context, message: String?) =
        p(ctx).edit().apply { if (message == null) remove(KEY_ERROR) else putString(KEY_ERROR, message) }
            .apply()

    /** Borra el APK descargado (tras instalar o si ya no es valido). */
    fun clearApk(ctx: Context) {
        runCatching { apkFile(ctx)?.delete() }
        p(ctx).edit()
            .remove(KEY_APK_PATH)
            .remove(KEY_APK_SHA)
            .remove(KEY_PENDING_CODE)
            .remove(KEY_PENDING_VERSION)
            .putBoolean(KEY_READY, false)
            .apply()
    }
}
