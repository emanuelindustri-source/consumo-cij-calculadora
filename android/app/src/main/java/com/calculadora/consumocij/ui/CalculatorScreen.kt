package com.calculadora.consumocij.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calculadora.consumocij.CalculatorViewModel
import com.calculadora.consumocij.data.CalcEngine
import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.CalcResult
import com.calculadora.consumocij.data.TemperatureModel
import com.calculadora.consumocij.data.VideojetData
import com.calculadora.consumocij.ui.theme.LocalExtraColors
import com.calculadora.consumocij.ui.theme.ThemeMode
import com.calculadora.consumocij.util.Fmt
import com.calculadora.consumocij.util.Report
import java.util.Locale

/* ============================================================
   PANTALLA PRINCIPAL — adaptable a:
   · teléfonos compactos · phablets · tabletas · TV · apaisado
   · pantallas LCD/IPS (tema claro/oscuro) y AMOLED (negro puro)
   ============================================================ */

/** Tamaños de cartucho válidos para la tinta (fuente: CIJ TCO Calculator v35 · SetupSheet). */
private fun cartSizesFor(ink: String): Pair<List<Double>, List<Double>> {
    val ic = VideojetData.inkContainers[ink]
    return (ic?.inkSizes?.map { it.toDouble() } ?: emptyList()) to
        (ic?.muSizes?.map { it.toDouble() } ?: emptyList())
}

/** Mantiene el tamaño actual si la tinta lo ofrece; si no, usa el primero disponible. */
private fun keepOrFirst(current: Double, valid: List<Double>): Double =
    if (valid.isEmpty() || valid.any { kotlin.math.abs(it - current) < 0.5 }) current else valid.first()

@Composable
private fun NoOfficialDataNote() {
    InfoBox(
        text = "Sin datos oficiales para la base de esta tinta: el CIJ Makeup Usage Model v14 devuelve " +
            "\u0023N/D para V4238 (MEK / Methanol) y V4251 (Methanol / Water). El consumo mostrado es una " +
            "estimación con base MEK, no un dato oficial.",
        color = MaterialTheme.colorScheme.errorContainer
    )
}

@Composable
fun CalculatorScreen(vm: CalculatorViewModel) {
    val input = vm.input
    val result = vm.result

    // ancho disponible → 1 columna (teléfono) o 2 columnas (tableta/apaisado)
    BoxWithConstraintsCompat { wide ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            AppHeader(
                themeMode = vm.themeMode,
                onTheme = { vm.setTheme(it) },
            )

            val scroll = rememberScrollState()
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .verticalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        EquipmentSection(vm)
                        InkSection(vm)
                        MessageSection(vm)
                        ProductionSection(vm)
                        AdvancedSection(vm)
                        LegalSection()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!result.officialData) NoOfficialDataNote()
                        ResultsSection(vm, input, result)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    EquipmentSection(vm)
                    InkSection(vm)
                    MessageSection(vm)
                    ProductionSection(vm)
                    if (!result.officialData) NoOfficialDataNote()
                    ResultsSection(vm, input, result)
                    AdvancedSection(vm)
                    LegalSection()
                }
            }
        }
    }
}

/** Detecta si la pantalla admite dos columnas (>= 840 dp de ancho útil). */
@Composable
private fun BoxWithConstraintsCompat(content: @Composable (wide: Boolean) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        content(maxWidth >= 840.dp)
    }
}

/* ---------------- cabecera ---------------- */

@Composable
private fun AppHeader(
    themeMode: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Consumo de Tinta y Solvente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Herramienta de apoyo no oficial · equipos CIJ (Simplicity / Serie 1000VJ) · 100 % offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            OutlinedButton(onClick = { menuOpen = true }) {
                Text(themeLabel(themeMode), style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(themeLabel(mode)) },
                        onClick = { onTheme(mode); menuOpen = false }
                    )
                }
            }
        }
    }
}

private fun themeLabel(m: ThemeMode): String = when (m) {
    ThemeMode.SYSTEM -> "Auto"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Oscuro"
    ThemeMode.AMOLED -> "AMOLED"
}

