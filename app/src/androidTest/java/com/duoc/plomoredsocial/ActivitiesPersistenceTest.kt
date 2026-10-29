package com.duoc.plomoredsocial

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.ActivityMember
import com.duoc.plomoredsocial.model.MygosDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ActivitiesPersistenceTest {
    @Test
    fun creationThroughExistingViewModelUpdatesTheSharedCatalogue() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val database = MygosDatabase.getInstance(context)
        val organizer = requireNotNull(com.duoc.plomoredsocial.repository.RoomAuthRepository(
            database, context.assets).getUserById(com.duoc.plomoredsocial.model.PRIMARY_DEMO_ID))
        val id = "main007-create-" + UUID.randomUUID()
        val activity = ActivityEntity(id, "Creación MAIN-007", "Maipú", "Plaza",
            "31-12-2099, 18:00", -33.51, -70.76, organizer.id)
        val store = androidx.lifecycle.ViewModelStore()
        lateinit var viewModel: com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel
        lateinit var catalogue: com.duoc.plomoredsocial.viewmodel.HomeViewModel
        try {
            instrumentation.runOnMainSync {
                viewModel = androidx.lifecycle.ViewModelProvider(store,
                    androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(
                        context.applicationContext as android.app.Application))[
                    com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel::class.java]
                catalogue = androidx.lifecycle.ViewModelProvider(store,
                    com.duoc.plomoredsocial.viewmodel.HomeViewModel.Factory(
                        com.duoc.plomoredsocial.repository.RoomActivityRepository(database.activityDao())))[
                    com.duoc.plomoredsocial.viewmodel.HomeViewModel::class.java]
                viewModel.create(activity) {}
            }
            kotlinx.coroutines.withTimeout(10000) {
                var ready = false
                while (!ready) {
                    instrumentation.runOnMainSync {
                        ready = !viewModel.saving && catalogue.uiState.activities.any { it.id == id }
                    }
                    if (!ready) kotlinx.coroutines.delay(20)
                }
            }
            assertTrue(database.activityDao().observeActivities().first().contains(activity))
            instrumentation.runOnMainSync { assertFalse(viewModel.error) }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            database.openHelper.writableDatabase.execSQL(
                "DELETE FROM activities WHERE id = ?", arrayOf(id))
        }
    }

    @Test
    fun migrationPreservesContactsAndActivitiesSurviveReopening() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "activities-test-${UUID.randomUUID()}.db"
        try {
            // Crear una base versión 1 para ejecutar y validar la migración real con Room.
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, alias TEXT NOT NULL, email TEXT NOT NULL, reason TEXT NOT NULL, message TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                old.execSQL("INSERT INTO contacts VALUES (1, 'Vecino', 'test@example.com', 'QUERY', 'Consulta de prueba', 1)")
                old.version = 1
            }
            fun open() = Room.databaseBuilder(context, MygosDatabase::class.java, name)
                .addMigrations(MygosDatabase.MIGRATION_1_2, MygosDatabase.MIGRATION_2_3, MygosDatabase.MIGRATION_3_4, MygosDatabase.MIGRATION_4_5, MygosDatabase.MIGRATION_5_6).build()
            val activity = ActivityEntity("activity", "Jugar", "Maipú", "Entrada de la plaza",
                "30/10/2026, 18:00", -33.51, -70.76, "organizer")
            val firstDatabase = open()
            try {
                val database = firstDatabase
                assertEquals("Vecino", database.contactDao().getById(1)?.alias)
                database.activityDao().create(activity)
                database.activityDao().join(ActivityMember("activity", "neighbor"))
                database.activityDao().join(ActivityMember("activity", "neighbor"))
            } finally { firstDatabase.close() }
            val reopenedDatabase = open()
            try {
                val database = reopenedDatabase
                val dao = database.activityDao()
                assertEquals(listOf(activity), dao.observeActivities().first())
                assertEquals(1, dao.observeMembers().first().size)
                dao.leave("activity", "neighbor")
                assertTrue(dao.observeMembers().first().isEmpty())
                assertEquals(1, database.contactDao().getAll().size)
            } finally { reopenedDatabase.close() }
        } finally { context.deleteDatabase(name) }
    }
}
