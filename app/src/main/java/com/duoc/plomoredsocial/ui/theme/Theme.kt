package com.duoc.plomoredsocial.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// El azul oscuro se usa en enlaces pequeños; el azul oficial se conserva en acciones y marca.
internal val MygosLightScheme = lightColorScheme(
    primary = MygosDarkBlue, onPrimary = MygosCardWhite,
    primaryContainer = MygosSoftAccent, onPrimaryContainer = MygosText,
    secondary = MygosDarkBlue, onSecondary = MygosCardWhite,
    secondaryContainer = MygosLavender, onSecondaryContainer = MygosText,
    tertiary = MygosSuccessText, onTertiary = MygosCardWhite,
    tertiaryContainer = MygosSuccess, onTertiaryContainer = MygosSuccessText,
    background = MygosBackground, onBackground = MygosText,
    surface = MygosCardWhite, onSurface = MygosText,
    surfaceVariant = MygosSoftAccent, onSurfaceVariant = MygosMutedText,
    surfaceContainer = MygosBackground, surfaceContainerLowest = MygosCardWhite,
    surfaceContainerLow = MygosCardWhite, surfaceContainerHigh = MygosSoftAccent,
    surfaceContainerHighest = MygosSoftAccent,
    outline = MygosMutedText, outlineVariant = MygosOutline,
    error = MygosErrorText, onError = MygosCardWhite,
    errorContainer = MygosError, onErrorContainer = MygosOnError
)

internal val MygosDarkScheme = darkColorScheme(
    primary = MygosLavender, onPrimary = MygosDarkBackground,
    primaryContainer = MygosDarkAccent, onPrimaryContainer = MygosDarkText,
    secondary = MygosDarkText, onSecondary = MygosDarkBackground,
    secondaryContainer = MygosDarkLavender, onSecondaryContainer = MygosDarkText,
    tertiary = MygosSuccess, onTertiary = MygosDarkBackground,
    tertiaryContainer = MygosDarkSuccess, onTertiaryContainer = MygosSuccess,
    background = MygosDarkBackground, onBackground = MygosDarkText,
    surface = MygosDarkSurface, onSurface = MygosDarkText,
    surfaceVariant = MygosDarkSurfaceHigh, onSurfaceVariant = MygosDarkMuted,
    surfaceContainerLowest = MygosDarkBackground, surfaceContainerLow = MygosDarkSurface,
    surfaceContainer = MygosDarkSurface, surfaceContainerHigh = MygosDarkSurfaceHigh,
    surfaceContainerHighest = MygosDarkSurfaceHigh,
    outline = MygosDarkMuted, outlineVariant = MygosDarkOutline,
    error = MygosError, onError = MygosOnError,
    errorContainer = MygosDarkError, onErrorContainer = MygosDarkText
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun PlomoRedSocialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Se conserva el parámetro por compatibilidad; el fondo de pantalla no cambia la identidad.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MygosDarkScheme else MygosLightScheme,
        typography = Typography,
        shapes = Shapes(extraSmall = RoundedCornerShape(14.dp), small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(32.dp), extraLarge = RoundedCornerShape(36.dp)),
        content = content
    )
}
