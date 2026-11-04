package com.calculadora.consumocij.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculadora.consumocij.CalculatorViewModel
import com.calculadora.consumocij.i18n.I18n
import com.calculadora.consumocij.i18n.tr
import com.calculadora.consumocij.ui.theme.LocalExtraColors

/**
 * Copia de seguridad: exporta e importa configuración, historial, tema e idioma
 * en un JSON local. Usa el selector de archivos del sistema (sin permisos).
 */
@Composable
fun BackupSection(vm: CalculatorViewModel) {
    val context = LocalContext.current
    val extra = LocalExtraColors.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = vm.exportBackup(context, uri)
            val msg = if (ok) "Copia exportada".tr() else "No se pudo guardar el archivo".tr()
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val count = vm.importBackup(context, uri)
            if (count != null) {
                Toast.makeText(
                    context,
                    I18n.tf("Copia importada: {0} cálculos", count),
                    Toast.LENGTH_LONG
                ).show()
                (context as? Activity)?.recreate()
            } else {
                Toast.makeText(
                    context,
                    "El archivo no es una copia válida de Consumo CIJ.".tr(),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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
                Icon(Icons.Filled.Backup, contentDescription = null, tint = extra.accent)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Copia de seguridad".tr(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Guarde la configuración, el historial y el tema en un archivo JSON. Sirve para cambiar de dispositivo o recuperar sus datos.".tr(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { exportLauncher.launch("ConsumoCIJ-copia.json") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Exportar historial y configuración".tr()) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importar copia de seguridad".tr()) }
        }
    }
}
