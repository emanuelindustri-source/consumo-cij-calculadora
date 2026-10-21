package com.calculadora.consumocij.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Temas de la aplicación.
 *
 * · CLARO ......... pantallas LCD / IPS en ambientes luminosos
 * · OSCURO ........ gris oscuro (evita "halos" en LCD con negros profundos)
 * · AMOLED ........ negro puro #000000 (los píxeles se apagan: ahorra batería y
 *                   reduce burn-in en paneles OLED/AMOLED)
 * · AUTOMÁTICO .... sigue la configuración del sistema (tema claro/oscuro)
 *
 * Android no expone una API pública fiable para distinguir AMOLED de LCD,
 * por eso el modo AMOLED se ofrece como selección del usuario (y se recuerda).
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

data class ExtraColors(
    val isAmoled: Boolean,
    val accent: Color,
    val accent2: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val chartGrid: Color
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(
        isAmoled = false,
        accent = Color(0xFF2DD4BF),
        accent2 = Color(0xFF38BDF8),
        success = Color(0xFF4ADE80),
        warning = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        chartGrid = Color(0xFF24344F)
    )
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7C71),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F2EC),
    onPrimaryContainer = Color(0xFF04222A),
    secondary = Color(0xFF0369A1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3EEFF),
    onSecondaryContainer = Color(0xFF04202E),
    background = Color(0xFFF4F7FD),
    onBackground = Color(0xFF12203A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12203A),
    surfaceVariant = Color(0xFFEDF2FA),
    onSurfaceVariant = Color(0xFF405372),
    outline = Color(0xFFB3C2DA),
    error = Color(0xFFC0332B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF04222A),
    primaryContainer = Color(0xFF123B3A),
    onPrimaryContainer = Color(0xFFB7F2EC),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF04202E),
    secondaryContainer = Color(0xFF123247),
    onSecondaryContainer = Color(0xFFD3EEFF),
    background = Color(0xFF121A2B),
    onBackground = Color(0xFFE8EEFC),
    surface = Color(0xFF16233D),
    onSurface = Color(0xFFE8EEFC),
    surfaceVariant = Color(0xFF1B2A47),
    onSurfaceVariant = Color(0xFFA8B8D4),
    outline = Color(0xFF33486B),
    error = Color(0xFFF87171)
)

/* Negro puro para AMOLED: los píxeles negros se apagan de verdad */
private val AmoledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF0B0B0B),
    onSurfaceVariant = Color(0xFFB8B8B8),
    outline = Color(0xFF2A2A2A)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 15.sp)
)

@Composable
fun VideojetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = themeMode == ThemeMode.AMOLED || (themeMode == ThemeMode.SYSTEM && systemDark && false)
    val colors = when {
        amoled -> AmoledColors
        dark -> DarkColors
        else -> LightColors
    }
    val extra = ExtraColors(
        isAmoled = amoled,
        accent = if (dark) Color(0xFF2DD4BF) else Color(0xFF0E7C71),
        accent2 = if (dark) Color(0xFF38BDF8) else Color(0xFF0369A1),
        success = if (dark) Color(0xFF4ADE80) else Color(0xFF178043),
        warning = if (dark) Color(0xFFFBBF24) else Color(0xFF9A6B00),
        danger = if (dark) Color(0xFFF87171) else Color(0xFFC0332B),
        chartGrid = if (amoled) Color(0xFF232323) else if (dark) Color(0xFF24344F) else Color(0xFFD3DDED)
    )
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = content
        )
    }
}
