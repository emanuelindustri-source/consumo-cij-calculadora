package com.calculadora.consumocij.util

import com.calculadora.consumocij.HistoryEntry
import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.Scenario
import com.calculadora.consumocij.data.TemperatureModel
import com.calculadora.consumocij.data.VideojetData
import com.calculadora.consumocij.i18n.I18n
import com.calculadora.consumocij.ui.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Copia de seguridad local en JSON: configuración, historial, tema e idioma.
 *
 * No se sube a ningún servidor. El archivo se escribe con el selector del
 * sistema (SAF), así que sirve para cambiar de dispositivo o recuperar datos.
 */
object Backup {

    const val FORMAT = 1
    const val MIME = "application/json"

    data class Payload(
        val input: CalcInput,
        val history: List<HistoryEntry>,
        val theme: ThemeMode,
        val language: String,
        val scenarios: List<Scenario> = emptyList()
    )

    fun encode(payload: Payload): String {
        val root = JSONObject()
        root.put("app", "Consumo CIJ")
        root.put("format", FORMAT)
        root.put("createdAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date()))
        root.put("theme", payload.theme.name)
        root.put("language", payload.language)
        root.put("input", inputToJson(payload.input))
        val hist = JSONArray()
        payload.history.take(20).forEach { e ->
            hist.put(JSONObject().put("date", e.date).put("input", inputToJson(e.input)))
        }
        root.put("history", hist)
        root.put("scenarios", JSONArray(ScenarioStore.encode(payload.scenarios)))
        return root.toString(2)
    }

    fun decode(text: String): Payload? = runCatching {
        val root = JSONObject(text)
        if (root.optString("app") != "Consumo CIJ") return null
        val input = inputFromJson(root.getJSONObject("input"))
        val hist = ArrayList<HistoryEntry>()
        val arr = root.optJSONArray("history") ?: JSONArray()
        for (i in 0 until minOf(arr.length(), 20)) {
            val e = arr.optJSONObject(i) ?: continue
            val date = e.optString("date", "").trim()
            if (date.isEmpty()) continue
            hist.add(
                HistoryEntry(
                    id = System.currentTimeMillis() + i,
                    date = date,
                    input = inputFromJson(e.optJSONObject("input") ?: JSONObject())
                )
            )
        }
        val theme = runCatching { ThemeMode.valueOf(root.optString("theme", "SYSTEM")) }
            .getOrDefault(ThemeMode.SYSTEM)
        val lang = root.optString("language", "")
        val scenarios = ScenarioStore.decode(root.optJSONArray("scenarios")?.toString())
        Payload(input, hist, theme, if (lang in I18n.LANGUAGES) lang else "", scenarios)
    }.getOrNull()

    /* ---------------- serialización de CalcInput ---------------- */

    internal fun inputToJson(i: CalcInput): JSONObject = JSONObject().apply {
        put("modelId", i.modelId)
        put("nozzle", i.nozzle)
        put("ink", i.ink)
        put("inkBase", i.inkBase)
        put("font", i.font)
        put("chars", i.chars)
        put("lines", i.lines)
        put("stroke", i.stroke)
        put("widthMm", i.widthMm)
        put("tempC", i.tempC)
        put("ppm", i.ppm)
        put("hoursDay", i.hoursDay)
        put("daysWeek", i.daysWeek)
        put("daysMonth", i.daysMonth)
        put("efficiencyPct", i.efficiencyPct)
        put("lineSpeed", i.lineSpeed)
        put("productWidthMm", i.productWidthMm)
        put("gapMm", i.gapMm)
        put("inkCartMl", i.inkCartMl)
        put("muCartMl", i.muCartMl)
        put("shelfLifeMonths", i.shelfLifeMonths)
        put("coreHours", i.coreHours)
        put("coreDays", i.coreDays)
        put("temperatureModel", i.temperatureModel.name)
        put("envFactor", i.envFactor)
        put("currency", i.currency)
        put("inkPrice", i.inkPrice)
        put("muPrice", i.muPrice)
        put("equipPrice", i.equipPrice)
        put("years", i.years)
    }

    internal fun inputFromJson(o: JSONObject): CalcInput {
        val def = CalcInput()
        fun s(k: String, d: String) = o.optString(k, d).ifBlank { d }
        fun n(k: String, d: Int, lo: Int, hi: Int) = o.optInt(k, d).coerceIn(lo, hi)
        fun d(k: String, v: Double, lo: Double, hi: Double) = o.optDouble(k, v).let {
            if (it.isFinite()) it.coerceIn(lo, hi) else v
        }

        val model = VideojetData.models.firstOrNull { it.id == s("modelId", def.modelId) }
            ?: VideojetData.models.first()
        val nozzle = s("nozzle", def.nozzle).let { nz ->
            if (nz in model.nozzles) nz else model.nozzles.firstOrNull() ?: def.nozzle
        }
        val inks = VideojetData.inksFor(model.inkGroup)
        val ink = s("ink", def.ink).let { if (it in inks) it else inks.firstOrNull() ?: def.ink }
        val inkBase = s("inkBase", def.inkBase).let {
            if (it in VideojetData.inkBases) it else VideojetData.inks[ink] ?: def.inkBase
        }
        val font = s("font", def.font).let { if (it in VideojetData.fonts) it else def.font }

        val inkCart = d("inkCartMl", def.inkCartMl, 1.0, 20_000.0)
        val muCart = d("muCartMl", def.muCartMl, 1.0, 20_000.0)
        fun pick(v: Double, sizes: List<Int>) =
            if (sizes.any { kotlin.math.abs(it - v) < 0.5 }) v else sizes.firstOrNull()?.toDouble() ?: v

        return CalcInput(
            modelId = model.id,
            nozzle = nozzle,
            ink = ink,
            inkBase = inkBase,
            font = font,
            chars = n("chars", def.chars, 1, 500),
            lines = n("lines", def.lines, 1, 5),
            stroke = n("stroke", def.stroke, 1, 4),
            widthMm = d("widthMm", def.widthMm, 1.0, 500.0),
            tempC = d("tempC", def.tempC, -20.0, 60.0),
            ppm = d("ppm", def.ppm, 0.0, 5000.0),
            hoursDay = d("hoursDay", def.hoursDay, 0.0, 24.0),
            daysWeek = n("daysWeek", def.daysWeek, 1, 7),
            daysMonth = n("daysMonth", def.daysMonth, 1, 31),
            efficiencyPct = d("efficiencyPct", def.efficiencyPct, 1.0, 100.0),
            lineSpeed = d("lineSpeed", def.lineSpeed, 0.0, 1000.0),
            productWidthMm = d("productWidthMm", def.productWidthMm, 0.0, 5000.0),
            gapMm = d("gapMm", def.gapMm, 0.0, 1000.0),
            inkCartMl = pick(inkCart, VideojetData.inkSizesFor(model.id, ink)),
            muCartMl = pick(muCart, VideojetData.muSizesFor(model.id, ink)),
            shelfLifeMonths = d("shelfLifeMonths", def.shelfLifeMonths, 0.0, 120.0),
            coreHours = d("coreHours", def.coreHours, 0.0, 50_000.0),
            coreDays = d("coreDays", def.coreDays, 0.0, 5000.0),
            temperatureModel = runCatching {
                TemperatureModel.valueOf(o.optString("temperatureModel", def.temperatureModel.name))
            }.getOrDefault(def.temperatureModel),
            envFactor = d("envFactor", def.envFactor, 0.2, 3.0),
            currency = Money.normalize(s("currency", def.currency)),
            inkPrice = d("inkPrice", def.inkPrice, 0.0, 1_000_000.0),
            muPrice = d("muPrice", def.muPrice, 0.0, 1_000_000.0),
            equipPrice = d("equipPrice", def.equipPrice, 0.0, 100_000_000.0),
            years = n("years", def.years, 1, 20)
        )
    }
}
