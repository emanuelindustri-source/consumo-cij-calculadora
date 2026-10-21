package com.calculadora.consumocij

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.calculadora.consumocij.data.CalcEngine
import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.CalcResult
import com.calculadora.consumocij.data.TemperatureModel
import com.calculadora.consumocij.ui.theme.ThemeMode
import com.calculadora.consumocij.util.Fmt

data class HistoryEntry(
    val id: Long,
    val date: String,
    val input: CalcInput
) {
    val result: CalcResult get() = CalcEngine.compute(input)
}

/**
 * Estado de la aplicación + persistencia 100 % local (SharedPreferences).
 * No se usa red: la app recuerda los últimos valores y el tema elegido.
 */
class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs: SharedPreferences =
        app.getSharedPreferences("vj_calc_state", Context.MODE_PRIVATE)

    var input by mutableStateOf(loadInput())
        private set

    var themeMode by mutableStateOf(loadTheme())
        private set

    var history by mutableStateOf(loadHistory())
        private set

    val result: CalcResult get() = CalcEngine.compute(input)

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
            date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date()),
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

    /* ---------------- persistencia ---------------- */

    private fun loadTheme(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    private fun saveInput(i: CalcInput) = saveInputInto(prefs, "", i)

    private fun loadInput(): CalcInput = loadInputFrom(prefs, "", CalcInput())

    private fun saveHistory(list: List<HistoryEntry>) {
        val ed = prefs.edit()
        ed.putInt(KEY_HIST_COUNT, list.size)
        list.forEachIndexed { idx, e ->
            saveInputInto(ed, "$KEY_HIST$idx.", e.input)
            ed.putString("$KEY_HIST$idx.date", e.date)
        }
        ed.apply()
    }

    private fun loadHistory(): List<HistoryEntry> {
        val n = prefs.getInt(KEY_HIST_COUNT, 0)
        if (n <= 0) return emptyList()
        val out = ArrayList<HistoryEntry>()
        for (idx in 0 until n) {
            val prefix = "$KEY_HIST$idx."
            val i = loadInputFrom(prefs, prefix, CalcInput())
            val date = prefs.getString(prefix + "date", "") ?: ""
            if (date.isEmpty()) continue
            out.add(HistoryEntry(id = (prefix + date).hashCode().toLong(), date = date, input = i))
        }
        return out
    }

    fun restoreFromHistory(id: Long) {
        // reconstruye la entrada a partir de su prefijo almacenado
        val n = prefs.getInt(KEY_HIST_COUNT, 0)
        for (idx in 0 until n) {
            val prefix = "$KEY_HIST$idx."
            val date = prefs.getString(prefix + "date", "") ?: ""
            if ((prefix + date).hashCode().toLong() == id) {
                input = loadInputFrom(prefs, prefix, input)
                saveInput(input)
                return
            }
        }
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_HIST_COUNT = "hist.count"
        private const val KEY_HIST = "hist."

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
