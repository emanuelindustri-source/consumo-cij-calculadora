package com.calculadora.consumocij.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Valida el motor de solvente contra las 800 fichas oficiales
 * (CIJ Makeup Usage Model v14, 12.800 valores entre 10 y 40 °C).
 *
 * El fixture `solvent_cards.json` se genera desde los PDF por equipo/tinta.
 * Tolerancia: 0,06 ml/h (las fichas publican un decimal).
 */
class SolventCardsTest {

    private data class Card(
        val model: String,
        val ink: String,
        val base: String,
        val values: List<Double>
    )

    private fun loadCards(): List<Card> {
        val text = javaClass.classLoader!!.getResourceAsStream("solvent_cards.json")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<Card>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val values = o.getJSONArray("values")
            out.add(
                Card(
                    model = o.getString("model"),
                    ink = o.getString("ink"),
                    base = o.getString("base"),
                    values = (0 until values.length()).map { values.getDouble(it) }
                )
            )
        }
        return out
    }

    @Test
    fun lasOchoCientasFichasCoincidenConElMotor() {
        val cards = loadCards()
        assertTrue("faltan fichas", cards.size >= 800)
        val errors = ArrayList<String>()
        var checked = 0
        for (card in cards) {
            val baseName = card.model.replace(Regex("\\s*(60|70|50|40)\\s*µm.*$"), "").trim()
            val nozzle = Regex("\\s*(60|70|50|40)\\s*µm").find(card.model)?.groupValues?.get(1)
            val model = VideojetData.models.firstOrNull { it.id == baseName }
            if (model == null) {
                errors.add("modelo no encontrado: ${card.model}")
                continue
            }
            val variant = (nozzle?.let { model.variants[it] }) ?: model.defaultVariant
            val base = VideojetData.inkBases[card.base]
            if (base == null) {
                errors.add("base no encontrada: ${card.base}")
                continue
            }
            card.values.forEachIndexed { i, expected ->
                val temp = (10 + i * 2).toDouble()
                val got = CalcEngine.makeupPerHour(
                    variant.hrsL, base, variant, temp, 1.0, TemperatureModel.PUBLISHED
                )
                checked++
                if (abs(got - expected) > 0.06) {
                    errors.add(
                        "${card.model} · ${card.ink} · ${temp.toInt()} °C: " +
                            "esperado $expected, obtenido ${"%.3f".format(got)}"
                    )
                }
            }
        }
        assertEquals("valores comprobados", 12_800, checked)
        assertEquals("primeras diferencias", emptyList<String>(), errors.take(10))
        assertEquals("total de diferencias", 0, errors.size)
    }
}
