package com.duoc.plomoredsocial.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.duoc.plomoredsocial.R

// Archivos locales con pesos reales: la tipografía funciona también sin conexión.
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

private fun jakartaStyle(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

// Se definen los quince roles para evitar que algún componente vuelva a la fuente del sistema.
// Se mantienen los tamaños existentes y el texto sigue respondiendo a la escala de accesibilidad.
val Typography = Typography(
    displayLarge = jakartaStyle(44, 52, FontWeight.Bold),
    displayMedium = jakartaStyle(36, 44, FontWeight.Bold),
    displaySmall = jakartaStyle(32, 40, FontWeight.Bold),
    headlineLarge = jakartaStyle(30, 38, FontWeight.Bold),
    headlineMedium = jakartaStyle(24, 32, FontWeight.Bold),
    headlineSmall = jakartaStyle(22, 30, FontWeight.Bold),
    titleLarge = jakartaStyle(22, 28, FontWeight.SemiBold),
    titleMedium = jakartaStyle(18, 26, FontWeight.SemiBold),
    titleSmall = jakartaStyle(14, 20, FontWeight.SemiBold),
    bodyLarge = jakartaStyle(16, 24, FontWeight.Normal),
    bodyMedium = jakartaStyle(14, 21, FontWeight.Normal),
    bodySmall = jakartaStyle(12, 18, FontWeight.Normal),
    labelLarge = jakartaStyle(14, 20, FontWeight.SemiBold),
    labelMedium = jakartaStyle(12, 16, FontWeight.Medium),
    labelSmall = jakartaStyle(11, 16, FontWeight.Medium)
)
