package com.duoc.plomoredsocial

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LocalAuthRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun accountsSurviveReopeningAndKeepTheirActivityAssociations() = runBlocking {
        val name = "local-auth-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, MygosDatabase::class.java, name).build()
        var firstId = ""
        var secondId = ""
        try {
            val first = open()
            try {
                val auth = RoomAuthRepository(first, context.assets)
                val a = auth.register(" Ana ", " ANA@example.com ", " Una frase larga ñ ")
                val b = auth.register("Bea", "bea@example.com", "Otra frase muy larga")
                firstId = a.id; secondId = b.id
                assertNotEquals(a.id, b.id)
                assertFalse(a.isDemo)
                assertEquals("Ana", a.name)
                assertEquals(a.id, auth.authenticate("ana@EXAMPLE.COM", " Una frase larga ñ ")?.id)
                assertNull(auth.authenticate("ana@example.com", "Una frase larga ñ"))
                assertNull(auth.authenticate("ana@example.com", "incorrecta"))
                val row = requireNotNull(first.userDao().findById(a.id))
                assertNotEquals(" Una frase larga ñ ", row.passwordHash)
                assertEquals(PasswordDeriver.ALGORITHM, row.algorithm)
                assertEquals(600_000, row.iterations)
                assertEquals(32, row.salt.length)
                first.activityDao().create(ActivityEntity("event", "Encuentro", "Maipú", "Plaza",
                    "31-12-2099, 18:00", null, null, a.id, "Actividad local"))
                first.activityDao().join(ActivityMember("event", b.id))
            } finally { first.close() }
            val reopened = open()
            try {
                val auth = RoomAuthRepository(reopened, context.assets)
                assertEquals(firstId, auth.getUserById(firstId)?.id)
                assertEquals(secondId, auth.authenticate(" BEA@example.com ", "Otra frase muy larga")?.id)
                assertEquals(firstId, reopened.activityDao().observeActivities().first().single().organizerId)
                assertEquals(secondId, reopened.activityDao().observeMembers().first().single().userId)
                // Quitar una inscripción no modifica al organizador ni al usuario.
                reopened.activityDao().leave("event", secondId)
                assertTrue(reopened.activityDao().observeMembers().first().isEmpty())
                assertNotNull(auth.getUserById(firstId))
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun normalizedDuplicatesAreRejectedIncludingDemoAndConcurrentRegistrations() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        try {
            val first = RoomAuthRepository(database, context.assets)
            val second = RoomAuthRepository(database, context.assets)
            val results = coroutineScope {
                listOf(
                    async { runCatching { first.register("Una", " Vecina@example.com ", "Una contraseña larga") } },
                    async { runCatching { second.register("Otra", "VECINA@EXAMPLE.COM", "Otra contraseña larga") } }
                ).awaitAll()
            }
            assertEquals(1, results.count { it.isSuccess })
            assertEquals(1, results.count { it.exceptionOrNull() is DuplicateEmailException })
            try {
                first.register("Duplicada", " DEMO@MYGOS.CL ", "Una contraseña larga")
                fail("El correo demo debe estar reservado.")
            } catch (_: DuplicateEmailException) { }
            val demo = first.authenticate(" Demo@MYGOS.cl ", "Mygos2026!")
            assertEquals("demo-001", demo?.id)
            assertTrue(requireNotNull(demo).isDemo)
            assertEquals("demo-001", second.getUserById("demo-001")?.id)
            assertNull(first.authenticate("demo@mygos.cl", "incorrecta"))
        } finally { database.close() }
    }
}
