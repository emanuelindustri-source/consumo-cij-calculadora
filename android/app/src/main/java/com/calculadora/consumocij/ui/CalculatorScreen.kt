package com.calculadora.consumocij.ui

import android.widget.Toast
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.calculadora.consumocij.i18n.I18n
import com.calculadora.consumocij.i18n.tr
import com.calculadora.consumocij.util.ReportIssue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
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
import com.calculadora.consumocij.BuildConfig
import com.calculadora.consumocij.data.CalcEngine
import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.CalcResult
import com.calculadora.consumocij.data.Comparator
import com.calculadora.consumocij.data.CompareColumn
import com.calculadora.consumocij.data.Scenario
import com.calculadora.consumocij.data.TemperatureModel
import com.calculadora.consumocij.data.VideojetData
import com.calculadora.consumocij.ui.theme.LocalExtraColors
import com.calculadora.consumocij.ui.theme.ThemeMode
import com.calculadora.consumocij.util.Fmt
import com.calculadora.consumocij.util.Money
import com.calculadora.consumocij.util.Report
import java.util.Locale

/* ============================================================
   PANTALLA PRINCIPAL — adaptable a:
   · teléfonos compactos · phablets · tabletas · TV · apaisado
   · pantallas LCD/IPS (tema claro/oscuro) y AMOLED (negro puro)
   ============================================================ */

/** Tamaños de cartucho válidos para el modelo y la tinta (CIJ TCO Calculator v35). */
private fun cartSizesFor(modelId: String, ink: String): Pair<List<Double>, List<Double>> =
    VideojetData.inkSizesFor(modelId, ink).map { it.toDouble() } to
        VideojetData.muSizesFor(modelId, ink).map { it.toDouble() }

/** Mantiene el tamaño actual si la tinta lo ofrece; si no, usa el primero disponible. */
private fun keepOrFirst(current: Double, valid: List<Double>): Double =
    if (valid.isEmpty() || valid.any { kotlin.math.abs(it - current) < 0.5 }) current else valid.first()

@Composable
private fun NoOfficialDataNote() {
    InfoBox(
        text = "Sin datos oficiales para la base de esta tinta: el CIJ Makeup Usage Model v14 devuelve #N/D para V4238 (MEK / Methanol) y V4251 (Methanol / Water). El consumo mostrado es una estimación con base MEK, no un dato oficial.".tr(),
        color = MaterialTheme.colorScheme.errorContainer
    )
}

