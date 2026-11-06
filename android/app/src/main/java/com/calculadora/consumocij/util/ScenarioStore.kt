package com.calculadora.consumocij.util

import android.content.SharedPreferences
import com.calculadora.consumocij.data.Comparator
import com.calculadora.consumocij.data.Scenario
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia local del comparador (2.5.0): hasta 4 escenarios por dispositivo.
 * No se envía nada a ningún servidor.
 */
object ScenarioStore {

    const val KEY = "scenarios_v1"

    fun load(prefs: SharedPreferences): List<Scenario> =
        decode(prefs.getString(KEY, null))

    fun save(prefs: SharedPreferences, list: List<Scenario>) {
        prefs.edit().putString(KEY, encode(list)).apply()
    }

    fun encode(list: List<Scenario>): String {
        val arr = JSONArray()
        list.take(Comparator.MAX).forEach { arr.put(toJson(it)) }
        return arr.toString()
    }

    fun decode(text: String?): List<Scenario> {
        if (text.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(text)
            val out = ArrayList<Scenario>()
            for (i in 0 until minOf(arr.length(), Comparator.MAX)) {
                val o = arr.optJSONObject(i) ?: continue
                fromJson(o)?.let { out.add(it) }
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun toJson(sc: Scenario): JSONObject = when (sc) {
        is Scenario.Videojet -> JSONObject()
            .put("id", sc.id)
            .put("kind", "videojet")
            .put("name", sc.name)
            .put("input", Backup.inputToJson(sc.input))
        is Scenario.Competitor -> JSONObject()
            .put("id", sc.id)
            .put("kind", "competitor")
            .put("name", sc.name)
            .put("inkMlMonth", sc.inkMlMonth)
            .put("muMlMonth", sc.muMlMonth)
            .put("inkPriceL", sc.inkPriceL)
            .put("muPriceL", sc.muPriceL)
            .put("printsMonth", sc.printsMonth)
            .put("currency", sc.currency)
    }

    private fun fromJson(o: JSONObject): Scenario? {
        return when (o.optString("kind", "")) {
            "videojet" -> {
                val input = o.optJSONObject("input")
                if (input == null) null else Scenario.Videojet(
                    id = o.optString("id", "").ifBlank { "s" + System.nanoTime() },
                    name = o.optString("name", "").ifBlank { "Videojet" },
                    input = Backup.inputFromJson(input)
                )
            }
            "competitor" -> Scenario.Competitor(
                id = o.optString("id", "").ifBlank { "c" + System.nanoTime() },
                name = o.optString("name", "").ifBlank { "—" },
                inkMlMonth = o.optDouble("inkMlMonth", 0.0).finite(0.0, 10_000_000.0),
                muMlMonth = o.optDouble("muMlMonth", 0.0).finite(0.0, 10_000_000.0),
                inkPriceL = o.optDouble("inkPriceL", 0.0).finite(0.0, 1_000_000.0),
                muPriceL = o.optDouble("muPriceL", 0.0).finite(0.0, 1_000_000.0),
                printsMonth = o.optDouble("printsMonth", 0.0).finite(0.0, 1_000_000_000.0),
                currency = Money.normalize(o.optString("currency", "USD"))
            )
            else -> null
        }
    }

    private fun Double.finite(lo: Double, hi: Double): Double =
        if (isFinite()) coerceIn(lo, hi) else lo
}
