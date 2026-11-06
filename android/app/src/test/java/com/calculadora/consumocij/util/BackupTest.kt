package com.calculadora.consumocij.util

import com.calculadora.consumocij.HistoryEntry
import com.calculadora.consumocij.data.CalcInput
import com.calculadora.consumocij.data.Scenario
import com.calculadora.consumocij.ui.theme.ThemeMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Copia de seguridad: ida y vuelta, validación y corrección de tamaños. */
class BackupTest {

    @Test
    fun idaYVueltaConservaConfiguracionEHistorial() {
        val input = CalcInput(
            modelId = "VJ 1880+", nozzle = "60", ink = "V4201", inkBase = "MEK",
            chars = 14, lines = 2, tempC = 30.0, ppm = 75.0, hoursDay = 12.0,
            daysMonth = 24, inkCartMl = 1000.0, muCartMl = 1000.0
        )
        val history = listOf(
            HistoryEntry(1L, "01/11/2026 10:00", input),
            HistoryEntry(2L, "02/11/2026 11:00", input.copy(modelId = "VJ 1580", inkCartMl = 750.0))
        )
        val payload = Backup.Payload(input, history, ThemeMode.AMOLED, "en")
        val decoded = Backup.decode(Backup.encode(payload))!!
        assertEquals("VJ 1880+", decoded.input.modelId)
        assertEquals(14, decoded.input.chars)
        assertEquals(30.0, decoded.input.tempC, 0.001)
        assertEquals(2, decoded.history.size)
        assertEquals(ThemeMode.AMOLED, decoded.theme)
        assertEquals("en", decoded.language)
    }

    @Test
    fun idaYVueltaConservaPreciosYComparador() {
        val input = CalcInput(
            modelId = "VJ 1880+", currency = "MXN", inkPrice = 1200.0,
            muPrice = 450.0, equipPrice = 350000.0, years = 7
        )
        val scenarios = listOf(
            Scenario.Videojet("s1", "VJ 1580 · V4201", input.copy(modelId = "VJ 1580")),
            Scenario.Competitor(
                "c1", "Equipo B", 2500.0, 8000.0, 900.0, 300.0, 500000.0, "MXN"
            )
        )
        val payload = Backup.Payload(input, emptyList(), ThemeMode.DARK, "es", scenarios)
        val decoded = Backup.decode(Backup.encode(payload))!!
        assertEquals("MXN", decoded.input.currency)
        assertEquals(1200.0, decoded.input.inkPrice, 0.001)
        assertEquals(350000.0, decoded.input.equipPrice, 0.001)
        assertEquals(7, decoded.input.years)
        assertEquals(2, decoded.scenarios.size)
        assertTrue(decoded.scenarios[0] is Scenario.Videojet)
        assertEquals("VJ 1580", (decoded.scenarios[0] as Scenario.Videojet).input.modelId)
        assertTrue(decoded.scenarios[1] is Scenario.Competitor)
        assertEquals(2500.0, (decoded.scenarios[1] as Scenario.Competitor).inkMlMonth, 0.001)
    }

    @Test
    fun rechazaArchivosQueNoSonDeLaApp() {
        assertNull(Backup.decode("no es json"))
        assertNull(Backup.decode("{}"))
        assertNull(Backup.decode("""{"app":"Otra app","input":{}}"""))
    }

    @Test
    fun corrigeValoresFueraDeRangoYModeloDesconocido() {
        val raw = JSONObject()
            .put("app", "Consumo CIJ")
            .put("format", 1)
            .put("input", JSONObject()
                .put("modelId", "VJ inventado")
                .put("ink", "V9999")
                .put("chars", 9999)
                .put("lines", 99)
                .put("tempC", 999.0)
                .put("ppm", -5.0)
                .put("hoursDay", 99.0))
        val decoded = Backup.decode(raw.toString())!!
        assertTrue(decoded.input.modelId == com.calculadora.consumocij.data.VideojetData.models.first().id)
        assertEquals(500, decoded.input.chars)
        assertEquals(5, decoded.input.lines)
        assertEquals(60.0, decoded.input.tempC, 0.001)
        assertEquals(0.0, decoded.input.ppm, 0.001)
        assertEquals(24.0, decoded.input.hoursDay, 0.001)
    }

    @Test
    fun unModelo750NoConservaCartuchosDeCincoLitros() {
        val raw = JSONObject()
            .put("app", "Consumo CIJ")
            .put("input", JSONObject()
                .put("modelId", "VJ 1580")
                .put("ink", "V4201")
                .put("inkCartMl", 5000.0)
                .put("muCartMl", 5000.0))
        val decoded = Backup.decode(raw.toString())!!
        assertEquals(750.0, decoded.input.inkCartMl, 0.001)
        assertEquals(750.0, decoded.input.muCartMl, 0.001)
    }
}
