package com.duoc.plomoredsocial

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.repository.RoomAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class UserMigrationTest {
    @Test fun version3AddsUsersWithoutChangingExistingDataOrDemoIds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration34-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,alias TEXT NOT NULL,email TEXT NOT NULL,reason TEXT NOT NULL,message TEXT NOT NULL,createdAt INTEGER NOT NULL)")
                old.execSQL("INSERT INTO contacts VALUES(1,'Vecino','v@example.com','QUERY','Consulta',1)")
                old.execSQL("CREATE TABLE activities (id TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,commune TEXT NOT NULL,meetingPoint TEXT NOT NULL,schedule TEXT NOT NULL,latitude REAL,longitude REAL,organizerId TEXT NOT NULL,description TEXT NOT NULL DEFAULT '')")
                old.execSQL("INSERT INTO activities VALUES('event','Anterior','Maipú','Plaza','31-12-2099, 18:00',NULL,NULL,'demo-001','Descripción anterior')")
                old.execSQL("CREATE TABLE activity_members (activityId TEXT NOT NULL,userId TEXT NOT NULL,PRIMARY KEY(activityId,userId))")
                old.execSQL("INSERT INTO activity_members VALUES('event','demo-001')")
                old.version = 3
            }
            val database = Room.databaseBuilder(context, MygosDatabase::class.java, name)
                .addMigrations(MygosDatabase.MIGRATION_3_4).build()
            try {
                assertEquals("Vecino", database.contactDao().getById(1)?.alias)
                val activity = database.activityDao().observeActivities().first().single()
                assertEquals("demo-001", activity.organizerId)
                assertEquals("Descripción anterior", activity.description)
                assertNull(activity.latitude)
                assertEquals(listOf(ActivityMember("event", "demo-001")), database.activityDao().observeMembers().first())
                assertEquals("demo-001", RoomAuthRepository(database, context.assets).getUserById("demo-001")?.id)
                assertEquals("event", database.activityDao().observeActivities().first().single().id)
            } finally { database.close() }
        } finally { context.deleteDatabase(name) }
    }
}