@Composable
fun CalculatorScreen(vm: CalculatorViewModel) {
    val input = vm.input
    val result = vm.result
    val context = LocalContext.current

    // Avisos de actualización (diálogo, permiso de notificaciones y aviso superior).
    UpdateDialogHost(vm)
    // El permiso de notificaciones se pide después de cerrar el diálogo de versión.
    if (!vm.updateDialogOpen) NotificationsPromptHost(vm)

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
                lang = I18n.preference,
                onLang = { tag ->
                    I18n.setLanguage(context, tag)
                    (context as? android.app.Activity)?.recreate()
                },
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
                        if (vm.updateBannerVisible) UpdateBanner(vm)
                        EquipmentSection(vm)
                        InkSection(vm)
                        MessageSection(vm)
                        ProductionSection(vm)
                        PriceSection(vm)
                        AdvancedSection(vm)
                        BackupSection(vm)
                        WhatsNewSection()
                        UpdateSection(vm)
                        LegalSection()
                    ReportSection(vm)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!result.officialData) NoOfficialDataNote()
                        ResultsSection(vm, input, result)
                        ComparatorSection(vm)
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
                    if (vm.updateBannerVisible) UpdateBanner(vm)
                    EquipmentSection(vm)
                    InkSection(vm)
                    MessageSection(vm)
                    ProductionSection(vm)
                    PriceSection(vm)
                    if (!result.officialData) NoOfficialDataNote()
                    ResultsSection(vm, input, result)
                    ComparatorSection(vm)
                    AdvancedSection(vm)
                    BackupSection(vm)
                    WhatsNewSection()
                    UpdateSection(vm)
                    LegalSection()
                    ReportSection(vm)
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
    lang: String,
    onLang: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    var langOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            "Consumo de Tinta y Solvente".tr(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Herramienta de apoyo no oficial · equipos CIJ (Simplicity / Serie 1000VJ) · funciona sin internet".tr(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(
                    onClick = { langOpen = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = I18n.t("Idioma de la aplicación"),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        I18n.labelOf(lang),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp)
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = langOpen, onDismissRequest = { langOpen = false }) {
                    I18n.LANGUAGES.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(I18n.labelOf(tag)) },
                            leadingIcon = if (tag == lang) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                            onClick = { if (tag != lang) onLang(tag); langOpen = false }
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box {
                OutlinedButton(
                    onClick = { menuOpen = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
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
}

private fun themeLabel(m: ThemeMode): String = when (m) {
    ThemeMode.SYSTEM -> "Auto".tr()
    ThemeMode.LIGHT -> "Claro".tr()
    ThemeMode.DARK -> "Oscuro".tr()
    ThemeMode.AMOLED -> "AMOLED".tr()
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
        title = "Equipo impresor".tr(),
        icon = Icons.Filled.Info,
        subtitle = "Seleccione familia, modelo y boquilla. Los datos oficiales se cargan automáticamente.".tr()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "Todos".tr(), "simplicity" to "Simplicity", "serie1000" to "Serie 1000VJ").forEach { (k, label) ->
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
            label = "Modelo de impresora".tr(),
            options = models.map { it.id },
            selected = input.modelId,
            onSelect = { id ->
                vm.update { cur ->
                    val m = CalcEngine.model(id)
                    val inks = VideojetData.inksFor(m.inkGroup)
                    val newInk = if (cur.ink in inks) cur.ink else (inks.firstOrNull() ?: cur.ink)
                    val newBase = VideojetData.inks[newInk] ?: cur.inkBase
                    val (inkSizes, muSizes) = cartSizesFor(id, newInk)
                    cur.copy(
                        modelId = id,
                        ink = newInk,
                        inkBase = newBase,
                        nozzle = m.nozzles.firstOrNull() ?: cur.nozzle,
                        inkCartMl = keepOrFirst(cur.inkCartMl, inkSizes),
                        muCartMl = keepOrFirst(cur.muCartMl, muSizes)
                    )
                }
            }
        )

        val model = CalcEngine.model(input.modelId)
        DropdownField(
            label = "Boquilla (µm)".tr(),
            options = model.nozzles,
            selected = input.nozzle,
            onSelect = { nz ->
                vm.update { cur ->
                    val (inkSizes, muSizes) = cartSizesFor(cur.modelId, cur.ink)
                    cur.copy(
                        nozzle = nz,
                        inkCartMl = keepOrFirst(cur.inkCartMl, inkSizes),
                        muCartMl = keepOrFirst(cur.muCartMl, muSizes)
                    )
                }
            }
        )

        val inkSizes = VideojetData.inkSizesFor(input.modelId, input.ink).map { it.toDouble() }
            .takeIf { it.isNotEmpty() } ?: listOf(input.inkCartMl)
        val muSizes = VideojetData.muSizesFor(input.modelId, input.ink).map { it.toDouble() }
            .takeIf { it.isNotEmpty() } ?: listOf(input.muCartMl)

        DropdownField(
            label = "Cartucho de tinta (según la tinta)".tr(),
            options = inkSizes.map { "${Fmt.n(it, 0)} ml" },
            selected = "${Fmt.n(input.inkCartMl, 0)} ml",
            onSelect = { s ->
                vm.update { cur ->
                    cur.copy(inkCartMl = s.removeSuffix(" ml").replace(",", "").toDoubleOrNull() ?: cur.inkCartMl)
                }
            }
        )
        DropdownField(
            label = "Cartucho de solvente (make-up)".tr(),
            options = muSizes.map { "${Fmt.n(it, 0)} ml" },
            selected = "${Fmt.n(input.muCartMl, 0)} ml",
            onSelect = { s ->
                vm.update { cur ->
                    cur.copy(muCartMl = s.removeSuffix(" ml").replace(",", "").toDoubleOrNull() ?: cur.muCartMl)
                }
            }
        )

        if (input.modelId in VideojetData.only750Models) {
            InfoBox(
                text = "Este modelo usa exclusivamente cartuchos de 750 ml (CIJ TCO Calculator v35).".tr(),
                color = LocalExtraColors.current.warning.copy(alpha = 0.12f)
            )
        }

        val mnt = VideojetData.maintenance[model.id]
        val variant = CalcEngine.variant(model, input.nozzle)
        InfoBox(
            text = buildString {
                append(I18n.tf("{0} · familia {1} · boquilla {2} µm", model.id,
                    if (model.platform == "simplicity") "Simplicity" else "Serie 1000VJ", input.nozzle))
                append("\n")
                append(I18n.tf("1 L de solvente dura {0} h a 20 °C ({1} ml/h)", Fmt.i(variant.hrsL), Fmt.n(1000.0 / variant.hrsL, 2)))
                if (mnt != null) {
                    append("\n")
                    append(I18n.t("Mantenimiento") + ": ")
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
        title = "Tinta y solvente".tr(),
        icon = Icons.Filled.Info,
        subtitle = "La base de solvente determina el factor de consumo y su sensibilidad térmica.".tr()
    ) {
        DropdownField(
            label = "Tipo de tinta".tr(),
            options = inks,
            selected = if (inks.contains(input.ink)) input.ink else (inks.firstOrNull() ?: input.ink),
            onSelect = { ink ->
                vm.update { cur ->
                    val (inkSizes, muSizes) = cartSizesFor(model.id, ink)
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
            label = "Base de solvente / make-up".tr(),
            options = VideojetData.inkBases.keys.toList(),
            selected = input.inkBase,
            onSelect = { b -> vm.update { it.copy(inkBase = b) } }
        )
        val base = VideojetData.inkBases[input.inkBase]
        val ic = VideojetData.inkContainers[input.ink]
        if (base != null) {
            InfoBox(
                text = I18n.tf(
                    "Base {0} — factor de consumo {1} · factor exponencial {2} %/°C · solvente asociado {3}",
                    input.inkBase, Fmt.n(base.cons, 3), Fmt.n(base.k[0] * 100, 2),
                    ic?.muCode?.takeIf { it.isNotEmpty() } ?: "—"
                )
            )
        } else {
            InfoBox(
                text = I18n.tf(
                    "La base «{0}» no tiene factor oficial en el Makeup Usage Model v14 "
                        + "(la ficha original da #N/D). El cálculo usará MEK como estimación.",
                    input.inkBase
                ),
                color = MaterialTheme.colorScheme.errorContainer
            )
        }
    }
}

@Composable
private fun MessageSection(vm: CalculatorViewModel) {
    val input = vm.input
    SectionCard(
        title = "Mensaje / código impreso".tr(),
        icon = Icons.Filled.Info,
        subtitle = "La matriz de la fuente define las gotas por carácter.".tr()
    ) {
        val fontNames = VideojetData.fonts.keys.toList()
        DropdownField(
            label = "Tipo de fuente".tr(),
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
                label = "Caracteres/línea".tr(), value = input.chars.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(chars = v.toInt().coerceIn(1, 500)) } }
            NumberField(
                label = "Líneas".tr(), value = input.lines.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(lines = v.toInt().coerceIn(1, 5)) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Stroke rate".tr(), value = input.stroke.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(stroke = v.toInt().coerceIn(1, 4)) } }
            NumberField(
                label = "Ancho de impresión".tr(), value = input.widthMm, suffix = "mm",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(widthMm = v) } }
        }
    }
}

@Composable
private fun ProductionSection(vm: CalculatorViewModel) {
    val input = vm.input
    SectionCard(
        title = "Producción y temperatura".tr(),
        icon = Icons.Filled.Info,
        subtitle = "El solvente se consume por hora de encendido; la tinta, por carácter impreso.".tr()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Temperatura".tr(), value = input.tempC, suffix = "°C",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(tempC = v) } }
            NumberField(
                label = "Impresiones/min", value = input.ppm,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(ppm = v) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Horas/día".tr(), value = input.hoursDay, suffix = "h",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(hoursDay = v) } }
            NumberField(
                label = "Días/semana".tr(), value = input.daysWeek.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(daysWeek = v.toInt().coerceIn(1, 7)) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Días por mes".tr(), value = input.daysMonth.toDouble(), isInt = true,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(daysMonth = v.toInt().coerceIn(1, 31)) } }
            NumberField(
                label = "Eficiencia".tr(), value = input.efficiencyPct, suffix = "%",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(efficiencyPct = v.coerceIn(1.0, 100.0)) } }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Velocidad de línea".tr(), value = input.lineSpeed, suffix = "m/min",
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(lineSpeed = v) } }
            NumberField(
                label = "Ancho producto".tr(), value = input.productWidthMm, suffix = "mm",
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
private fun PriceSection(vm: CalculatorViewModel) {
    val input = vm.input
    var tcoOpen by remember { mutableStateOf(false) }
    val cost = CalcEngine.costs(input, vm.result)

    SectionCard(
        title = "Precios y costos para ventas".tr(),
        icon = Icons.Filled.Info,
        subtitle = "Introduzca el precio de compra de cada cartucho y la moneda. La calculadora obtiene el costo por hora, por mes, por año y por cada 1.000 códigos impresos.".tr()
    ) {
        DropdownField(
            label = "Moneda".tr(),
            options = Money.CODES,
            selected = input.currency,
            onSelect = { c -> vm.update { it.copy(currency = Money.normalize(c)) } }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                label = "Precio del cartucho de tinta".tr(),
                value = input.inkPrice,
                suffix = input.currency,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(inkPrice = v.coerceIn(0.0, 1_000_000.0)) } }
            NumberField(
                label = "Precio del cartucho de solvente".tr(),
                value = input.muPrice,
                suffix = input.currency,
                modifier = Modifier.weight(1f)
            ) { v -> vm.update { it.copy(muPrice = v.coerceIn(0.0, 1_000_000.0)) } }
        }
        if (cost.inkPricePerLiter > 0 || cost.muPricePerLiter > 0) {
            InfoBox(
                buildString {
                    if (cost.inkPricePerLiter > 0) {
                        append(I18n.t("Tinta") + ": " +
                            I18n.tf("Precio por litro: {0}", Money.format(cost.inkPricePerLiter, input.currency)))
                    }
                    if (cost.muPricePerLiter > 0) {
                        if (isNotEmpty()) append("\n")
                        append(I18n.t("Solvente") + ": " +
                            I18n.tf("Precio por litro: {0}", Money.format(cost.muPricePerLiter, input.currency)))
                    }
                }
            )
        }

        OutlinedButton(
            onClick = { tcoOpen = !tcoOpen },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Costo total de propiedad (equipo + consumibles)".tr())
        }
        if (tcoOpen) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    label = "Precio del equipo".tr(),
                    value = input.equipPrice,
                    suffix = input.currency,
                    modifier = Modifier.weight(1f)
                ) { v -> vm.update { it.copy(equipPrice = v.coerceIn(0.0, 100_000_000.0)) } }
                NumberField(
                    label = "Años de comparación".tr(),
                    value = input.years.toDouble(),
                    suffix = "años".tr(),
                    isInt = true,
                    modifier = Modifier.weight(1f)
                ) { v -> vm.update { it.copy(years = v.toInt().coerceIn(1, 20)) } }
            }
            if (cost.configured && input.equipPrice > 0) {
                InfoBox(
                    I18n.tf("Costo a {0} años: {1} de consumibles + {2} del equipo = {3}.",
                        cost.years.toString(),
                        Money.format(cost.consumablesYears, input.currency),
                        Money.format(input.equipPrice, input.currency),
                        Money.format(cost.totalOwnership, input.currency))
                )
            }
        }
        InfoBox("Los precios no se envían a ningún servidor: se guardan solo en este dispositivo.".tr())
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
                "Mantenimiento y ajustes avanzados".tr(),
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
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Vida útil en almacén".tr(), value = input.shelfLifeMonths, suffix = "meses".tr(),
                        modifier = Modifier.weight(1f)
                    ) { v -> vm.update { it.copy(shelfLifeMonths = v) } }
                    NumberField(
                        label = "Vida núcleo (horas)".tr(), value = input.coreHours, suffix = "h",
                        modifier = Modifier.weight(1f)
                    ) { v -> vm.update { it.copy(coreHours = v) } }
                    NumberField(
                        label = "Vida núcleo (días)".tr(), value = input.coreDays, suffix = "días".tr(),
                        modifier = Modifier.weight(1f)
                    ) { v -> vm.update { it.copy(coreDays = v) } }
                }
                DropdownField(
                    label = "Modelo de temperatura del solvente".tr(),
                    options = listOf("Publicado (Makeup Usage v14)".tr(), "Por ecuación del equipo (TCO v35)".tr()),
                    selected = if (input.temperatureModel == TemperatureModel.PUBLISHED)
                        "Publicado (Makeup Usage v14)".tr() else "Por ecuación del equipo (TCO v35)".tr(),
                    onSelect = { s ->
                        vm.update {
                            it.copy(
                                temperatureModel = if (s.startsWith("Publicado".tr()))
                                    TemperatureModel.PUBLISHED else TemperatureModel.EQUATION
                            )
                        }
                    }
                )
                NumberField(
                    label = "Factor ambiental (humedad, recirculación, condensador, boquilla)".tr(),
                    value = input.envFactor
                ) { v -> vm.update { it.copy(envFactor = v.coerceIn(0.2, 3.0)) } }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.weight(1f)) {
                        Text("Restablecer".tr())
                    }
                    OutlinedButton(onClick = { vm.addHistory() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar".tr())
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
                Toast.makeText(context, "Informe CSV guardado".tr(), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "No se pudo guardar el archivo".tr(), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Informe TXT guardado".tr(), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "No se pudo guardar el archivo".tr(), Toast.LENGTH_SHORT).show()
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
                "CONSUMO ESTIMADO DE SOLVENTE".tr(),
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
                MiniStat("Tinta".tr(), "${Fmt.n(r.inkPerHour, 2)} ml/h")
                MiniStat("Relación S:T".tr(), "${Fmt.n(r.ratio, 2)} : 1")
                MiniStat("Cart. solvente".tr(), "${Fmt.n(r.hoursPerMuCart, 0)} h")
            }
        }
    }

    // KPIs
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Solvente / mes".tr(), "${Fmt.i(r.muMonth)} ml", "≈ ${Fmt.n(r.muMonth / 1000.0, 2)} L", Modifier.weight(1f))
        KpiCard("Tinta / mes".tr(), "${Fmt.i(r.inkMonth)} ml", "≈ ${Fmt.n(r.inkMonth / 1000.0, 2)} L", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard("Cartuchos solvente mes".tr(), Fmt.n(r.muCartMonth, 1), "${Fmt.n(r.hoursPerMuCart, 0)} ${I18n.t("h c/u")}", Modifier.weight(1f))
        KpiCard("Cartuchos tinta mes".tr(), Fmt.n(r.inkCartMonth, 1), "${Fmt.i(r.printsPerInkCart)} ${I18n.t("impresiones c/u")}", Modifier.weight(1f))
    }

    // costos de consumibles (2.5.0)
    CostResults(input, r)

    // tabla de detalle
    SectionCard(title = "Detalle del cálculo".tr(), icon = Icons.Filled.Info) {
        DetailRow("Consumo solvente".tr(), r.muPerHour, r.muDay, r.muMonth, r.muYear, "ml")
        DetailRow("Consumo tinta".tr(), r.inkPerHour, r.inkDay, r.inkMonth, r.inkYear, "ml")
        DetailRow("Impresiones".tr(), r.printsPerHour, r.printsPerHour * input.hoursDay, r.printsPerHour * r.hoursMonth, r.printsPerHour * r.hoursYear, "")
        DetailRow(
            "Gotas de tinta".tr(),
            r.dropsPerMessage * r.printsPerHour,
            r.dropsPerMessage * r.printsPerHour * input.hoursDay,
            r.dropsPerMessage * r.printsPerHour * r.hoursMonth,
            r.dropsPerMessage * r.printsPerHour * r.hoursYear,
            "gotas".tr()
        )
        Spacer(Modifier.height(8.dp))
        InfoBox("${I18n.t("Relación solvente : tinta")} = ${Fmt.n(r.ratio, 2)} : 1  ${I18n.t("(regla práctica 4 : 1)")}")
    }

    // gráficos
    SectionCard(title = "Solvente vs temperatura".tr(), icon = Icons.Filled.Info) {
        val curve = remember(input) { CalcEngine.makeupCurve(input) }
        TempCurveChart(points = curve, currentTemp = input.tempC, heightDp = 220)
    }
    SectionCard(title = "Tinta vs solvente mensual".tr(), icon = Icons.Filled.Info) {
        BarsChart(solventMonth = r.muMonth, inkMonth = r.inkMonth, heightDp = 200)
    }
    SectionCard(title = "Consumo acumulado en la jornada".tr(), icon = Icons.Filled.Info) {
        TimelineChart(
            muPerHour = r.muPerHour,
            inkPerHour = r.inkPerHour,
            hoursDay = input.hoursDay,
            cartMl = input.muCartMl,
            heightDp = 200
        )
    }

    // validación / avisos
    SectionCard(title = "Avisos y validación".tr(), icon = Icons.Filled.Warning) {
        AlertsList(input, r)
    }

    // mantenimiento
    SectionCard(title = "Mantenimiento (núcleo / kit)".tr(), icon = Icons.Filled.Info) {
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
                label = "Vida del núcleo consumida al año".tr(),
                pct = pctYear,
                detail = I18n.tf("cambio cada {0} meses de producción", Fmt.n(input.coreHours / r.hoursMonth, 1))
            )
        }
        if (input.coreDays > 0 && input.daysWeek > 0) {
            val pctYear = input.daysWeek * 52.0 / input.coreDays * 100.0
            ProgressLine(
                label = "Vida por días consumida al año".tr(),
                pct = pctYear,
                detail = I18n.tf("cambio cada {0} meses", Fmt.n(input.coreDays / (input.daysWeek * 4.345), 1))
            )
        }
    }

    // exportar
    SectionCard(title = "Exportar resultado".tr(), icon = Icons.Filled.Share) {
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
    SectionCard(title = "Historial de cálculos".tr(), icon = Icons.Filled.Save) {
        if (vm.history.isEmpty()) {
            Text(
                "Todavía no hay cálculos guardados en este dispositivo.".tr(),
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
                    TextButton(onClick = { vm.restoreFromHistory(e.id) }) { Text("Cargar".tr()) }
                    IconButton(onClick = { vm.removeHistory(e.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar".tr())
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))
}

@Composable
private fun CostResults(input: CalcInput, r: CalcResult) {
    val c = CalcEngine.costs(input, r)
    SectionCard(title = "Costo de consumibles".tr(), icon = Icons.Filled.Info) {
        if (!c.configured) {
            Text(
                "Introduzca los precios en «Precios y costos para ventas» para ver el costo real.".tr(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    "Costo por hora".tr(),
                    Money.format(c.totalHour, input.currency),
                    I18n.tf("Tinta {0} · solvente {1}",
                        Money.format(c.inkHour, input.currency),
                        Money.format(c.muHour, input.currency)),
                    Modifier.weight(1f)
                )
                KpiCard(
                    "Costo por mes".tr(),
                    Money.format(c.totalMonth, input.currency),
                    I18n.tf("= {0} al año", Money.format(c.totalYear, input.currency)),
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    "Costo por año".tr(),
                    Money.format(c.totalYear, input.currency),
                    I18n.tf("{0} cartuchos de tinta · {1} de solvente al año",
                        Fmt.n(r.inkCartMonth * 12.0, 0), Fmt.n(r.muCartMonth * 12.0, 0)),
                    Modifier.weight(1f)
                )
                KpiCard(
                    "Costo por 1.000 códigos".tr(),
                    if (c.costPer1000.isFinite() && c.costPer1000 > 0)
                        Money.format(c.costPer1000, input.currency) else "—",
                    I18n.tf("{0} códigos al mes", Fmt.i(r.printsPerHour * r.hoursMonth)),
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))
            CostRow("Costo de tinta".tr(), c.inkHour, c.inkDay, c.inkMonth, c.inkYear, input.currency, true)
            CostRow("Costo de solvente".tr(), c.muHour, c.muDay, c.muMonth, c.muYear, input.currency, true)
            CostRow("Costo total".tr(), c.totalHour, c.totalDay, c.totalMonth, c.totalYear, input.currency, true)

            Spacer(Modifier.height(8.dp))
            ProgressLine(
                label = "Tinta".tr(),
                pct = c.shareInk * 100.0,
                detail = Money.format(c.inkMonth, input.currency) + " / " + I18n.t("mes")
            )
            ProgressLine(
                label = "Solvente".tr(),
                pct = c.shareMu * 100.0,
                detail = Money.format(c.muMonth, input.currency) + " / " + I18n.t("mes")
            )
            if (input.equipPrice > 0) {
                InfoBox(
                    I18n.tf("Costo a {0} años: {1} de consumibles + {2} del equipo = {3}.",
                        c.years.toString(),
                        Money.format(c.consumablesYears, input.currency),
                        Money.format(input.equipPrice, input.currency),
                        Money.format(c.totalOwnership, input.currency))
                )
            }
        }
    }
}

@Composable
private fun CostRow(
    label: String,
    h: Double,
    d: Double,
    m: Double,
    y: Double,
    currency: String,
    on: Boolean
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniMoney("Por hora".tr(), h, currency, on, Modifier.weight(1f))
            MiniMoney("Por día".tr(), d, currency, on, Modifier.weight(1f))
            MiniMoney("Por mes".tr(), m, currency, on, Modifier.weight(1f))
            MiniMoney("Por año".tr(), y, currency, on, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniMoney(label: String, value: Double, currency: String, on: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Money.formatOrDash(value, currency, on),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ComparatorSection(vm: CalculatorViewModel) {
    val input = vm.input
    val context = LocalContext.current
    val columns = remember(input, vm.scenarios) {
        Comparator.columns(input, vm.result, "Cálculo actual".tr(), vm.scenarios)
    }
    val bestId = columns
        .filter { it.configured && it.totalMonth > 0.0 }
        .minByOrNull { it.totalMonth }?.id

    // Exportación del comparador a CSV (mismo formato que la versión web).
    val cmpCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(("\uFEFF" + Comparator.csv(columns)).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Comparación exportada".tr(), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "No se pudo guardar el archivo".tr(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var inkMl by remember { mutableStateOf(0.0) }
    var muMl by remember { mutableStateOf(0.0) }
    var inkPriceL by remember { mutableStateOf(0.0) }
    var muPriceL by remember { mutableStateOf(0.0) }
    var prints by remember { mutableStateOf(0.0) }

    fun resetForm() {
        name = ""
        inkMl = 0.0
        muMl = 0.0
        inkPriceL = 0.0
        muPriceL = 0.0
        prints = 0.0
        showForm = false
    }

    SectionCard(
        title = "Comparador de equipos y competencia".tr(),
        icon = Icons.Filled.Info,
        subtitle = "Compare el cálculo actual con otros equipos Videojet o con equipos de la competencia. Se comparan consumo, cartuchos y costo mensual.".tr()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val ok = vm.addCurrentScenario()
                    Toast.makeText(
                        context,
                        (if (ok) "Cálculo añadido al comparador." else "El comparador admite hasta 4 escenarios.").tr(),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Guardar el cálculo actual".tr()) }
            OutlinedButton(
                onClick = { showForm = !showForm },
                modifier = Modifier.weight(1f)
            ) { Text("Añadir competidor".tr()) }
        }
        if (vm.scenarios.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        cmpCsvLauncher.launch("Comparacion_Videojet_${Report.stamp()}.csv")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Exportar comparación".tr()) }
                TextButton(
                    onClick = {
                        vm.clearScenarios()
                        Toast.makeText(context, "Comparador vaciado".tr(), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Vaciar comparador".tr()) }
            }
        }

        if (showForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        label = { Text("Nombre del equipo o proveedor".tr()) },
                        placeholder = { Text("Competidor A".tr()) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            label = "Tinta consumida al mes".tr(), value = inkMl, suffix = "ml",
                            modifier = Modifier.weight(1f)
                        ) { inkMl = it.coerceIn(0.0, 10_000_000.0) }
                        NumberField(
                            label = "Solvente consumido al mes".tr(), value = muMl, suffix = "ml",
                            modifier = Modifier.weight(1f)
                        ) { muMl = it.coerceIn(0.0, 10_000_000.0) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            label = "Precio de la tinta por litro".tr(), value = inkPriceL,
                            suffix = input.currency, modifier = Modifier.weight(1f)
                        ) { inkPriceL = it.coerceIn(0.0, 1_000_000.0) }
                        NumberField(
                            label = "Precio del solvente por litro".tr(), value = muPriceL,
                            suffix = input.currency, modifier = Modifier.weight(1f)
                        ) { muPriceL = it.coerceIn(0.0, 1_000_000.0) }
                    }
                    NumberField(
                        label = "Impresiones al mes".tr(), value = prints, suffix = "cód.".tr(),
                        isInt = true, modifier = Modifier.fillMaxWidth()
                    ) { prints = it.coerceIn(0.0, 1_000_000_000.0) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val ok = vm.addCompetitorScenario(name, inkMl, muMl, inkPriceL, muPriceL, prints)
                                Toast.makeText(
                                    context,
                                    (if (ok) "Competidor añadido al comparador." else "El comparador admite hasta 4 escenarios.").tr(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (ok) resetForm()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Añadir al comparador".tr()) }
                        OutlinedButton(onClick = { resetForm() }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar".tr())
                        }
                    }
                }
            }
        }

        columns.forEach { col ->
            CompareCard(
                col = col,
                current = columns.firstOrNull(),
                bestId = bestId,
                onDelete = if (col.kind == "current") null else {
                    {
                        vm.removeScenario(col.id)
                        Toast.makeText(context, "Escenario eliminado".tr(), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        Text(
            "El cálculo actual se muestra siempre en la primera columna. Para una comparación justa, use la misma producción y la misma moneda.".tr(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (columns.filter { it.configured }.map { it.currency }.distinct().size > 1) {
            InfoBox(
                "Atención: los escenarios usan monedas distintas; convierta los precios a una sola moneda para comparar.".tr(),
                color = LocalExtraColors.current.warning.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun CompareCard(
    col: CompareColumn,
    current: CompareColumn?,
    bestId: String?,
    onDelete: (() -> Unit)?
) {
    val extra = LocalExtraColors.current
    val kindLabel = when (col.kind) {
        "current" -> "Actual".tr()
        "competitor" -> "Competencia".tr()
        else -> "Videojet"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(col.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "$kindLabel · ${col.model}" + if (col.ink != "—") " · ${col.ink}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar".tr())
                    }
                }
            }
            if (col.id == bestId) {
                Text(
                    "Mejor costo mensual".tr(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = extra.success
                )
            }
            if (!col.configured) {
                Text(
                    "Introduzca al menos un precio para calcular los costos.".tr(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Costo total al mes".tr() + ": " + Money.format(col.totalMonth, col.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Costo total al año".tr() + ": " + Money.format(col.totalYear, col.currency),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Costo por 1.000 códigos".tr() + ": " +
                        (if (col.costPer1000.isFinite() && col.costPer1000 > 0)
                            Money.format(col.costPer1000, col.currency) else "—"),
                    style = MaterialTheme.typography.bodySmall
                )
                val inkPart = "Tinta al mes".tr() + ": " + Fmt.n(col.inkMonth, 0) + " ml"
                val muPart = "Solvente al mes".tr() + ": " + Fmt.n(col.muMonth, 0) + " ml"
                Text(
                    inkPart + " · " + muPart,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (col.kind != "current" && current != null && current.configured) {
                    val diff = col.totalMonth - current.totalMonth
                    val sign = if (diff < 0) "−" else if (diff > 0) "+" else ""
                    Text(
                        "Diferencia mensual".tr() + ": " + sign + Money.format(kotlin.math.abs(diff), col.currency),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (diff < 0) extra.success else if (diff > 0) extra.danger
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
                    I18n.tf("Relación solvente : tinta = {0} : 1, dentro de la regla práctica de 4 cartuchos de solvente por cada uno de tinta.", Fmt.n(r.ratio, 2)))
            )
            r.ratio > 5.0 -> alerts.add(
                Triple(Icons.Filled.Warning, extra.warning,
                    I18n.tf("Relación solvente : tinta = {0} : 1, superior a la regla práctica de 4 : 1. Suele deberse a pocas impresiones/minuto o muchas horas de encendido: el solvente se evapora aunque no se imprima.", Fmt.n(r.ratio, 2)))
            )
            else -> alerts.add(
                Triple(Icons.Filled.Info, extra.accent2,
                    I18n.tf("Relación solvente : tinta = {0} : 1, inferior a la regla práctica de 4 : 1: la línea tiene alta actividad de impresión.", Fmt.n(r.ratio, 2)))
            )
        }
    }
    if (input.tempC >= 32) {
        alerts.add(
            Triple(Icons.Filled.Warning, extra.warning,
                I18n.tf("Temperatura de {0} °C: el consumo de solvente se dispara (factor exponencial). Considere ventilación o condensador activo.", Fmt.n(input.tempC, 1)))
        )
    } else if (input.tempC <= 12) {
        alerts.add(
            Triple(Icons.Filled.Info, extra.accent2,
                I18n.tf("Temperatura baja ({0} °C): menor evaporación de solvente, pero vigile la viscosidad de la tinta y el arranque en frío.", Fmt.n(input.tempC, 1)))
        )
    }
    alerts.add(
        Triple(Icons.Filled.Info, extra.accent,
            I18n.tf("Un litro de solvente dura {0} h y un cartucho de {1} ml dura {2} h.", Fmt.i(1000.0 / r.muPerHour), Fmt.i(input.muCartMl), Fmt.i(r.hoursPerMuCart)))
    )
    alerts.add(
        Triple(Icons.Filled.Info, extra.accent,
            I18n.tf("Tinta: {0} µl por impresión ({1} gotas · boquilla {2} µm).", Fmt.n(r.inkMlPerPrint * 1000.0, 2), Fmt.i(r.dropsPerMessage), input.nozzle))
    )
    if (input.shelfLifeMonths > 0) {
        val months = if (r.muMonth > 0) input.muCartMl / r.muMonth else 0.0
        if (months > input.shelfLifeMonths) {
            alerts.add(
                Triple(Icons.Filled.Warning, extra.warning,
                    I18n.tf("Un cartucho tarda {0} meses en gastarse y su vida útil en almacén es de {1} meses: riesgo de caducidad. Compre envases menores o ajuste el consumo.", Fmt.n(months, 1), Fmt.i(input.shelfLifeMonths)))
            )
        }
    }
    r.maxLineSpeed?.let { max ->
        if (input.lineSpeed > max) {
            alerts.add(
                Triple(Icons.Filled.Error, extra.danger,
                    I18n.tf("La velocidad de línea de {0} m/min supera la máxima permitida para {1} con {2} caracteres y {3} mm de ancho: {4} m/min. Reduzca caracteres, use fuente más pequeña o aumente el ancho.", Fmt.n(input.lineSpeed, 0), input.font, input.chars, Fmt.n(input.widthMm, 1), Fmt.n(max, 1)))
            )
        } else {
            alerts.add(
                Triple(Icons.Filled.CheckCircle, extra.success,
                    I18n.tf("Velocidad de línea válida: máx. {0} m/min para {1} · {2} car. · {3} mm (capacidad {4} caracteres/seg).", Fmt.n(max, 1), input.font, input.chars, Fmt.n(input.widthMm, 1), Fmt.n(r.cps ?: 0.0, 1)))
            )
        }
    }
    r.maxPpm?.let { maxPpm ->
        if (input.ppm > maxPpm) {
            alerts.add(
                Triple(Icons.Filled.Warning, extra.warning,
                    I18n.tf("Las {0} impresiones/min superan las {1} máximas que permite la velocidad de línea con producto de {2} mm + {3} mm de hueco.", Fmt.n(input.ppm, 0), Fmt.n(maxPpm, 0), Fmt.n(input.productWidthMm, 0), Fmt.n(input.gapMm, 0)))
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
private fun ReportSection(vm: CalculatorViewModel) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    val crash = remember { ReportIssue.lastCrash(ctx) }
    var showCrash by remember { mutableStateOf(!crash.isNullOrBlank()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Reportar un error".tr(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Se abrirá su aplicación de correo con el informe ya redactado. No se envía nada automáticamente.".tr(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showCrash) {
                Text(
                    "Se detectó un fallo en el arranque anterior".tr(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        ReportIssue.send(ctx, ReportIssue.build(ctx, I18n.t("Fallo automático registrado")))
                    }) { Text("Enviar el detalle".tr()) }
                    OutlinedButton(onClick = {
                        ReportIssue.clearCrash(ctx)
                        showCrash = false
                    }) { Text("Descartar".tr()) }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Describa el problema o la sugerencia".tr()) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    ReportIssue.send(ctx, ReportIssue.build(ctx, contextDetail(vm) + "\n" + text))
                }) { Text("Enviar por correo".tr()) }
                OutlinedButton(onClick = {
                    ReportIssue.copy(ctx, ReportIssue.build(ctx, contextDetail(vm) + "\n" + text))
                    Toast.makeText(ctx, "Informe copiado al portapapeles".tr(), Toast.LENGTH_SHORT).show()
                }) { Text("Copiar informe".tr()) }
            }
        }
    }
}

private fun contextDetail(vm: CalculatorViewModel): String {
    val i = vm.input
    val r = vm.result
    return buildString {
        appendLine(I18n.tf("Equipo: {0} · boquilla {1} µm · tinta {2} · base {3}", i.modelId, i.nozzle, i.ink, i.inkBase))
        appendLine(I18n.tf("Mensaje: {0} · {1} car. · {2} líneas · {3} mm", i.font, i.chars, i.lines, Fmt.n(i.widthMm, 1)))
        appendLine(I18n.tf("Producción: {0} imp/min · {1} h/día · {2} días/mes · {3} °C", Fmt.n(i.ppm, 0), Fmt.n(i.hoursDay, 1), i.daysMonth, Fmt.n(i.tempC, 1)))
        appendLine(I18n.tf("Resultado: {0} ml/h solvente · {1} ml/h tinta", Fmt.n(r.muPerHour, 2), Fmt.n(r.inkPerHour, 2)))
    }
}

/* ---------------- novedades de la version ---------------- */

@Composable
private fun WhatsNewSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NewReleases, contentDescription = null, tint = LocalExtraColors.current.accent)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Novedades de esta versión".tr(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("Calculadora de costos: precio por cartucho y moneda para ver el costo por hora, día, mes, año y por 1.000 códigos.".tr(), style = MaterialTheme.typography.bodySmall)
            Text("Comparador: el cálculo actual más hasta 4 escenarios Videojet o de la competencia, con diferencias y exportación CSV.".tr(), style = MaterialTheme.typography.bodySmall)
            Text("Costo total de propiedad: consumibles a N años más precio del equipo, con reparto entre tinta y solvente.".tr(), style = MaterialTheme.typography.bodySmall)
            Text("Los informes TXT y CSV incluyen los costos; los precios se guardan solo en este dispositivo.".tr(), style = MaterialTheme.typography.bodySmall)
            Text(
                I18n.tf("Versión {0} · compilación {1} · {2}.", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString(), BuildConfig.BUILD_DATE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
                "Fuentes de datos y notas técnicas".tr(),
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
                    "Los cálculos proceden de los modelos oficiales: CIJ Makeup Usage Model v14 (solvente por temperatura), CIJ Line Speed Calculator v9.7 (velocidades, cpi y dpi) y CIJ TCO Calculator v35 (gotas/litro, gotas/carácter, contenedores y mantenimiento). Contrastados con 734 fichas de solvente y 535 de velocidad de línea.".tr(),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Los tamaños de cartucho varían según la tinta y el equipo (p. ej. V4225 = 700 ml, V480 = 600 ml, V401 = 750/5000 ml); el solvente asociado (V7201, V7257, V7220…) se indica junto a cada tinta.".tr(),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Estimación técnica: contraste los valores con el consumo real del equipo antes de planificar pedidos de consumibles.".tr(),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    I18n.tf("Versión {0} · compilación {1} · herramienta técnica interna · cálculos sin internet · familias Simplicity y Serie 1000VJ.", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()),
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
