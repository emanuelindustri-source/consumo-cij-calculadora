package com.calculadora.consumocij.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.calculadora.consumocij.CalculatorViewModel
import com.calculadora.consumocij.i18n.I18n
import com.calculadora.consumocij.i18n.tr
import com.calculadora.consumocij.ui.theme.LocalExtraColors
import java.text.SimpleDateFormat
import java.util.Date

/* ============================================================
   ACTUALIZACIONES
   · Aviso dentro de la app (siempre funciona, sin permisos)
   · Notificacion local diaria (solo si el usuario la activa)
   · Descarga del APK, verificacion SHA-256 e instalador del sistema
   ============================================================ */

private fun granted(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun lastCheckLabel(millis: Long): String =
    if (millis <= 0L) I18n.t("Nunca")
    else SimpleDateFormat("dd/MM/yyyy HH:mm", I18n.activeLocale()).format(Date(millis))

/** Aviso discreto bajo la cabecera: se ve sin abrir ningun menu. */
@Composable
fun UpdateBanner(vm: CalculatorViewModel) {
    val info = vm.updateAvailable ?: return
    var visible by remember(info.versionCode) { mutableStateOf(true) }
    if (!visible) return
    val extra = LocalExtraColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extra.accent.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.NewReleases, contentDescription = null, tint = extra.accent)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    I18n.tf("Nueva versión {0} disponible.", info.version),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    I18n.t("Toque para ver las novedades y actualizar."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = {
                visible = false
                vm.requestUpdateDialog()
            }) { Text("Actualizar".tr()) }
            IconButton(onClick = { visible = false }) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar".tr())
            }
        }
    }
}

