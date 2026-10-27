package com.duoc.plomoredsocial.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)

// El esquema claro conserva la identidad de Mygos en todas las pantallas.
private val LightColorScheme = lightColorScheme(
    primary = MygosBlue, onPrimary = Color.White,
    primaryContainer = MygosSky, onPrimaryContainer = MygosText,
    secondary = MygosSecondaryText, secondaryContainer = MygosSky,
    onSecondaryContainer = MygosBlue,
    background = MygosBackground, onBackground = MygosText,
    surface = Color.White, onSurface = MygosText,
    surfaceVariant = MygosSky, onSurfaceVariant = MygosSecondaryText,
    surfaceContainer = MygosBackground, surfaceContainerLow = Color.White,
    surfaceContainerHigh = MygosBackground,
    outline = MygosSecondaryText, outlineVariant = MygosOutline,
    error = Color(0xFFB3261E), errorContainer = Color(0xFFFCEAE8),
    onErrorContainer = Color(0xFF8C1D18)
)

@Composable
fun PlomoRedSocialTheme(
    darkTheme: Boolean = false,
    // Se conserva la opción, pero la paleta de la app no cambia con el dispositivo.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
