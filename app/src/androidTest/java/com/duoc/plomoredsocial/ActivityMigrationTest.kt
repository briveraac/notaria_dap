package com.duoc.plomoredsocial

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ActivityMigrationTest {
    @Test fun version2KeepsCoordinatesMembersAndContactsAndPersistsManualLocation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration23-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, alias TEXT NOT NULL, email TEXT NOT NULL, reason TEXT NOT NULL, message TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                old.execSQL("INSERT INTO contacts VALUES(1,'Vecino','test@example.com','QUERY','Consulta',1)")
                old.execSQL("CREATE TABLE activities (id TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,commune TEXT NOT NULL,meetingPoint TEXT NOT NULL,schedule TEXT NOT NULL,latitude REAL NOT NULL,longitude REAL NOT NULL,organizerId TEXT NOT NULL)")
                old.execSQL("INSERT INTO activities VALUES('old','Actividad anterior','Maipú','Plaza','30/10/2026, 18:00',-33.51,-70.76,'owner')")
                old.execSQL("CREATE TABLE activity_members (activityId TEXT NOT NULL,userId TEXT NOT NULL,PRIMARY KEY(activityId,userId))")
                old.execSQL("INSERT INTO activity_members VALUES('old','neighbor')")
                old.version = 2
            }
            fun open() = Room.databaseBuilder(context, MygosDatabase::class.java, name)
                .addMigrations(MygosDatabase.MIGRATION_2_3).build()
            val first = open()
            try {
                val old = first.activityDao().observeActivities().first().single()
                assertEquals("old", old.id)
                assertEquals(-33.51, requireNotNull(old.latitude), 0.00001)
                assertEquals(-70.76, requireNotNull(old.longitude), 0.00001)
                assertEquals("30/10/2026, 18:00", old.schedule)
                assertEquals("", old.description)
                assertEquals(1, first.contactDao().getAll().size)
                assertEquals(listOf(ActivityMember("old", "neighbor")), first.activityDao().observeMembers().first())
                first.activityDao().create(ActivityEntity("manual", "Actividad manual", "Maipú", "Entrada de la plaza",
                    "31-12-2099, 19:20", null, null, "owner", "Descripción guardada en Room."))
            } finally { first.close() }
            val reopened = open()
            try {
                val activities = reopened.activityDao().observeActivities().first()
                assertEquals(2, activities.size)
                val manual = activities.first { it.id == "manual" }
                assertNull(manual.latitude)
                assertNull(manual.longitude)
                assertEquals("Descripción guardada en Room.", manual.description)
                assertEquals(1, reopened.activityDao().observeMembers().first().size)
                assertEquals(1, reopened.contactDao().getAll().size)
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}
