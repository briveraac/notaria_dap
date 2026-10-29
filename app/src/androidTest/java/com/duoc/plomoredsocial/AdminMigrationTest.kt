package com.duoc.plomoredsocial

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AdminMigrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun prepareVersion5(name: String, demoOrigin: Boolean): PasswordRecord {
        val credentials = PasswordDeriver().create("Contraseña de migración larga")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,alias TEXT NOT NULL,email TEXT NOT NULL,reason TEXT NOT NULL,message TEXT NOT NULL,createdAt INTEGER NOT NULL)")
            old.execSQL("INSERT INTO contacts VALUES(1,'Vecina','contact@example.com','QUERY','Consulta',42)")
            old.execSQL("CREATE TABLE activities (id TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,commune TEXT NOT NULL,meetingPoint TEXT NOT NULL,schedule TEXT NOT NULL,latitude REAL,longitude REAL,organizerId TEXT NOT NULL,description TEXT NOT NULL DEFAULT '',capacity INTEGER NOT NULL DEFAULT 10)")
            old.execSQL("INSERT INTO activities VALUES('located','Con mapa','Maipú','Plaza','31-12-2099, 18:00',-33.51,-70.76,'demo-001','Descripción',5)")
            old.execSQL("INSERT INTO activities VALUES('manual','Sin mapa','Maipú','Entrada','31-12-2099, 19:00',NULL,NULL,'historical','Referencia',10)")
            old.execSQL("CREATE TABLE activity_members (activityId TEXT NOT NULL,userId TEXT NOT NULL,PRIMARY KEY(activityId,userId))")
            old.execSQL("INSERT INTO activity_members VALUES('located','normal')")
            old.execSQL("CREATE TABLE users (id TEXT NOT NULL PRIMARY KEY,name TEXT NOT NULL,email TEXT NOT NULL,normalizedEmail TEXT NOT NULL,passwordHash TEXT NOT NULL,salt TEXT NOT NULL,algorithm TEXT NOT NULL,iterations INTEGER NOT NULL,keyLength INTEGER NOT NULL,formatVersion INTEGER NOT NULL,isDemo INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_users_normalizedEmail ON users(normalizedEmail)")
            fun insert(id: String, email: String, isDemo: Boolean) = old.execSQL(
                "INSERT INTO users VALUES(?,?,?,?,?,?,?,?,?,?,?)", arrayOf<Any>(id, "Nombre editado", email, email,
                    credentials.hash, credentials.salt, credentials.algorithm, credentials.iterations,
                    credentials.keyLength, credentials.formatVersion, if (isDemo) 1 else 0))
            insert(PRIMARY_DEMO_ID, "changed@example.com", demoOrigin)
            insert("normal", "demo@mygos.cl", false)
            old.version = 5
        }
        return credentials
    }

    @Test fun version5PreservesEveryTableAndGrantsAdminByStableIdAndOriginOnly() = runBlocking {
        val name = "admin-migration-${UUID.randomUUID()}.db"
        val record = prepareVersion5(name, true)
        try {
            repeat(2) {
                val db = Room.databaseBuilder(context, MygosDatabase::class.java, name)
                    .addMigrations(MygosDatabase.MIGRATION_5_6).build()
                try {
                    val principal = requireNotNull(db.userDao().findById(PRIMARY_DEMO_ID))
                    assertEquals("ADMIN", principal.role)
                    assertEquals("Nombre editado", principal.name)
                    assertEquals("changed@example.com", principal.email)
                    assertTrue(principal.passwordHash == record.hash && principal.salt == record.salt)
                    assertEquals("USER", db.userDao().findById("normal")?.role)
                    val auth = RoomAuthRepository(db, context.assets)
                    assertTrue(requireNotNull(auth.getUserById(PRIMARY_DEMO_ID)).isAdmin)
                    assertEquals("changed@example.com", auth.getUserById(PRIMARY_DEMO_ID)?.email)
                    assertEquals(PRIMARY_DEMO_ID, auth.authenticate("changed@example.com", "Contraseña de migración larga")?.id)
                    assertFalse(requireNotNull(auth.getUserById("normal")).isAdmin)
                    val events = db.activityDao().observeActivities().first()
                    assertEquals(2, events.size)
                    val located = events.single { it.id == "located" }
                    assertEquals(5, located.capacity)
                    assertEquals(-33.51, requireNotNull(located.latitude), 0.00001)
                    assertEquals(-70.76, requireNotNull(located.longitude), 0.00001)
                    assertEquals("Descripción", located.description)
                    assertNull(events.single { it.id == "manual" }.latitude)
                    assertEquals(listOf(ActivityMember("located", "normal")), db.activityDao().observeMembers().first())
                    assertEquals(42L, db.contactDao().getById(1)?.createdAt)
                    assertEquals(6, db.openHelper.readableDatabase.version)
                } finally { db.close() }
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun matchingIdWithoutDemoOriginNeverReceivesAdmin() = runBlocking {
        val name = "admin-origin-${UUID.randomUUID()}.db"
        prepareVersion5(name, false)
        try {
            val db = Room.databaseBuilder(context, MygosDatabase::class.java, name)
                .addMigrations(MygosDatabase.MIGRATION_5_6).build()
            try {
                assertEquals("USER", db.userDao().findById(PRIMARY_DEMO_ID)?.role)
                val admin = AdminRepository(db, AdminTestSession(PRIMARY_DEMO_ID))
                try { admin.observePanel().first(); fail("Origen incorrecto autorizado") }
                catch (_: SecurityException) { }
                try { RoomAuthRepository(db, context.assets).getUserById(PRIMARY_DEMO_ID); fail("Se aceptó el conflicto de origen") }
                catch (_: IllegalStateException) { }
                assertEquals("Nombre editado", db.userDao().findById(PRIMARY_DEMO_ID)?.name)
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
