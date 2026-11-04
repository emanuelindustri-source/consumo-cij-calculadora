package com.calculadora.consumocij

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calculadora.consumocij.data.CalcEngine
import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.CalcResult
import com.calculadora.consumocij.data.TemperatureModel
import com.calculadora.consumocij.data.VideojetData
import com.calculadora.consumocij.i18n.I18n
import com.calculadora.consumocij.ui.theme.ThemeMode
import com.calculadora.consumocij.util.Backup
import com.calculadora.consumocij.update.UpdateChecker
import com.calculadora.consumocij.update.UpdateDownloadProcessor
import com.calculadora.consumocij.update.UpdateDownloader
import com.calculadora.consumocij.update.UpdateInfo
import com.calculadora.consumocij.update.UpdateInstaller
import com.calculadora.consumocij.update.UpdateNotifier
import com.calculadora.consumocij.update.UpdateScheduler
import com.calculadora.consumocij.update.UpdateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

data class HistoryEntry(
    val id: Long,
    val date: String,
    val input: CalcInput
) {
    val result: CalcResult get() = CalcEngine.compute(input)
}

/**
 * Estado de la aplicación + persistencia local (SharedPreferences).
 *
 * Los cálculos, el historial y el tema son 100 % locales. La única función que
 * usa la red es la comprobación de actualizaciones, que el usuario puede
 * desactivar y que solo descarga el archivo de versión del proyecto.
 */
