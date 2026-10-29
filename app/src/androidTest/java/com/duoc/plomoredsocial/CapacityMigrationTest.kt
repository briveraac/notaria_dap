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

class CapacityMigrationTest {
    @Test fun version4PreservesAllTablesAndAssignsTenPlaces() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "capacity-migration-${UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,alias TEXT NOT NULL,email TEXT NOT NULL,reason TEXT NOT NULL,message TEXT NOT NULL,createdAt INTEGER NOT NULL)")
                old.execSQL("INSERT INTO contacts VALUES(1,'Vecino','v@example.com','QUERY','Consulta',42)")
                old.execSQL("CREATE TABLE activities (id TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,commune TEXT NOT NULL,meetingPoint TEXT NOT NULL,schedule TEXT NOT NULL,latitude REAL,longitude REAL,organizerId TEXT NOT NULL,description TEXT NOT NULL DEFAULT '')")
                old.execSQL("INSERT INTO activities VALUES('located','Anterior','Maipú','Plaza','31-12-2099, 18:00',-33.51,-70.76,'historical-owner','Descripción anterior')")
                old.execSQL("INSERT INTO activities VALUES('manual','Sin mapa','Maipú','Entrada','31-12-2099, 19:00',NULL,NULL,'historical-owner','Referencia manual')")
                old.execSQL("CREATE TABLE activity_members (activityId TEXT NOT NULL,userId TEXT NOT NULL,PRIMARY KEY(activityId,userId))")
                old.execSQL("INSERT INTO activity_members VALUES('located','historical-owner')")
                old.execSQL("INSERT INTO activity_members VALUES('located','guest')")
                old.execSQL("CREATE TABLE users (id TEXT NOT NULL PRIMARY KEY,name TEXT NOT NULL,email TEXT NOT NULL,normalizedEmail TEXT NOT NULL,passwordHash TEXT NOT NULL,salt TEXT NOT NULL,algorithm TEXT NOT NULL,iterations INTEGER NOT NULL,keyLength INTEGER NOT NULL,formatVersion INTEGER NOT NULL,isDemo INTEGER NOT NULL)")
                old.execSQL("CREATE UNIQUE INDEX index_users_normalizedEmail ON users(normalizedEmail)")
                old.execSQL("INSERT INTO users VALUES('registered','Registrada','test@example.com','test@example.com','test-hash','test-salt','test-algorithm',1234,256,1,0)")
                old.version = 4
            }
            repeat(2) {
                val db = Room.databaseBuilder(context, MygosDatabase::class.java, name)
                    .addMigrations(MygosDatabase.MIGRATION_4_5).build()
                try {
                    val activities = db.activityDao().observeActivities().first()
                    assertEquals(2, activities.size)
                    assertTrue(activities.all { it.capacity == 10 })
                    val located = activities.single { it.id == "located" }
                    assertEquals("Descripción anterior", located.description)
                    assertEquals("historical-owner", located.organizerId)
                    assertEquals(-33.51, requireNotNull(located.latitude), 0.00001)
                    assertEquals(-70.76, requireNotNull(located.longitude), 0.00001)
                    assertEquals(2, located.participantCount(db.activityDao().observeMembers().first()))
                    val manual = activities.single { it.id == "manual" }
                    assertNull(manual.latitude); assertNull(manual.longitude)
                    assertEquals("Referencia manual", manual.description)
                    assertEquals(42L, db.contactDao().getById(1)?.createdAt)
                    val user = requireNotNull(db.userDao().findById("registered"))
                    assertEquals("test-hash", user.passwordHash)
                    assertEquals("test-salt", user.salt)
                    assertEquals(1234, user.iterations)
                    assertEquals("test@example.com", user.normalizedEmail)
                    assertFalse(user.isDemo)
                    assertEquals(5, db.openHelper.readableDatabase.version)
                } finally { db.close() }
            }
        } finally { context.deleteDatabase(name) }
    }
}
