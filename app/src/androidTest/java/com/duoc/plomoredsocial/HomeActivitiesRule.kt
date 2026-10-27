package com.duoc.plomoredsocial

import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.MygosDatabase
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import java.util.UUID

// Solo las pruebas agregan estas actividades. Cada prueba borra únicamente sus propios datos.
class HomeActivitiesRule : ExternalResource() {
    private val prefix = "main004-test-${UUID.randomUUID()}-"
    private val names = listOf("park", "volley", "books", "games", "code", "garden", "photo", "stretch")
    private val database get() = MygosDatabase.getInstance(
        InstrumentationRegistry.getInstrumentation().targetContext)

    fun id(name: String) = prefix + name

    override fun before() = runBlocking {
        // Room muestra primero lo más reciente; se inserta al revés para probar todo el recorrido.
        names.reversed().forEach { name ->
            database.activityDao().create(ActivityEntity(id(name), "Prueba MAIN-004 $name",
                "Maipú", "Entrada de la plaza", "30/10/2026, 18:00", -33.51, -70.76, "test-organizer"))
        }
    }

    override fun after() {
        // No borra actividades de otras personas ni toca sus inscripciones.
        val connection = database.openHelper.writableDatabase
        names.forEach { name ->
            connection.execSQL("DELETE FROM activity_members WHERE activityId = ?", arrayOf(id(name)))
            connection.execSQL("DELETE FROM activities WHERE id = ?", arrayOf(id(name)))
        }
    }
}
