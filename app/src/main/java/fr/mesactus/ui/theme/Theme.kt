package fr.mesactus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import fr.mesactus.data.ThemeMode

// Palette « papier journal » : fond crème, encre bleu nuit, accent rouge.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1D3557),
    onPrimary = Color.White,
    secondary = Color(0xFFC0392B),
    onSecondary = Color.White,
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFECE7DE),
    onSurfaceVariant = Color(0xFF6B655C),
    outlineVariant = Color(0xFFDDD6CA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DBCE3),
    onPrimary = Color(0xFF0E1B2C),
    secondary = Color(0xFFF08A7E),
    onSecondary = Color(0xFF2B0B07),
    background = Color(0xFF111315),
    onBackground = Color(0xFFECE9E4),
    surface = Color(0xFF1B1E21),
    onSurface = Color(0xFFECE9E4),
    surfaceVariant = Color(0xFF26292D),
    onSurfaceVariant = Color(0xFFA6A19A),
    outlineVariant = Color(0xFF34383D),
)

private val Base = Typography()

private val EditorialTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = Base.bodyLarge.copy(fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge,
    labelMedium = Base.labelMedium.copy(letterSpacing = 0.3.sp),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
)

@Composable
fun MesActusTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = EditorialTypography,
        content = content,
    )
}
