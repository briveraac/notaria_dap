package com.duoc.plomoredsocial.ui.activities

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Conserva selección y composición contando solo los dígitos anteriores a cada extremo.
// También admite pegar texto ya formateado, reemplazar selecciones y mover el cursor.
internal fun numericInput(value: TextFieldValue, maxDigits: Int): TextFieldValue {
    val digits = value.text.filter { it in '0'..'9' }.take(maxDigits)
    fun position(offset: Int) = value.text.take(offset.coerceIn(0, value.text.length))
        .count { it in '0'..'9' }.coerceAtMost(digits.length)
    fun range(range: TextRange) = TextRange(position(range.start), position(range.end))
    return TextFieldValue(digits, range(value.selection),
        value.composition?.let(::range)?.takeUnless { it.collapsed })
}

// Los separadores son visuales: borrar junto a ellos siempre borra un dígito real.
internal class NumericSeparatorTransformation(
    private val boundaries: List<Int>, private val separator: Char
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val cuts = boundaries.filter { it > 0 && it < text.length }
        val formatted = buildString {
            text.text.forEachIndexed { index, character ->
                if (index in cuts) append(separator)
                append(character)
            }
        }
        return TransformedText(AnnotatedString(formatted), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val position = offset.coerceIn(0, text.length)
                return position + cuts.count { it <= position }
            }
            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset.coerceIn(0, formatted.length)).count { it != separator }
                    .coerceAtMost(text.length)
        })
    }
}

internal val DateInputTransformation = NumericSeparatorTransformation(listOf(2, 4), '-')
internal val TimeInputTransformation = NumericSeparatorTransformation(listOf(2), ':')
