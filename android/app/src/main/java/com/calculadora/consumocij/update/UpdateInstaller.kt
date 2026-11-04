package com.calculadora.consumocij.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/** Instalacion del APK descargado con el instalador del sistema. */
object UpdateInstaller {

    fun install(ctx: Context): Boolean {
        val file = UpdateStore.apkFile(ctx) ?: return false
        return try {
            val uri = FileProvider.getUriForFile(
                ctx,
                ctx.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun sha256(file: File): String = try {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (true) {
                read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        ""
    }
}
