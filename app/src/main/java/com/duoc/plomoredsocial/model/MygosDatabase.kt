// Base local compartida. Aquí se pueden incorporar las tablas de las otras tareas.
package com.duoc.plomoredsocial.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.duoc.plomoredsocial.model.ContactDao
import com.duoc.plomoredsocial.model.ContactEntity

@Database(entities = [ContactEntity::class, ActivityEntity::class, ActivityMember::class, UserEntity::class], version = 6, exportSchema = true)
abstract class MygosDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun activityDao(): ActivityDao
    abstract fun userDao(): UserDao

    companion object {
        // Conserva IDs y credenciales; no concede permisos por coincidencia de correo.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'")
                db.execSQL("UPDATE users SET role = 'ADMIN' WHERE id = 'demo-001' AND isDemo = 1")
            }
        }
        // Solo agrega capacidad: conserva todas las tablas y sus datos históricos.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activities ADD COLUMN capacity INTEGER NOT NULL DEFAULT 10")
            }
        }
        // Solo añade usuarios: conserva intactas actividades, inscritos y contactos.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS users (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL, normalizedEmail TEXT NOT NULL, passwordHash TEXT NOT NULL, salt TEXT NOT NULL, algorithm TEXT NOT NULL, iterations INTEGER NOT NULL, keyLength INTEGER NOT NULL, formatVersion INTEGER NOT NULL, isDemo INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_normalizedEmail ON users(normalizedEmail)")
            }
        }

        // Conserva IDs y coordenadas reales; la descripción anterior queda vacía.
        // Reconstruimos solo activities para permitir coordenadas ausentes, siempre como pareja.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE activities_v3 (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, commune TEXT NOT NULL, meetingPoint TEXT NOT NULL, schedule TEXT NOT NULL, latitude REAL, longitude REAL, organizerId TEXT NOT NULL, description TEXT NOT NULL DEFAULT '')")
                db.execSQL("INSERT INTO activities_v3 (id,title,commune,meetingPoint,schedule,latitude,longitude,organizerId,description) SELECT id,title,commune,meetingPoint,schedule,latitude,longitude,organizerId,'' FROM activities ORDER BY rowid")
                db.execSQL("DROP TABLE activities")
                db.execSQL("ALTER TABLE activities_v3 RENAME TO activities")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS activities (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, commune TEXT NOT NULL, meetingPoint TEXT NOT NULL, schedule TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, organizerId TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS activity_members (activityId TEXT NOT NULL, userId TEXT NOT NULL, PRIMARY KEY(activityId, userId))")
            }
        }
        @Volatile
        private var instance: MygosDatabase? = null

        // Usamos applicationContext para no retener una actividad.
        // Al cambiar tablas, subir la versión y agregar una migración; no borrar los datos.
        fun getInstance(context: Context): MygosDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MygosDatabase::class.java,
                    "mygos.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
            }
    }
}
