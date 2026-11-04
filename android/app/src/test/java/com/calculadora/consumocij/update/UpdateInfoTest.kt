package com.calculadora.consumocij.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas del manifiesto de actualizaciones: la app solo debe aceptar un JSON
 * con versionCode positivo, version y una URL HTTPS; el resto se descarta para
 * no ofrecer una descarga no verificable.
 */
class UpdateInfoTest {

    private val json = """
        {
          "product": "Consumo CIJ",
          "version": "2.3.0",
          "versionCode": 8,
          "date": "2026-11-04",
          "apk": "ConsumoCIJ-2.3.0.apk",
          "apkUrl": "https://github.com/emanuelindustri-source/consumo-cij-calculadora/releases/download/v2.3.0/ConsumoCIJ-2.3.0.apk",
          "apkSizeBytes": 2040430,
          "apkSha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
          "notesUrl": "https://emanuelindustri-source.github.io/consumo-cij-calculadora/notas.html",
          "mandatory": false
        }
    """.trimIndent()

    @Test
    fun parseaUnManifiestoValido() {
        val info = UpdateInfo.parse(json)
        assertNotNull(info)
        assertEquals("2.3.0", info!!.version)
        assertEquals(8, info.versionCode)
        assertEquals(2040430L, info.apkSizeBytes)
        assertEquals("2026-11-04", info.date)
        assertEquals(false, info.mandatory)
    }

    @Test
    fun rechazaUrlSinHttps() {
        assertNull(UpdateInfo.parse(json.replace("https://github.com", "http://github.com")))
    }

    @Test
    fun rechazaVersionCodeNoPositivo() {
        assertNull(UpdateInfo.parse(json.replace("\"versionCode\": 8", "\"versionCode\": 0")))
    }

    @Test
    fun rechazaJsonSinVersion() {
        assertNull(UpdateInfo.parse("""{"versionCode": 8, "apkUrl": "https://example.com/a.apk"}"""))
        assertNull(UpdateInfo.parse("no es json"))
    }

    @Test
    fun tamanoLegibleEnMegabytes() {
        val info = UpdateInfo.parse(json)!!
        assertEquals("1.9 MB", info.sizeLabel())
    }
}
