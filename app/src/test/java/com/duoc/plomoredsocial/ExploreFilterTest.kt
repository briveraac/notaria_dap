package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.viewmodel.filterActivities
import org.junit.Assert.*
import org.junit.Test

class ExploreFilterTest {
    private val park = ActivityEntity("park", "Paseo por el parque", "  Quinta   Normal ",
        "Entrada de la plaza", "18:00", -33.4, -70.6, "owner")
    private val game = park.copy(id = "game", title = "Juegos de mesa", commune = "Maipú",
        meetingPoint = "Biblioteca municipal")
    private val all = listOf(park, game)

    @Test fun searchesEveryAvailableFieldAndNormalizesSpaces() {
        assertEquals(listOf(park), filterActivities(all, "  PASEO  ", ""))
        assertEquals(listOf(park), filterActivities(all, "quinta normal", ""))
        assertEquals(listOf(game), filterActivities(all, "BIBLIOTECA", ""))
        assertEquals(all, filterActivities(all, "   ", " "))
    }

    @Test fun combinesPartialCommuneAndQueryWithoutInventingResults() {
        assertEquals(listOf(park), filterActivities(all, "entrada   de", " quinta  nor "))
        assertTrue(filterActivities(all, "biblioteca", "quinta").isEmpty())
        assertTrue(filterActivities(all, "no existe", "").isEmpty())
        assertTrue(filterActivities(emptyList(), "", "").isEmpty())
    }
}
