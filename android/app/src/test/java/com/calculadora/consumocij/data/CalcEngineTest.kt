package com.calculadora.consumocij.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del motor de cálculo contra los valores de referencia de los modelos
 * oficiales de Videojet. Son las mismas comprobaciones que
 * `tools/test_engine.js` ejecuta sobre el motor de la web, de modo que web y
 * app quedan fijadas a los mismos números.
 *
 *  · CIJ Makeup Usage Model v14      → consumo de solvente
 *  · CIJ TCO Calculator v35          → gotas/litro, gotas/carácter, mantenimiento
 *  · CIJ Line Speed Calculator v9.7  → velocidad de línea
 */
class CalcEngineTest {

    private val base = CalcInput(
        modelId = "VJ 1880+", nozzle = "60", ink = "V4201", inkBase = "MEK",
        font = "5x7 Single", chars = 12, lines = 1, stroke = 1, widthMm = 11.0,
        tempC = 30.0, ppm = 60.0, hoursDay = 16.0, daysWeek = 5, daysMonth = 22,
        efficiencyPct = 90.0, lineSpeed = 50.0, productWidthMm = 120.0, gapMm = 30.0,
        inkCartMl = 750.0, muCartMl = 750.0, envFactor = 1.0
    )

    private fun at(temp: Double = 30.0, nozzle: String = "60", model: String = "VJ 1880+",
                   ink: String = "V4201", font: String = "5x7 Single", chars: Int = 12,
                   lines: Int = 1, stroke: Int = 1, env: Double = 1.0): CalcInput =
        base.copy(tempC = temp, nozzle = nozzle, modelId = model, ink = ink,
            font = font, chars = chars, lines = lines, stroke = stroke, envFactor = env)

    /* ---------------------------------------------------------------
       1. Constantes de los modelos oficiales
       --------------------------------------------------------------- */
    @Test
    fun constantesDeLosModelos() {
        assertEquals(2.923 / 6.475, VideojetData.K_PUBLISHED, 1e-12)
        assertEquals(13.0, VideojetData.dropsChar["5x7"] ?: 0.0, 0.0)
        assertEquals(19.0, VideojetData.dropsChar["7x9"] ?: 0.0, 0.0)
        assertEquals(61.0, VideojetData.dropsChar["10x16"] ?: 0.0, 0.0)
        assertEquals(1_232_240_697.0, CalcEngine.dropsLiterOf("60", "VJ1000"), 1.0)
        assertEquals(883_500_036.0, CalcEngine.dropsLiterOf("70", "VJ1000"), 1.0)
    }

    @Test
    fun catalogoDeModelos() {
        assertTrue("debe haber un catalogo amplio de equipos", VideojetData.models.size >= 25)
        val m = CalcEngine.model("VJ 1880+")
        assertEquals("VJ 1880+", m.id)
    }

    /* ---------------------------------------------------------------
       2. Solvente — CIJ Makeup Usage Model v14
       --------------------------------------------------------------- */
    @Test
    fun horasPorLitroDeLasFichasOficiales() {
        val m = CalcEngine.model("VJ 1880+")
        // Documento «CIJ Makeup Usage Model v14_VJ 1880+_V4201.pdf»:
        // 286 h/L a 20 °C con boquilla de 60 µm y 222 h/L con 70 µm.
        assertEquals(286.0, CalcEngine.variant(m, "60").hrsL, 0.0)
        assertEquals(222.0, CalcEngine.variant(m, "70").hrsL, 0.0)
    }

    @Test
    fun consumoPublicadoAVeinteYTreintaGrados() {
        // Documentacion del proyecto: VJ 1880+ / V4201 a 30 °C = 5,21 ml/h
        // (el documento oficial publica 5,2).
        assertEquals(5.21, CalcEngine.compute(at(temp = 30.0)).muPerHour, 0.01)
        assertEquals(3.50, CalcEngine.compute(at(temp = 20.0)).muPerHour, 0.01)
    }

    @Test
    fun elConsumoCreceConLaTemperatura() {
        var anterior = 0.0
        for (t in 10..40) {
            val actual = CalcEngine.compute(at(temp = t.toDouble())).muPerHour
            assertTrue("el consumo debe crecer al subir la temperatura ($t °C)", actual > anterior)
            anterior = actual
        }
    }

    @Test
    fun losFactoresAmbientalesSonMultiplicadores() {
        val normal = CalcEngine.compute(at(temp = 30.0)).muPerHour
        val doble = CalcEngine.compute(at(temp = 30.0, env = 2.0)).muPerHour
        assertEquals(normal * 2.0, doble, 1e-9)
    }

    @Test
    fun factoresDeLasBasesDeSolvente() {
        assertEquals(1.0, VideojetData.inkBases.getValue("MEK").cons, 0.0)
        assertEquals(0.46, VideojetData.inkBases.getValue("Ethanol").cons, 0.0)
        assertEquals(2.2, VideojetData.inkBases.getValue("Acetone").cons, 0.0)
    }