/* ---------------- secciones de entrada ---------------- */

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = LocalExtraColors.current.accent)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
private fun EquipmentSection(vm: CalculatorViewModel) {
    val input = vm.input
    var familyFilter by remember { mutableStateOf("all") }

    SectionCard(
        title = "Equipo impresor",
        icon = Icons.Filled.Info,
        subtitle = "Seleccione familia, modelo y boquilla. Los datos oficiales se cargan automáticamente."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "Todos", "simplicity" to "Simplicity", "serie1000" to "Serie 1000VJ").forEach { (k, label) ->
                FilterChip(
                    selected = familyFilter == k,
                    onClick = { familyFilter = k },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        val models = VideojetData.models.filter { familyFilter == "all" || it.platform == familyFilter }
        DropdownField(
            label = "Modelo de impresora",
            options = models.map { it.id },
            selected = input.modelId,
            onSelect = { id ->
                vm.update { cur ->
                    val m = CalcEngine.model(id)
                    val (inkSizes, muSizes) = cartSizesFor(cur.ink)
                    cur.copy(
                        modelId = id,
                        nozzle = m.nozzles.firstOrNull() ?: cur.nozzle,
                        inkCartMl = keepOrFirst(cur.inkCartMl, inkSizes),
                        muCartMl = keepOrFirst(cur.muCartMl, muSizes)
                    )
                }
            }
        )

        val model = CalcEngine.model(input.modelId)
        DropdownField(
            label = "Boquilla (µm)",
            options = model.nozzles,
            selected = input.nozzle,
            onSelect = { nz ->
                vm.update { cur ->
                    val (inkSizes, muSizes) = cartSizesFor(cur.ink)
                    cur.copy(
                        nozzle = nz,
                        inkCartMl = keepOrFirst(cur.inkCartMl, inkSizes),
                        muCartMl = keepOrFirst(cur.muCartMl, muSizes)
                    )
                }
            }
        )

        val ic = VideojetData.inkContainers[input.ink]
        val inkSizes = ic?.inkSizes?.takeIf { it.isNotEmpty() }?.map { it.toDouble() }
            ?: listOf(input.inkCartMl)
        val muSizes = ic?.muSizes?.takeIf { it.isNotEmpty() }?.map { it.toDouble() }
            ?: listOf(input.muCartMl)

        DropdownField(
            label = "Cartucho de tinta (según la tinta)",
            options = inkSizes.map { "${Fmt.n(it, 0)} ml" },
            selected = "${Fmt.n(input.inkCartMl, 0)} ml",
            onSelect = { s ->
                vm.update { cur ->
                    cur.copy(inkCartMl = s.removeSuffix(" ml").replace(",", "").toDoubleOrNull() ?: cur.inkCartMl)
                }
            }
        )
        DropdownField(
            label = "Cartucho de solvente (make-up)",
            options = muSizes.map { "${Fmt.n(it, 0)} ml" },
            selected = "${Fmt.n(input.muCartMl, 0)} ml",
            onSelect = { s ->
                vm.update { cur ->
                    cur.copy(muCartMl = s.removeSuffix(" ml").replace(",", "").toDoubleOrNull() ?: cur.muCartMl)
                }
            }
        )

        val mnt = VideojetData.maintenance[model.id]
        val variant = CalcEngine.variant(model, input.nozzle)
        InfoBox(
            text = buildString {
                append("${model.id} — familia ")
                append(if (model.platform == "simplicity") "Simplicity" else "Serie 1000VJ")
                append(" · boquilla ${input.nozzle} µm\n")
                append("1 L de solvente dura ${Fmt.i(variant.hrsL)} h a 20 °C (${Fmt.n(1000.0 / variant.hrsL, 2)} ml/h)")
                if (mnt != null) {
                    append("\nMantenimiento: ")
                    append(
                        mnt.joinToString(" · ") { item ->
                            val pn = if (item.pn.isNotEmpty()) " (P/N ${item.pn})" else ""
                            "${item.part}$pn · ${CalcEngine.formatInterval(item.hours)}"
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun InkSection(vm: CalculatorViewModel) {
    val input = vm.input
    val model = CalcEngine.model(input.modelId)
    val inks = remember(model.inkGroup) { VideojetData.inksFor(model.inkGroup) }

    SectionCard(
        title = "Tinta y solvente",
        icon = Icons.Filled.Info,
        subtitle = "La base de solvente determina el factor de consumo y su sensibilidad térmica."
    ) {
        DropdownField(
            label = "Tipo de tinta",
            options = inks,
            selected = if (inks.contains(input.ink)) input.ink else (inks.firstOrNull() ?: input.ink),
            onSelect = { ink ->
                vm.update { cur ->
                    val (inkSizes, muSizes) = cartSizesFor(ink)
                    cur.copy(
                        ink = ink,
                        inkBase = VideojetData.inks[ink] ?: cur.inkBase,
                        inkCartMl = keepOrFirst(cur.inkCartMl, inkSizes),
                        muCartMl = keepOrFirst(cur.muCartMl, muSizes)
                    )
                }
            }
        )
        DropdownField(
            label = "Base de solvente / make-up",
            options = VideojetData.inkBases.keys.toList(),
            selected = input.inkBase,
            onSelect = { b -> vm.update { it.copy(inkBase = b) } }
        )
        val base = VideojetData.inkBases[input.inkBase]
        val ic = VideojetData.inkContainers[input.ink]
        if (base != null) {
            InfoBox(
                text = "Base ${input.inkBase} — factor de consumo ${Fmt.n(base.cons, 3)} · " +
                    "factor exponencial ${Fmt.n(base.k[0] * 100, 2)} %/°C" +
                    if (ic != null && ic.muCode.isNotEmpty()) " · solvente asociado ${ic.muCode}" else ""
            )
        } else {
            InfoBox(
                text = "La base «${input.inkBase}» no tiene factor oficial en el Makeup Usage Model v14 " +
                    "(la ficha original da #N/D). El cálculo usará MEK como estimación.",
                color = MaterialTheme.colorScheme.errorContainer
            )
        }
    }
}

@Composable
private fun MessageSection(vm: CalculatorViewModel) {
    val input = vm.input
    SectionCard(
        title = "Mensaje / código impreso",
        icon = Icons.Filled.Info,
        subtitle = "La matriz de la fuente define las gotas por carácter."
    ) {
        val fontNames = VideojetData.fonts.keys.toList()
        DropdownField(
            label = "Tipo de fuente",
            options = fontNames,
            selected = input.font,
            onSelect = { f ->
                vm.update {
                    it.copy(font = f, lines = VideojetData.fonts[f]?.lines ?: it.lines)
                }
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Caracteres/línea", value = input.chars.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(chars = v.toInt().coerceIn(1, 500)) } }
            NumberField(
                label = "Líneas", value = input.lines.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(lines = v.toInt().coerceIn(1, 5)) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Stroke rate", value = input.stroke.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(stroke = v.toInt().coerceIn(1, 4)) } }
            NumberField(
                label = "Ancho impresión", value = input.widthMm, suffix = "mm",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(widthMm = v) } }
        }
    }
}

@Composable
private fun ProductionSection(vm: CalculatorViewModel) {
    val input = vm.input
    SectionCard(
        title = "Producción y temperatura",
        icon = Icons.Filled.Info,
        subtitle = "El solvente se consume por hora de encendido; la tinta, por carácter impreso."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Temperatura", value = input.tempC, suffix = "°C",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(tempC = v) } }
            NumberField(
                label = "Impresiones/min", value = input.ppm,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(ppm = v) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Horas/día", value = input.hoursDay, suffix = "h",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(hoursDay = v) } }
            NumberField(
                label = "Días/semana", value = input.daysWeek.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(daysWeek = v.toInt().coerceIn(1, 7)) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Días/mes", value = input.daysMonth.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(daysMonth = v.toInt().coerceIn(1, 31)) } }
            NumberField(
                label = "Eficiencia", value = input.efficiencyPct, suffix = "%",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(efficiencyPct = v.coerceIn(1.0, 100.0)) } }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Velocidad línea", value = input.lineSpeed, suffix = "m/min",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(lineSpeed = v) } }
            NumberField(
                label = "Ancho producto", value = input.productWidthMm, suffix = "mm",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(productWidthMm = v) } }
            NumberField(
                label = "Hueco", value = input.gapMm, suffix = "mm",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(gapMm = v) } }
        }
    }
}

@Composable
private fun AdvancedSection(vm: CalculatorViewModel) {
    val input = vm.input
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mantenimiento y ajustes avanzados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
    if (expanded) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Vida en almacén", value = input.shelfLifeMonths, suffix = "meses",
                        modifier = Modifier.weight(1f)
                    ) { v -> vm.update { it.copy(shelfLifeMonths = v) } }
                    NumberField(
                        label = "Núcleo (horas)", value = input.coreHours, suffix = "h",
                        modifier = Modifier.weight(1f)
                    ) { v -> vm.update { it.copy(coreHours = v) } }
                    NumberField(
                        label = "Núcleo (días)", value = input.coreDays, suffix = "días",
                        modifier = Modifier.weight(1f)
                    ) { v -> vm.update { it.copy(coreDays = v) } }
                }
                DropdownField(
                    label = "Modelo de temperatura del solvente",
                    options = listOf("Publicado (Makeup Usage v14)", "Por ecuación del equipo (TCO v35)"),
                    selected = if (input.temperatureModel == TemperatureModel.PUBLISHED)
                        "Publicado (Makeup Usage v14)" else "Por ecuación del equipo (TCO v35)",
                    onSelect = { s ->
                        vm.update {
                            it.copy(
                                temperatureModel = if (s.startsWith("Publicado"))
                                    TemperatureModel.PUBLISHED else TemperatureModel.EQUATION
                            )
                        }
                    }
                )
                NumberField(
                    label = "Factor ambiental (humedad, recirculación, condensador, boquilla)",
                    value = input.envFactor
                ) { v -> vm.update { it.copy(envFactor = v.coerceIn(0.2, 3.0)) } }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.weight(1f)) {
                        Text("Restablecer")
                    }
                    OutlinedButton(onClick = { vm.addHistory() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

/* ---------------- resultados ---------------- */

@Composable
private fun ResultsSection(vm: CalculatorViewModel, input: CalcInput, r: CalcResult) {
    val context = LocalContext.current

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(("\uFEFF" + Report.csv(input, r)).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Informe CSV guardado", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "No se pudo guardar el archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(Report.txt(input, r).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Informe TXT guardado", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "No se pudo guardar el archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // hero
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "CONSUMO ESTIMADO DE SOLVENTE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Fmt.n(r.muPerHour, 2),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    " ml/hora",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MiniStat("Tinta", "${Fmt.n(r.inkPerHour, 2)} ml/h")
                MiniStat("Relación S:T", "${Fmt.n(r.ratio, 2)} : 1")
                MiniStat("Cart. solvente", "${Fmt.n(r.hoursPerMuCart, 0)} h")
            }
        }
    }

    // KPIs
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Solvente / mes", "${Fmt.i(r.muMonth)} ml", "≈ ${Fmt.n(r.muMonth / 1000.0, 2)} L", Modifier.weight(1f))
        KpiCard("Tinta / mes", "${Fmt.i(r.inkMonth)} ml", "≈ ${Fmt.n(r.inkMonth / 1000.0, 2)} L", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Cart. solvente/mes", Fmt.n(r.muCartMonth, 1), "${Fmt.n(r.hoursPerMuCart, 0)} h c/u", Modifier.weight(1f))
        KpiCard("Cart. tinta/mes", Fmt.n(r.inkCartMonth, 1), "${Fmt.i(r.printsPerInkCart)} imp. c/u", Modifier.weight(1f))
    }

    // tabla de detalle
    SectionCard(title = "Detalle del cálculo", icon = Icons.Filled.Info) {
        DetailRow("Consumo solvente", r.muPerHour, r.muDay, r.muMonth, r.muYear, "ml")
        DetailRow("Consumo tinta", r.inkPerHour, r.inkDay, r.inkMonth, r.inkYear, "ml")
        DetailRow("Impresiones", r.printsPerHour, r.printsPerHour * input.hoursDay, r.printsPerHour * r.hoursMonth, r.printsPerHour * r.hoursYear, "")
        DetailRow(
            "Gotas de tinta",
            r.dropsPerMessage * r.printsPerHour,
            r.dropsPerMessage * r.printsPerHour * input.hoursDay,
            r.dropsPerMessage * r.printsPerHour * r.hoursMonth,
            r.dropsPerMessage * r.printsPerHour * r.hoursYear,
            "gotas"
        )
        Spacer(Modifier.height(8.dp))
        InfoBox("Relación solvente : tinta = ${Fmt.n(r.ratio, 2)} : 1  (regla práctica 4 : 1)")
    }

    // gráficos
    SectionCard(title = "Solvente vs temperatura", icon = Icons.Filled.Info) {
        val curve = remember(input) { CalcEngine.makeupCurve(input) }
        TempCurveChart(points = curve, currentTemp = input.tempC, heightDp = 220)
    }
    SectionCard(title = "Tinta vs solvente mensual", icon = Icons.Filled.Info) {
        BarsChart(solventMonth = r.muMonth, inkMonth = r.inkMonth, heightDp = 200)
    }
    SectionCard(title = "Consumo acumulado en la jornada", icon = Icons.Filled.Info) {
        TimelineChart(
            muPerHour = r.muPerHour,
            inkPerHour = r.inkPerHour,
            hoursDay = input.hoursDay,
            cartMl = input.muCartMl,
            heightDp = 200
        )
    }

    // validación / avisos
    SectionCard(title = "Avisos y validación", icon = Icons.Filled.Warning) {
        AlertsList(input, r)
    }

    // mantenimiento
    SectionCard(title = "Mantenimiento (núcleo / kit)", icon = Icons.Filled.Info) {
        val mnt = VideojetData.maintenance[r.model.id]
        if (mnt != null && mnt.isNotEmpty()) {
            mnt.forEach { item ->
                val pn = if (item.pn.isNotEmpty()) " · P/N ${item.pn}" else ""
                InfoBox("${item.part} — ${CalcEngine.formatInterval(item.hours)}$pn")
            }
        }
        if (input.coreHours > 0 && r.hoursMonth > 0) {
            val pctYear = r.hoursYear / input.coreHours * 100.0
            ProgressLine(
                label = "Vida del núcleo consumida al año",
                pct = pctYear,
                detail = "cambio cada ${Fmt.n(input.coreHours / r.hoursMonth, 1)} meses de producción"
            )
        }
        if (input.coreDays > 0 && input.daysWeek > 0) {
            val pctYear = input.daysWeek * 52.0 / input.coreDays * 100.0
            ProgressLine(
                label = "Vida por días consumida al año",
                pct = pctYear,
                detail = "cambio cada ${Fmt.n(input.coreDays / (input.daysWeek * 4.345), 1)} meses"
            )
        }
    }

    // exportar
    SectionCard(title = "Exportar resultado", icon = Icons.Filled.Share) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { csvLauncher.launch("Consumo_Videojet_${Report.stamp()}.csv") },
                modifier = Modifier.weight(1f)
            ) { Text("CSV") }
            OutlinedButton(
                onClick = { txtLauncher.launch("Consumo_Videojet_${Report.stamp()}.txt") },
                modifier = Modifier.weight(1f)
            ) { Text("TXT") }
        }
    }

    // historial
    SectionCard(title = "Historial de cálculos", icon = Icons.Filled.Save) {
        if (vm.history.isEmpty()) {
            Text(
                "Todavía no hay cálculos guardados en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            vm.history.forEach { e ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${e.result.model.id} · ${e.input.font} · ${Fmt.n(e.input.tempC, 1)} °C",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${e.date} · solvente ${Fmt.n(e.result.muPerHour, 2)} ml/h · tinta ${Fmt.n(e.result.inkPerHour, 2)} ml/h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { vm.restoreFromHistory(e.id) }) { Text("Cargar") }
                    IconButton(onClick = { vm.removeHistory(e.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun KpiCard(title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(label: String, h: Double, d: Double, m: Double, y: Double, unit: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            if (unit.isNotEmpty()) "$label ($unit)" else label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniCol("Hora", h, Modifier.weight(1f))
            MiniCol("Día", d, Modifier.weight(1f))
            MiniCol("Mes", m, Modifier.weight(1f))
            MiniCol("Año", y, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniCol(label: String, v: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            compactNumber(v),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Números grandes en notación compacta (evita saltos de línea en pantallas estrechas). */
private fun compactNumber(v: Double): String {
    val a = kotlin.math.abs(v)
    return when {
        !v.isFinite() -> "—"
        a >= 1e9 -> Fmt.n(v / 1e9, 2) + " B"
        a >= 1e6 -> Fmt.n(v / 1e6, 2) + " M"
        a >= 1e4 -> Fmt.n(v / 1e3, 1) + " k"
        a >= 1000 -> Fmt.i(v)
        else -> Fmt.n(v, 2)
    }
}

@Composable
private fun InfoBox(text: String, color: androidx.compose.ui.graphics.Color? = null) {
    val extra = LocalExtraColors.current
    val bg = color ?: extra.accent.copy(alpha = 0.10f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProgressLine(label: String, pct: Double, detail: String) {
    val extra = LocalExtraColors.current
    val capped = pct.coerceIn(0.0, 100.0).toFloat() / 100f
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${Fmt.n(pct, 0)} %", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(extra.chartGrid)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(capped)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (pct > 100) extra.danger else if (pct > 75) extra.warning else extra.success)
            )
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlertsList(input: CalcInput, r: CalcResult) {
    val extra = LocalExtraColors.current
    val alerts = ArrayList<Triple<ImageVector, androidx.compose.ui.graphics.Color, String>>()

    if (r.ratio.isFinite()) {
        when {
            r.ratio in 3.0..5.0 -> alerts.add(
                Triple(Icons.Filled.CheckCircle, extra.success,
                    "Relación solvente : tinta = ${Fmt.n(r.ratio, 2)} : 1, dentro de la regla práctica de 4 : 1.")
            )
            r.ratio > 5.0 -> alerts.add(
                Triple(Icons.Filled.Warning, extra.warning,
                    "Relación solvente : tinta = ${Fmt.n(r.ratio, 2)} : 1, superior a 4 : 1. Suele deberse a pocas impresiones/min o muchas horas de encendido.")
            )
            else -> alerts.add(
                Triple(Icons.Filled.Info, extra.accent2,
                    "Relación solvente : tinta = ${Fmt.n(r.ratio, 2)} : 1, inferior a 4 : 1: línea con alta actividad de impresión.")
            )
        }
    }
    if (input.tempC >= 32) {
        alerts.add(
            Triple(Icons.Filled.Warning, extra.warning,
                "Temperatura de ${Fmt.n(input.tempC, 1)} °C: el consumo de solvente se dispara. Considere ventilación o condensador activo.")
        )
    } else if (input.tempC <= 12) {
        alerts.add(
            Triple(Icons.Filled.Info, extra.accent2,
                "Temperatura baja (${Fmt.n(input.tempC, 1)} °C): menor evaporación, vigile la viscosidad de la tinta.")
        )
    }
    alerts.add(
        Triple(Icons.Filled.Info, extra.accent,
            "Un litro de solvente dura ${Fmt.i(1000.0 / r.muPerHour)} h y un cartucho de ${Fmt.i(input.muCartMl)} ml dura ${Fmt.i(r.hoursPerMuCart)} h.")
    )
    alerts.add(
        Triple(Icons.Filled.Info, extra.accent,
            "Tinta: ${Fmt.n(r.inkMlPerPrint * 1000.0, 2)} µl por impresión (${Fmt.i(r.dropsPerMessage)} gotas · boquilla ${input.nozzle} µm).")
    )
    if (input.shelfLifeMonths > 0) {
        val months = if (r.muMonth > 0) input.muCartMl / r.muMonth else 0.0
        if (months > input.shelfLifeMonths) {
            alerts.add(
                Triple(Icons.Filled.Warning, extra.warning,
                    "Un cartucho tarda ${Fmt.n(months, 1)} meses en gastarse y su vida en almacén es de ${Fmt.i(input.shelfLifeMonths)} meses: riesgo de caducidad.")
            )
        }
    }
    r.maxLineSpeed?.let { max ->
        if (input.lineSpeed > max) {
            alerts.add(
                Triple(Icons.Filled.Error, extra.danger,
                    "La velocidad de ${Fmt.n(input.lineSpeed, 0)} m/min supera la máxima (${Fmt.n(max, 1)} m/min) para ${input.font} con ${input.chars} caracteres y ${Fmt.n(input.widthMm, 1)} mm.")
            )
        } else {
            alerts.add(
                Triple(Icons.Filled.CheckCircle, extra.success,
                    "Velocidad válida: máx. ${Fmt.n(max, 1)} m/min para ${input.font} · ${input.chars} car. · ${Fmt.n(input.widthMm, 1)} mm.")
            )
        }
    }
    r.maxPpm?.let { maxPpm ->
        if (input.ppm > maxPpm) {
            alerts.add(
                Triple(Icons.Filled.Warning, extra.warning,
                    "Las ${Fmt.n(input.ppm, 0)} impresiones/min superan las ${Fmt.n(maxPpm, 0)} máximas para esa velocidad de línea.")
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { (icon, color, text) ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = color)
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/* ---------------- fuentes de datos ---------------- */

@Composable
private fun LegalSection() {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = LocalExtraColors.current.accent)
            Spacer(Modifier.width(10.dp))
            Text(
                "Fuentes de datos y notas técnicas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
    if (expanded) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Los cálculos proceden de los modelos oficiales: CIJ Makeup Usage Model v14 (solvente por " +
                        "temperatura), CIJ Line Speed Calculator v9.7 (velocidades, cpi y dpi) y CIJ TCO Calculator v35 " +
                        "(gotas/litro, gotas/carácter, contenedores y mantenimiento). Contrastados con 734 fichas " +
                        "oficiales de solvente y 535 de velocidad de línea.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Los tamaños de cartucho varían según la tinta y el equipo (p. ej. V4225 = 700 ml, V480 = 600 ml, " +
                        "V401 = 750/5000 ml); el solvente asociado —V7201, V7257, V7220…— se indica junto a cada tinta.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Estimación técnica: contraste los valores con el consumo real del equipo antes de planificar " +
                        "pedidos de consumibles.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Versión 2.0 · herramienta técnica interna · funciona sin internet · familias Simplicity y Serie 1000VJ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ---------------- controles ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Double,
    suffix: String? = null,
    isInt: Boolean = false,
    modifier: Modifier = Modifier,
    onValueChange: (Double) -> Unit
) {
    var text by remember { mutableStateOf(formatPlain(value, isInt)) }
    var lastValue by remember { mutableStateOf(value) }
    if (value != lastValue) {
        text = formatPlain(value, isInt)
        lastValue = value
    }
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            val parsed = newText.replace(',', '.').toDoubleOrNull()
            if (parsed != null) {
                onValueChange(parsed)
                lastValue = parsed
            }
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInt) KeyboardType.Number else KeyboardType.Decimal
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    )
}

private fun formatPlain(v: Double, isInt: Boolean): String =
    if (isInt) v.toInt().toString()
    else if (v == v.toLong().toDouble()) v.toLong().toString()
    else String.format(Locale.US, "%.2f", v)
