package com.calculadora.consumocij.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas de contenedores del CIJ TCO Calculator v35.
 *
 * El TCO marca seis modelos como «750mL Only»: no admiten 1 L ni 5 L aunque la
 * tinta elegida tenga esos tamaños en el catálogo. Es la corrección del aviso
 * «este equipo usa cartuchos de 5000 ml» que no era cierto.
 */
class ContainerRulesTest {

    @Test
    fun losSeisModelos750SoloAdmiten750() {
        val modelos = listOf("VJ 1210", "VJ 1220", "VJ 1240", "VJ 1280", "VJ 1510", "VJ 1580")
        for (m in modelos) {
            assertEquals("tinta $m", listOf(750), VideojetData.inkSizesFor(m, "V401"))
            assertEquals("solvente $m", listOf(750), VideojetData.muSizesFor(m, "V401"))
            assertEquals("tinta $m", listOf(750), VideojetData.inkSizesFor(m, "V4201"))
        }
    }

    @Test
    fun simplicidadNoAdmiteCincoLitros() {
        for (m in listOf("VJ 1240", "VJ 1280", "VJ 1580", "VJ 1580+", "VJ 1580C",
            "VJ 1880", "VJ 1880+", "VJ 1880 UHS", "VJ 1880 HR")) {
            assertFalse("$m no debe ofrecer 5 L", VideojetData.inkSizesFor(m, "V401").contains(5000))
            assertFalse("$m no debe ofrecer 5 L de solvente", VideojetData.muSizesFor(m, "V401").contains(5000))
        }
    }

    @Test
    fun modelos1000AdmitenCincoLitrosSoloConTintas5L() {
        assertEquals(listOf(750, 5000), VideojetData.inkSizesFor("VJ 1520", "V401"))
        assertEquals(listOf(750, 5000), VideojetData.muSizesFor("VJ 1520", "V401"))
        // V4215 no está en la lista del grupo «std»: se usan los tamaños del modelo.
        assertEquals(listOf(750), VideojetData.inkSizesFor("VJ 1520", "V4215"))
        assertEquals(listOf(750, 1000), VideojetData.inkSizesFor("VJ 1880+", "V4201"))
        assertEquals(listOf(750, 5000), VideojetData.inkSizesFor("VJ 1620 UHS", "V505"))
    }

    @Test
    fun el1710UsaSeiscientosDeTintaYSetecientosCincuentaDeSolvente() {
        assertEquals(listOf(600), VideojetData.inkSizesFor("VJ 1710", "V480"))
        assertEquals(listOf(750), VideojetData.muSizesFor("VJ 1710", "V480"))
    }

    @Test
    fun losModelos750EstanMarcadosEnLaListaOficial() {
        assertTrue(VideojetData.only750Models.containsAll(
            listOf("VJ 1210", "VJ 1220", "VJ 1240", "VJ 1280", "VJ 1510", "VJ 1580")
        ))
        assertEquals(6, VideojetData.only750Models.size)
    }
}