/** Dialogo principal: comprobacion, novedades, descarga e instalacion. */
@Composable
fun UpdateDialogHost(vm: CalculatorViewModel) {
    if (!vm.updateDialogOpen) return
    val context = LocalContext.current
    val info = vm.updateAvailable
    val extra = LocalExtraColors.current

    val title: String
    val body: @Composable () -> Unit
    var confirm: (@Composable () -> Unit)? = null

    if (info != null) {
        title = "Nueva versión disponible".tr()
        body = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    I18n.tf(
                        "Versión {0} · compilación {1} · {2}",
                        info.version, info.versionCode.toString(), info.date
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (info.apkSizeBytes > 0) {
                    Text(
                        I18n.tf("Descarga: {0}", info.sizeLabel()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val bullets = info.highlightsFor(I18n.lang)
                if (bullets.isNotEmpty()) {
                    Text(
                        "Novedades de esta versión".tr(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    bullets.forEach { b ->
                        Text("• $b", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (info.mandatory) {
                    Text(
                        "Actualización obligatoria: incluye correcciones importantes.".tr(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "Se instalará sobre la versión actual; sus datos y su historial se conservan.".tr(),
                    style = MaterialTheme.typography.bodySmall
                )
                if (vm.downloadReady) {
                    Text(
                        "Actualización descargada. Pulse «Instalar» para terminar.".tr(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extra.success
                    )
                } else if (vm.downloading) {
                    Text(
                        "Descargando la actualización…".tr(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                vm.updateError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                vm.installError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (!vm.downloadReady && !info.mandatory) {
                    TextButton(
                        onClick = { vm.skipUpdate() },
                        enabled = !vm.downloading
                    ) { Text("Omitir esta versión".tr()) }
                }
            }
        }
        confirm = {
            if (vm.downloadReady) {
                Button(onClick = { vm.installUpdate(context) }) { Text("Instalar".tr()) }
            } else {
                Button(
                    onClick = { vm.downloadUpdate(context) },
                    enabled = !vm.downloading
                ) { Text(if (vm.downloading) "Descargando la actualización…".tr() else "Descargar e instalar".tr()) }
            }
        }
    } else {
        title = if (vm.updateChecking) "Buscando actualizaciones…".tr() else "Actualizaciones".tr()
        body = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (vm.updateChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(34.dp))
                } else if (vm.updateError != null) {
                    Text(
                        "No se pudo comprobar la actualización. Revise la conexión e inténtelo de nuevo.".tr(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "Tiene la última versión instalada.".tr(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    I18n.tf("Versión instalada: {0} · compilación {1}", BuildConfigVersion.name, BuildConfigVersion.code),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = { vm.closeUpdateDialog() },
        title = { Text(title) },
        text = { body() },
        confirmButton = { confirm?.invoke() },
        dismissButton = {
            TextButton(onClick = {
                vm.clearUpdateError()
                vm.closeUpdateDialog()
            }) { Text(if (info != null) "Más tarde".tr() else "Cerrar".tr()) }
        }
    )
}

/** Pregunta una sola vez si se quieren recibir notificaciones de version nueva. */
@Composable
fun NotificationsPromptHost(vm: CalculatorViewModel) {
    if (vm.notificationsAsked) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> vm.enableNotifyUpdates(ok) }

    AlertDialog(
        onDismissRequest = { vm.markNotificationsAsked() },
        icon = { Icon(Icons.Filled.NewReleases, contentDescription = null) },
        title = { Text("¿Activar los avisos de actualización?".tr()) },
        text = {
            Text(
                "Recibirá una notificación cuando se publique una versión nueva. Puede desactivarlo cuando quiera en «Actualizaciones».".tr(),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = {
                if (granted(context)) vm.enableNotifyUpdates(true) else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("Activar avisos".tr()) }
        },
        dismissButton = {
            TextButton(onClick = { vm.markNotificationsAsked() }) { Text("Ahora no".tr()) }
        }
    )
}

/** Tarjeta de ajustes: version instalada, comprobacion automatica y avisos. */
@Composable
fun UpdateSection(vm: CalculatorViewModel) {
    val context = LocalContext.current
    val extra = LocalExtraColors.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> vm.enableNotifyUpdates(ok) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = extra.accent)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Actualizaciones".tr(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                I18n.tf("Versión instalada: {0} · compilación {1}", BuildConfigVersion.name, BuildConfigVersion.code),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                when {
                    vm.updateChecking -> "Buscando actualizaciones…".tr()
                    vm.updateAvailable != null ->
                        I18n.tf("Nueva versión {0} disponible.", vm.updateAvailable!!.version)
                    vm.updateError != null -> "No se pudo comprobar la actualización. Revise la conexión e inténtelo de nuevo.".tr()
                    vm.updateUpToDate -> "Tiene la última versión instalada.".tr()
                    else -> I18n.tf("Última comprobación: {0}", lastCheckLabel(vm.lastCheck))
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (vm.updateError != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            SwitchRow(
                label = "Comprobación automática".tr(),
                checked = vm.autoCheck,
                onChange = { vm.enableAutoCheck(it) }
            )
            SwitchRow(
                label = "Avisarme cuando haya una versión nueva".tr(),
                checked = vm.notifyUpdates,
                onChange = { on ->
                    if (on && !granted(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.enableNotifyUpdates(on)
                    }
                }
            )
            if (vm.notificationsHint) {
                Text(
                    "Aviso dentro de la app: las notificaciones están desactivadas en el sistema.".tr(),
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.warning
                )
            }
            Text(
                "Se comprueba una vez al día si hay una versión nueva. Solo se descarga el archivo de versión del proyecto; no se envía ningún dato.".tr(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { vm.checkUpdates(silent = false) },
                    enabled = !vm.updateChecking,
                    modifier = Modifier.weight(1f)
                ) { Text("Buscar actualizaciones".tr()) }
                if (vm.updateAvailable != null) {
                    Button(
                        onClick = { vm.requestUpdateDialog() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Actualizar".tr()) }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * Version instalada leida de BuildConfig.
 */
private object BuildConfigVersion {
    val name: String = com.calculadora.consumocij.BuildConfig.VERSION_NAME
    val code: String = com.calculadora.consumocij.BuildConfig.VERSION_CODE.toString()
}