class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx: Context = app.applicationContext

    private val prefs: SharedPreferences =
        app.getSharedPreferences("vj_calc_state", Context.MODE_PRIVATE)

    var input by mutableStateOf(loadInput())
        private set

    var themeMode by mutableStateOf(loadTheme())
        private set

    var history by mutableStateOf(loadHistory())
        private set

    val result: CalcResult get() = CalcEngine.compute(input)

    /* ---------------- actualizaciones ---------------- */

    var updateAvailable by mutableStateOf<UpdateInfo?>(null)
        private set
    var updateChecking by mutableStateOf(false)
        private set
    var updateDialogOpen by mutableStateOf(false)
        private set
    var updateError by mutableStateOf<String?>(null)
        private set
    var updateUpToDate by mutableStateOf(false)
        private set
    var autoCheck by mutableStateOf(UpdateStore.autoCheck(appCtx))
        private set
    var notifyUpdates by mutableStateOf(UpdateStore.notify(appCtx))
        private set
    var notificationsAsked by mutableStateOf(UpdateStore.askedNotifications(appCtx))
        private set
    var notificationsHint by mutableStateOf(false)
        private set
    var downloading by mutableStateOf(UpdateStore.downloadId(appCtx) != -1L)
        private set
    var downloadReady by mutableStateOf(UpdateStore.apkReady(appCtx))
        private set
    var skippedVersion by mutableStateOf(UpdateStore.skippedVersion(appCtx))
        private set
    var lastCheck by mutableStateOf(UpdateStore.lastCheck(appCtx))
        private set
    var installError by mutableStateOf<String?>(null)
        private set

    /** Versión instalada, para mostrarla en la interfaz y en los informes. */
    val installedVersion: String get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    /** El aviso superior solo aparece si el usuario no omitio esa version. */
    val updateBannerVisible: Boolean
        get() = updateAvailable?.let { skippedVersion != it.versionCode } == true

    init {
        // Si la version descargada ya esta instalada, el APK temporal sobra.
        if (UpdateStore.pendingVersionCode(appCtx) in 1..BuildConfig.VERSION_CODE) {
            UpdateStore.clearApk(appCtx)
        }
        UpdateNotifier.ensureChannel(appCtx)
        UpdateScheduler.sync(appCtx)
        if (UpdateStore.autoCheck(appCtx)) checkUpdates(silent = true)
    }

    fun update(transform: (CalcInput) -> CalcInput) {
        input = transform(input)
        saveInput(input)
    }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun reset() {
        input = CalcInput()
        saveInput(input)
    }

    fun addHistory() {
        val e = HistoryEntry(
            id = System.currentTimeMillis(),
            date = SimpleDateFormat("dd/MM/yyyy HH:mm", I18n.activeLocale()).format(Date()),
            input = input
        )
        val list = (listOf(e) + history).take(20)
        history = list
        saveHistory(list)
    }

    fun removeHistory(id: Long) {
        val list = history.filterNot { it.id == id }
        history = list
        saveHistory(list)
    }

    fun restoreFromHistory(id: Long) {
        val entry = history.firstOrNull { it.id == id } ?: return
        input = entry.input
        saveInput(input)
    }

    /* ---------------- copia de seguridad ---------------- */

    fun exportBackup(context: Context, uri: Uri): Boolean = runCatching {
        val json = Backup.encode(Backup.Payload(input, history, themeMode, I18n.preference))
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        true
    }.getOrDefault(false)

    /** Devuelve cuantos calculos se importaron, o null si el archivo no es valido. */
    fun importBackup(context: Context, uri: Uri): Int? = runCatching {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: return null
        val payload = Backup.decode(text) ?: return null
        input = payload.input
        saveInput(input)
        history = payload.history
        saveHistory(history)
        setTheme(payload.theme)
        I18n.setLanguage(context, payload.language)
        history.size
    }.getOrNull()

    /* ---------------- actualizaciones: acciones ---------------- */

    fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        val open = intent.getBooleanExtra(MainActivity.EXTRA_OPEN_UPDATE, false)
        val install = intent.getBooleanExtra(MainActivity.EXTRA_INSTALL_UPDATE, false)
        intent.removeExtra(MainActivity.EXTRA_OPEN_UPDATE)
        intent.removeExtra(MainActivity.EXTRA_INSTALL_UPDATE)
        if (!open && !install) return
        requestUpdateDialog()
        if (install) {
            downloading = false
            downloadReady = UpdateStore.apkReady(appCtx)
            if (downloadReady && !UpdateInstaller.install(appCtx)) {
                installError = I18n.t("No se pudo abrir el instalador. Permita instalar aplicaciones desconocidas para esta app.")
            }
        }
    }

    fun onResume() {
        downloadReady = UpdateStore.apkReady(appCtx)
        lastCheck = UpdateStore.lastCheck(appCtx)
        val id = UpdateStore.downloadId(appCtx)
        if (id > 0) {
            downloading = true
            viewModelScope.launch {
                withContext(Dispatchers.IO) { UpdateDownloadProcessor.process(appCtx, id) }
                downloading = UpdateStore.downloadId(appCtx) != -1L
                downloadReady = UpdateStore.apkReady(appCtx)
                if (UpdateStore.lastError(appCtx) != null) updateError = UpdateStore.lastError(appCtx)
            }
        } else {
            downloading = false
        }
    }

    /** Abre el diálogo y, si hace falta, comprueba si hay novedades. */
    fun requestUpdateDialog() {
        updateDialogOpen = true
        if (updateAvailable == null && !updateChecking) checkUpdates(silent = true)
    }

    fun closeUpdateDialog() {
        updateDialogOpen = false
    }

    fun checkUpdates(silent: Boolean = false) {
        if (updateChecking) return
        updateChecking = true
        updateError = null
        if (!silent) updateUpToDate = false
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { UpdateChecker.check(appCtx) }
            updateChecking = false
            lastCheck = UpdateStore.lastCheck(appCtx)
            when (res) {
                is UpdateChecker.Result.Newer -> {
                    updateAvailable = res.info
                    updateError = null
                    updateUpToDate = false
                    val prompted = UpdateStore.promptedVersion(appCtx)
                    val remind = UpdateStore.shouldRemind(appCtx, res.info.versionCode)
                    if (!silent || prompted < res.info.versionCode || remind || res.info.mandatory) {
                        updateDialogOpen = true
                        UpdateStore.setPromptedVersion(appCtx, res.info.versionCode)
                    }
                }
                UpdateChecker.Result.UpToDate -> {
                    updateAvailable = null
                    updateUpToDate = true
                    UpdateStore.setSkippedVersion(appCtx, 0)
                    skippedVersion = 0
                }
                is UpdateChecker.Result.Failed -> {
                    updateError = res.message
                }
            }
        }
    }

    fun skipUpdate() {
        updateAvailable?.let {
            UpdateStore.skipVersion(appCtx, it.versionCode)
            skippedVersion = it.versionCode
        }
        updateDialogOpen = false
        UpdateNotifier.cancel(appCtx, UpdateNotifier.ID_AVAILABLE)
    }

    fun downloadUpdate(context: Context) {
        val info = updateAvailable ?: return
        installError = null
        updateError = null
        if (!UpdateDownloader.start(context, info)) {
            updateError = I18n.t("No se pudo descargar la actualización.")
            return
        }
        downloading = true
        downloadReady = false
        viewModelScope.launch {
            repeat(300) {
                delay(2_000)
                val id = UpdateStore.downloadId(appCtx)
                if (id > 0) {
                    withContext(Dispatchers.IO) { UpdateDownloadProcessor.process(appCtx, id) }
                }
                if (UpdateStore.apkReady(appCtx)) {
                    downloading = false
                    downloadReady = true
                    updateError = null
                    return@launch
                }
                if (UpdateStore.downloadId(appCtx) == -1L) {
                    downloading = false
                    downloadReady = false
                    updateError = UpdateStore.lastError(appCtx)
                    return@launch
                }
            }
            downloading = false
            downloadReady = UpdateStore.apkReady(appCtx)
        }
    }

    fun installUpdate(context: Context) {
        installError = null
        if (!UpdateInstaller.install(context)) {
            installError = I18n.t("No se pudo abrir el instalador. Permita instalar aplicaciones desconocidas para esta app.")
        }
    }

    fun enableAutoCheck(enabled: Boolean) {
        autoCheck = enabled
        UpdateStore.setAutoCheck(appCtx, enabled)
        if (enabled && notifyUpdates) UpdateScheduler.schedule(appCtx) else UpdateScheduler.cancel(appCtx)
    }

    fun enableNotifyUpdates(enabled: Boolean) {
        notifyUpdates = enabled
        UpdateStore.setNotify(appCtx, enabled)
        UpdateStore.setAskedNotifications(appCtx, true)
        notificationsAsked = true
        notificationsHint = enabled && !UpdateNotifier.canNotify(appCtx)
        UpdateScheduler.sync(appCtx)
        if (!enabled) UpdateNotifier.cancel(appCtx, UpdateNotifier.ID_AVAILABLE)
    }

    fun markNotificationsAsked() {
        notificationsAsked = true
        UpdateStore.setAskedNotifications(appCtx, true)
    }

    fun clearNotificationsHint() {
        notificationsHint = false
    }

    fun clearUpdateError() {
        updateError = null
        installError = null
        UpdateStore.setLastError(appCtx, null)
    }

    /* ---------------- persistencia ---------------- */

    private fun loadTheme(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    private fun saveInput(i: CalcInput) = saveInputInto(prefs, "", i)

    private fun loadInput(): CalcInput {
        val i = loadInputFrom(prefs, "", CalcInput())
        // Corrige configuraciones guardadas: tinta que no pertenece al modelo y
        // tamaños que el modelo no admite (p. ej. 5000 ml en un equipo «750 mL Only»).
        fun pick(v: Double, sizes: List<Int>): Double {
            val valid = sizes.map { it.toDouble() }
            return if (valid.any { kotlin.math.abs(it - v) < 0.5 }) v
            else valid.firstOrNull() ?: v
        }
        val model = VideojetData.models.firstOrNull { it.id == i.modelId } ?: VideojetData.models.first()
        val inks = VideojetData.inksFor(model.inkGroup)
        val ink = if (i.ink in inks) i.ink else (inks.firstOrNull() ?: i.ink)
        val base = if (i.inkBase in VideojetData.inkBases) i.inkBase else (VideojetData.inks[ink] ?: i.inkBase)
        return i.copy(
            ink = ink,
            inkBase = base,
            inkCartMl = pick(i.inkCartMl, VideojetData.inkSizesFor(model.id, ink)),
            muCartMl = pick(i.muCartMl, VideojetData.muSizesFor(model.id, ink))
        )
    }

    private fun saveHistory(list: List<HistoryEntry>) {
        val previous = prefs.getInt(KEY_HIST_COUNT, 0)
        val ed = prefs.edit()
        ed.putInt(KEY_HIST_COUNT, list.size)
        list.forEachIndexed { idx, e ->
            saveInputInto(ed, "$KEY_HIST$idx.", e.input)
            ed.putString("$KEY_HIST$idx.date", e.date)
            ed.putString("$KEY_HIST$idx.id", e.id.toString())
        }
        // Limpia las entradas que ya no existen (evita datos huérfanos).
        for (idx in list.size until maxOf(previous, list.size)) {
            val prefix = "$KEY_HIST$idx."
            ed.remove(prefix + "date")
            ed.remove(prefix + "id")
            for (k in INPUT_KEYS) ed.remove(prefix + k)
        }
        ed.apply()
    }

    private fun loadHistory(): List<HistoryEntry> {
        val n = prefs.getInt(KEY_HIST_COUNT, 0)
        if (n <= 0) return emptyList()
        val out = ArrayList<HistoryEntry>()
        for (idx in 0 until n) {
            val prefix = "$KEY_HIST$idx."
            val date = prefs.getString(prefix + "date", "") ?: ""
            if (date.isEmpty()) continue
            val id = prefs.getString(prefix + "id", null)?.toLongOrNull()
                ?: (prefix + date).hashCode().toLong()
            out.add(HistoryEntry(id = id, date = date, input = loadInputFrom(prefs, prefix, CalcInput())))
        }
        return out
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_HIST_COUNT = "hist.count"
        private const val KEY_HIST = "hist."

        private val INPUT_KEYS = listOf(
            "modelId", "nozzle", "ink", "inkBase", "font", "chars", "lines", "stroke",
            "widthMm", "tempC", "ppm", "hoursDay", "daysWeek", "daysMonth",
            "efficiencyPct", "lineSpeed", "productWidthMm", "gapMm", "inkCartMl",
            "muCartMl", "shelfLifeMonths", "coreHours", "coreDays", "temperatureModel",
            "envFactor"
        )

        fun saveInputInto(prefs: SharedPreferences.Editor, p: String, i: CalcInput) {
            prefs.putString(p + "modelId", i.modelId)
            prefs.putString(p + "nozzle", i.nozzle)
            prefs.putString(p + "ink", i.ink)
            prefs.putString(p + "inkBase", i.inkBase)
            prefs.putString(p + "font", i.font)
            prefs.putInt(p + "chars", i.chars)
            prefs.putInt(p + "lines", i.lines)
            prefs.putInt(p + "stroke", i.stroke)
            prefs.putFloat(p + "widthMm", i.widthMm.toFloat())
            prefs.putFloat(p + "tempC", i.tempC.toFloat())
            prefs.putFloat(p + "ppm", i.ppm.toFloat())
            prefs.putFloat(p + "hoursDay", i.hoursDay.toFloat())
            prefs.putInt(p + "daysWeek", i.daysWeek)
            prefs.putInt(p + "daysMonth", i.daysMonth)
            prefs.putFloat(p + "efficiencyPct", i.efficiencyPct.toFloat())
            prefs.putFloat(p + "lineSpeed", i.lineSpeed.toFloat())
            prefs.putFloat(p + "productWidthMm", i.productWidthMm.toFloat())
            prefs.putFloat(p + "gapMm", i.gapMm.toFloat())
            prefs.putFloat(p + "inkCartMl", i.inkCartMl.toFloat())
            prefs.putFloat(p + "muCartMl", i.muCartMl.toFloat())
            prefs.putFloat(p + "shelfLifeMonths", i.shelfLifeMonths.toFloat())
            prefs.putFloat(p + "coreHours", i.coreHours.toFloat())
            prefs.putFloat(p + "coreDays", i.coreDays.toFloat())
            prefs.putString(p + "temperatureModel", i.temperatureModel.name)
            prefs.putFloat(p + "envFactor", i.envFactor.toFloat())
        }

        fun saveInputInto(prefs: SharedPreferences, p: String, i: CalcInput) {
            val ed = prefs.edit()
            saveInputInto(ed, p, i)
            ed.apply()
        }

        fun loadInputFrom(prefs: SharedPreferences, p: String, def: CalcInput): CalcInput {
            fun s(k: String, d: String) = prefs.getString(p + k, d) ?: d
            fun f(k: String, d: Float) = prefs.getFloat(p + k, d)
            fun n(k: String, d: Int) = prefs.getInt(p + k, d)
            return CalcInput(
                modelId = s("modelId", def.modelId),
                nozzle = s("nozzle", def.nozzle),
                ink = s("ink", def.ink),
                inkBase = s("inkBase", def.inkBase),
                font = s("font", def.font),
                chars = n("chars", def.chars),
                lines = n("lines", def.lines),
                stroke = n("stroke", def.stroke),
                widthMm = f("widthMm", def.widthMm.toFloat()).toDouble(),
                tempC = f("tempC", def.tempC.toFloat()).toDouble(),
                ppm = f("ppm", def.ppm.toFloat()).toDouble(),
                hoursDay = f("hoursDay", def.hoursDay.toFloat()).toDouble(),
                daysWeek = n("daysWeek", def.daysWeek),
                daysMonth = n("daysMonth", def.daysMonth),
                efficiencyPct = f("efficiencyPct", def.efficiencyPct.toFloat()).toDouble(),
                lineSpeed = f("lineSpeed", def.lineSpeed.toFloat()).toDouble(),
                productWidthMm = f("productWidthMm", def.productWidthMm.toFloat()).toDouble(),
                gapMm = f("gapMm", def.gapMm.toFloat()).toDouble(),
                inkCartMl = f("inkCartMl", def.inkCartMl.toFloat()).toDouble(),
                muCartMl = f("muCartMl", def.muCartMl.toFloat()).toDouble(),
                shelfLifeMonths = f("shelfLifeMonths", def.shelfLifeMonths.toFloat()).toDouble(),
                coreHours = f("coreHours", def.coreHours.toFloat()).toDouble(),
                coreDays = f("coreDays", def.coreDays.toFloat()).toDouble(),
                temperatureModel = runCatching {
                    TemperatureModel.valueOf(s("temperatureModel", def.temperatureModel.name))
                }.getOrDefault(def.temperatureModel),
                envFactor = f("envFactor", def.envFactor.toFloat()).toDouble()
            )
        }
    }
}
