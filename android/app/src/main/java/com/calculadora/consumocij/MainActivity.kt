package com.calculadora.consumocij

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.calculadora.consumocij.i18n.I18n
import com.calculadora.consumocij.ui.CalculatorScreen
import com.calculadora.consumocij.util.ReportIssue
import com.calculadora.consumocij.ui.theme.VideojetTheme

/**
 * Calculadora de consumo de tinta y solvente — Videojet CIJ.
 *
 * · Aplicación nativa (Kotlin + Jetpack Compose, Material 3)
 * · Android 14 (API 34) como versión mínima
 * · 100 % offline: el manifiesto NO declara el permiso INTERNET
 * · Interfaz adaptable: teléfonos, phablets, tabletas, TV, apaisado,
 *   distintas resoluciones/densidades y temas Claro / Oscuro / AMOLED
 */
class MainActivity : ComponentActivity() {

    private val vm: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        I18n.init(this)
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            ReportIssue.saveCrash(this, e)
            prev?.uncaughtException(t, e)
        }
        enableEdgeToEdge()
        setContent {
            // vm.themeMode es estado de Compose: el tema se recompone al cambiarlo
            VideojetTheme(themeMode = vm.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalculatorScreen(vm = vm)
                }
            }
        }
    }
}