    /* ---------------------------------------------------------------
       3. Tinta — CIJ TCO Calculator v35
       --------------------------------------------------------------- */
    @Test
    fun gotasPorMensaje() {
        // gotas/mensaje = gotas/car. × car/línea × líneas × stroke
        assertEquals(156.0, CalcEngine.compute(at()).dropsPerMessage, 0.0)
        assertEquals(1520.0,
            CalcEngine.compute(at(font = "7x9 Single", chars = 20, lines = 2, stroke = 2)).dropsPerMessage, 0.0)
    }

    @Test
    fun codigosQrUsanDoscientasVeinteGotas() {
        assertEquals(220.0, CalcEngine.compute(at(font = "QR Code 21")).dropsPerChar, 0.0)
    }

    @Test
    fun mlPorImpresionCoherenteConGotasPorLitro() {
        val r = CalcEngine.compute(at())
        assertEquals(r.dropsPerMessage / r.dropsPerLiter * 1000.0, r.inkMlPerPrint, 1e-12)
    }

    @Test
    fun laTintaNoDependeDeLaTemperatura() {
        assertEquals(CalcEngine.compute(at(temp = 30.0)).inkPerHour,
            CalcEngine.compute(at(temp = 40.0)).inkPerHour, 1e-12)
    }

    /* ---------------------------------------------------------------
       4. Velocidad — CIJ Line Speed Calculator v9.7
       --------------------------------------------------------------- */
    @Test
    fun velocidadDeLinea() {
        val r = CalcEngine.compute(at())
        assertEquals(1600.0, r.cps ?: 0.0, 0.0)
        assertEquals(243.84, r.nominalSpeed ?: 0.0, 0.01)
        // v máx (m/min) = (caracteres/seg ÷ caracteres/línea) × ancho_mm × 0,06
        assertEquals((r.cps!! / 12.0) * 11.0 * 0.06, r.maxLineSpeed!!, 1e-9)
        assertEquals(88.0, r.maxLineSpeed!!, 0.01)
    }

    @Test
    fun sinDatosDeVelocidadNoSeInventanCifras() {
        val sinDatos = VideojetData.models.asSequence().flatMap { m ->
            VideojetData.fonts.keys.map { f ->
                CalcEngine.compute(base.copy(modelId = m.id, nozzle = m.nozzles.first(), font = f))
            }
        }.firstOrNull { it.cps == null }
        assertNotNull("debe haber combinaciones equipo/fuente sin datos de velocidad", sinDatos)
        assertNull("sin datos la velocidad máxima debe ser null, no 0", sinDatos!!.maxLineSpeed)
    }

    /* ---------------------------------------------------------------
       5. Escalado temporal y cartuchos
       --------------------------------------------------------------- */
    @Test
    fun escaladoTemporalYCartuchos() {
        val r = CalcEngine.compute(at())
        assertEquals(r.muPerHour * 16.0, r.muDay, 1e-9)
        assertEquals(r.muPerHour * 16.0 * 22.0, r.muMonth, 1e-9)
        assertEquals(r.muPerHour * 16.0 * 5.0 * 52.0, r.muYear, 1e-9)
        assertEquals(750.0 / r.muPerHour, r.hoursPerMuCart, 1e-9)
        assertEquals(750.0 / r.inkMlPerPrint, r.printsPerInkCart, 1e-6)
        assertEquals(r.muMonth / r.inkMonth, r.ratio, 1e-9)
    }

    /* ---------------------------------------------------------------
       6. Mantenimiento — CIJ TCO v35 · Maintenance Setup
       --------------------------------------------------------------- */
    @Test
    fun mantenimientoDeLosModelosHr() {
        for (id in listOf("VJ 1620 HR", "VJ 1650 HR", "VJ 1650 UHS")) {
            val items = VideojetData.maintenance[id]
            assertNotNull("falta el mantenimiento de $id", items)
            assertEquals("$id debe tener el Core a 14 000 h", 14000.0, items!![0].hours, 0.0)
        }
    }

    /* ---------------------------------------------------------------
       7. Tintas sin datos oficiales
       --------------------------------------------------------------- */
    @Test
    fun tintasSinFactorOficial() {
        assertFalse("V4238 no tiene factor oficial",
            CalcEngine.compute(base.copy(ink = "V4238")).officialData)
        assertFalse("V4251 no tiene factor oficial",
            CalcEngine.compute(base.copy(ink = "V4251")).officialData)
        assertTrue("V4201 sí tiene factor oficial",
            CalcEngine.compute(base.copy(ink = "V4201")).officialData)
    }

    /* ---------------------------------------------------------------
       8. Tamaños de cartucho reales (TCO v35)
       --------------------------------------------------------------- */
    @Test
    fun tamanosDeCartuchoReales() {
        assertTrue("V4225 usa cartuchos de 700 ml",
            VideojetData.inkContainers["V4225"]!!.inkSizes.contains(700))
        assertTrue("V480 usa cartuchos de 600 ml",
            VideojetData.inkContainers["V480"]!!.inkSizes.contains(600))
        val v401 = VideojetData.inkContainers["V401"]!!.inkSizes
        assertTrue("V401 usa 750 y 5000 ml", v401.contains(750) && v401.contains(5000))
    }
}
