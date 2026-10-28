package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.ActivityFormValidator as V
import java.util.GregorianCalendar
import org.junit.Assert.*
import org.junit.Test

class ActivityFormValidatorTest {
    private val now = GregorianCalendar(2026, 9, 28, 12, 30, 15).timeInMillis
    private val activity = ActivityEntity("a", "Paseo", "Maipú", "Entrada de la plaza",
        "30-10-2026, 19:20", null, null, "owner", "Compartir una caminata.")

    @Test fun numericInputAddsSeparatorsAndIgnoresOtherCharacters() {
        assertEquals("30-10-2026", V.formatDate("30102026"))
        assertEquals("30-10-2026", V.formatDate("30a10/2026extra99"))
        assertEquals("30-1", V.formatDate("301"))
        assertEquals("19:20", V.formatTime("1920"))
        assertEquals("19:20", V.formatTime("19h20x9"))
        assertEquals("19", V.formatTime("19"))
    }

    @Test fun strictCalendarRejectsImpossibleDatesAndAcceptsLeapYears() {
        assertNull(V.dateError("29-02-2028", now))
        listOf("29-02-2027", "29-02-2100", "31-04-2099", "00-12-2099",
            "30-13-2099", "01-01-0000", "1-1-2099").forEach { assertNotNull(V.dateError(it, now)) }
        assertNotNull(V.dateError("27-10-2026", now))
        assertNull(V.dateError("28-10-2026", now))
    }

    @Test fun timeUses24HoursAndRejectsPastMoments() {
        listOf("00:00", "23:59", "19:20").forEach { assertNull(V.timeError(it)) }
        listOf("24:00", "23:60", "9:20", "-1:00", "1900").forEach { assertNotNull(V.timeError(it)) }
        assertNotNull(V.scheduleError("28-10-2026", "12:30", now))
        assertNotNull(V.scheduleError("28-10-2026", "10:00", now))
        assertNull(V.scheduleError("28-10-2026", "12:31", now))
        assertNull(V.scheduleError("29-10-2026", "00:00", now))
    }

    @Test fun descriptionLimitAndManualLocationDoNotRequireFakeCoordinates() {
        assertNull(V.descriptionError(""))
        assertNull(V.descriptionError("a".repeat(2000)))
        assertNotNull(V.descriptionError("a".repeat(2001)))
        assertTrue(V.isValid(activity, now))
        assertFalse(V.isValid(activity.copy(description = "a".repeat(2001)), now))
        assertFalse(V.isValid(activity.copy(meetingPoint = "  "), now))
        assertFalse(V.isValid(activity.copy(latitude = -33.5), now))
        assertFalse(V.isValid(activity.copy(latitude = 100.0, longitude = -70.6), now))
        assertTrue(V.isValid(activity.copy(latitude = -33.5, longitude = -70.6), now))
    }

    @Test fun calendarValidationUsesTheDeviceTimezoneWithoutRequiringMidnight() {
        val previous = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Santiago"))
            val localNow = GregorianCalendar(2026, 9, 28, 12, 0).timeInMillis
            assertNull(V.dateError("05-09-2027", localNow))
            assertNull(V.scheduleError("05-09-2027", "12:00", localNow))
        } finally { java.util.TimeZone.setDefault(previous) }
    }
}
