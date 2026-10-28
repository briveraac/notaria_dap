package com.duoc.plomoredsocial

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.duoc.plomoredsocial.ui.activities.*
import org.junit.Assert.*
import org.junit.Test

class DateTimeInputTest {
    @Test fun progressiveTypingKeepsTheCaretAtTheVisibleEnd() {
        for ((digits, transformation) in listOf("30122026" to DateInputTransformation, "1920" to TimeInputTransformation)) {
            for (length in 0..digits.length) {
                val input = numericInput(TextFieldValue(digits.take(length), TextRange(length)), digits.length)
                val visible = transformation.filter(AnnotatedString(input.text))
                assertEquals(length, input.selection.start)
                assertEquals(visible.text.length, visible.offsetMapping.originalToTransformed(input.selection.start))
            }
        }
        assertEquals("30-12-2026", DateInputTransformation.filter(AnnotatedString("30122026")).text.text)
        assertEquals("19:20", TimeInputTransformation.filter(AnnotatedString("1920")).text.text)
    }

    @Test fun offsetMappingsAreBoundedMonotonicAndRoundTripForEveryLength() {
        for ((max, transformation) in listOf(8 to DateInputTransformation, 4 to TimeInputTransformation)) {
            for (length in 0..max) {
                val result = transformation.filter(AnnotatedString("1".repeat(length)))
                val mapping = result.offsetMapping
                val forward = (0..length).map(mapping::originalToTransformed)
                val reverse = (0..result.text.length).map(mapping::transformedToOriginal)
                assertEquals(forward.sorted(), forward)
                assertEquals(reverse.sorted(), reverse)
                assertTrue(forward.all { it in 0..result.text.length })
                assertTrue(reverse.all { it in 0..length })
                for (offset in 0..length) {
                    assertEquals(offset, mapping.transformedToOriginal(mapping.originalToTransformed(offset)))
                }
            }
        }
    }

    @Test fun pastedSeparatorsAndOtherCharactersDoNotCorruptSelectionOrComposition() {
        val date = numericInput(TextFieldValue("30--12-2026", TextRange(11)), 8)
        assertEquals("30122026", date.text)
        assertEquals(TextRange(8), date.selection)
        val time = numericInput(TextFieldValue("19::20", TextRange(6)), 4)
        assertEquals("1920", time.text)
        assertEquals(TextRange(4), time.selection)
        val composed = numericInput(TextFieldValue("a12-3", TextRange(5, 1), TextRange(1, 5)), 8)
        assertEquals(TextRange(3, 0), composed.selection)
        assertEquals(TextRange(0, 3), composed.composition)
        assertEquals("1234", numericInput(TextFieldValue("123456", TextRange(6)), 4).text)
        assertEquals(TextRange(4), numericInput(TextFieldValue("123456", TextRange(6)), 4).selection)
    }

    @Test fun middleEditsKeepTheirLogicalPositionRatherThanJumpingToTheEnd() {
        val date = numericInput(TextFieldValue("30022026", TextRange(3)), 8)
        assertEquals(TextRange(3), date.selection)
        assertEquals(4, DateInputTransformation.filter(AnnotatedString(date.text))
            .offsetMapping.originalToTransformed(date.selection.start))
        val time = numericInput(TextFieldValue("1820", TextRange(2)), 4)
        assertEquals(TextRange(2), time.selection)
        assertEquals(3, TimeInputTransformation.filter(AnnotatedString(time.text))
            .offsetMapping.originalToTransformed(time.selection.start))
    }

    @Test fun bothSidesOfSeparatorsMapToTheSameDigitBoundary() {
        val date = DateInputTransformation.filter(AnnotatedString("30122026")).offsetMapping
        assertEquals(2, date.transformedToOriginal(2))
        assertEquals(2, date.transformedToOriginal(3))
        assertEquals(4, date.transformedToOriginal(5))
        assertEquals(4, date.transformedToOriginal(6))
        val time = TimeInputTransformation.filter(AnnotatedString("1920")).offsetMapping
        assertEquals(2, time.transformedToOriginal(2))
        assertEquals(2, time.transformedToOriginal(3))
    }
}
