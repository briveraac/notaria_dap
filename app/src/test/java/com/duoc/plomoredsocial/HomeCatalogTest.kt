package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.model.ActivityStatus
import com.duoc.plomoredsocial.model.HomeActivity
import com.duoc.plomoredsocial.model.InterestCategory
import com.duoc.plomoredsocial.repository.DemoHomeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

// Revisa que las actividades de ejemplo tengan cantidades y estados coherentes.
class HomeCatalogTest {
    @Test fun demoCatalogHasEightActivitiesWithConsistentStatusesAndCapacity() = runBlocking {
        val activities = DemoHomeRepository().observeActivities().first()
        assertEquals(8, activities.size)
        assertEquals(8, activities.map { it.id }.toSet().size)
        assertEquals(ActivityStatus.entries.toSet(), activities.map { it.status }.toSet())
        activities.forEach {
            assertTrue(it.capacity in 1..10)
            assertTrue(it.participants in 0..it.capacity)
            when (it.status) {
                ActivityStatus.OPEN -> assertTrue(it.participants < it.capacity)
                ActivityStatus.IN_PROGRESS -> assertTrue(it.participants > 0)
                ActivityStatus.COMPLETE -> assertEquals(it.capacity, it.participants)
            }
        }
    }

    // Comprueba que se rechacen cupos imposibles o estados que contradicen el contador.
    @Test fun invalidCapacityCountsAndStatusesAreRejected() {
        listOf(
            Triple(ActivityStatus.OPEN, 1, 11),
            Triple(ActivityStatus.OPEN, -1, 5),
            Triple(ActivityStatus.OPEN, 6, 5),
            Triple(ActivityStatus.OPEN, 5, 5),
            Triple(ActivityStatus.IN_PROGRESS, 0, 5),
            Triple(ActivityStatus.COMPLETE, 4, 5)
        ).forEach { (status, participants, capacity) ->
            assertThrows(IllegalArgumentException::class.java) {
                HomeActivity("invalid", "Ejemplo", InterestCategory.SPORTS,
                    "Actividad de prueba", status, participants, capacity)
            }
        }
    }
}
