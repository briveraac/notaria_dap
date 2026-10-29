package com.duoc.plomoredsocial

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

// Datos exclusivos de pruebas en memoria: no se agregan cuentas ni actividades a la aplicación.
class ActivityManagementTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun event(id: String, capacity: Int, owner: String = "owner") =
        ActivityEntity(id, "Actividad $id", "Maipú", "Plaza", "31-12-2099, 18:00",
            null, null, owner, "Descripción conservada", capacity)

    @Test fun capacitiesOrganizerDuplicatesAndReleasedPlaces() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        try {
            val dao = db.activityDao()
            for (capacity in listOf(1, 5, 10)) {
                val activity = event("capacity-$capacity", capacity)
                dao.create(activity)
                assertTrue(dao.join(ActivityMember(activity.id, "owner")))
                // Simula una inscripción histórica del organizador sin contar dos cupos.
                db.openHelper.writableDatabase.execSQL("INSERT OR IGNORE INTO activity_members VALUES (?, ?)",
                    arrayOf(activity.id, "owner"))
                assertEquals(1, activity.participantCount(dao.observeMembers().first()))
                dao.leave(activity.id, "owner")
                assertEquals(1, activity.participantCount(dao.observeMembers().first()))
                repeat(capacity - 1) { index ->
                    assertTrue(dao.join(ActivityMember(activity.id, "guest-$index")))
                    assertTrue(dao.join(ActivityMember(activity.id, "guest-$index")))
                }
                assertEquals(capacity, activity.participantCount(dao.observeMembers().first()))
                assertFalse(dao.join(ActivityMember(activity.id, "extra")))
                if (capacity > 1) {
                    dao.leave(activity.id, "guest-0")
                    assertEquals(capacity - 1, activity.participantCount(dao.observeMembers().first()))
                    assertTrue(dao.join(ActivityMember(activity.id, "extra")))
                }
                assertEquals(capacity, dao.find(activity.id)?.capacity)
            }
            for (invalid in listOf(-1, 0, 11, 100)) {
                assertFalse(ActivityFormValidator.isValid(event("invalid", invalid)))
                try { dao.create(event("invalid-$invalid", invalid)); fail("Capacidad inválida aceptada") }
                catch (_: IllegalArgumentException) { }
            }
            assertFalse(dao.join(ActivityMember("missing", "guest")))
        } finally { db.close() }
    }

    @Test fun concurrentJoinsCannotExceedCapacity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        try {
            val dao = db.activityDao()
            val activity = event("concurrent", 5)
            dao.create(activity)
            val accepted = coroutineScope {
                (1..30).map { async(Dispatchers.IO) { dao.join(ActivityMember(activity.id, "guest-$it")) } }.awaitAll()
            }
            assertEquals(4, accepted.count { it })
            assertEquals(5, activity.participantCount(dao.observeMembers().first()))
        } finally { db.close() }
    }

    @Test fun profileQueriesIsolateAccountsAndDeletionIsAtomicAndAuthorized() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        try {
            val dao = db.activityDao()
            val a = event("owned-a", 5, "account-a")
            val b = event("owned-b", 10, "account-b")
            dao.create(a); dao.create(b)
            dao.join(ActivityMember(a.id, "account-b"))
            dao.join(ActivityMember(b.id, "account-a"))
            db.openHelper.writableDatabase.execSQL("INSERT INTO activity_members VALUES ('owned-a','account-a')")
            assertEquals(listOf(a), dao.observeCreated("account-a").first())
            assertEquals(listOf(b), dao.observeCreated("account-b").first())
            assertEquals(listOf(b), dao.observeParticipating("account-a").first())
            assertEquals(listOf(a), dao.observeParticipating("account-b").first())
            assertTrue(dao.observeCreated("other-account").first().isEmpty())
            assertTrue(dao.observeParticipating("other-account").first().isEmpty())
            dao.leave(b.id, "account-a")
            assertTrue(dao.observeParticipating("account-a").first().isEmpty())
            assertFalse(dao.deleteAsOrganizer(a.id, "account-b"))
            assertFalse(dao.deleteAsOrganizer(a.id, ""))
            assertEquals(a, dao.find(a.id))
            assertEquals(2, dao.observeMembers().first().count { it.activityId == a.id })
            assertTrue(dao.deleteAsOrganizer(a.id, "account-a"))
            assertNull(dao.find(a.id))
            assertTrue(dao.observeMembers().first().none { it.activityId == a.id })
            assertTrue(dao.observeParticipating("account-b").first().isEmpty())
            assertTrue(dao.observeCreated("account-a").first().isEmpty())
            assertEquals(listOf(b), dao.observeActivities().first())
        } finally { db.close() }
    }

    @Test fun failedDeleteRollsBackMembershipRemoval() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        try {
            val dao = db.activityDao()
            dao.create(event("rollback", 5))
            dao.join(ActivityMember("rollback", "guest"))
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_test_delete BEFORE DELETE ON activities BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
            try { dao.deleteAsOrganizer("rollback", "owner"); fail("La eliminación debía fallar") }
            catch (_: android.database.sqlite.SQLiteException) { }
            assertNotNull(dao.find("rollback"))
            assertEquals(listOf(ActivityMember("rollback", "guest")), dao.observeMembers().first())
        } finally { db.close() }
    }
}
