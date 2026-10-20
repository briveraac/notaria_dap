// Base local compartida. Aquí se pueden incorporar las tablas de las otras tareas.
package com.duoc.plomoredsocial.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duoc.plomoredsocial.model.ContactDao
import com.duoc.plomoredsocial.model.ContactEntity

@Database(entities = [ContactEntity::class], version = 1, exportSchema = true)
abstract class MygosDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
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
                ).build().also { instance = it }
            }
    }
}
