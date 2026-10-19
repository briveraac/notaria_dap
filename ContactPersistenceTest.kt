// Pruebas con datos ficticios en una base en memoria, sin tocar las consultas de la app.
package com.duoc.plomoredsocial

import androidx.room.Room
import com.duoc.plomoredsocial.data.local.contact.ContactDao
import com.duoc.plomoredsocial.data.local.contact.ContactEntity
import com.duoc.plomoredsocial.ui.contact.ContactSaveStatus
import com.duoc.plomoredsocial.ui.contact.ContactViewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.data.contact.ContactRepository
import com.duoc.plomoredsocial.data.local.MygosDatabase
import com.duoc.plomoredsocial.domain.contact.ContactReason
import com.duoc.plomoredsocial.domain.contact.ContactRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ContactPersistenceTest {
    @Test
    fun failedInsertKeepsTheFormAndDoesNotReportSuccess() {
        val failingDao = object : ContactDao {
            override suspend fun insert(contact: ContactEntity): Long {
                throw IllegalStateException("Fallo de guardado simulado")
            }
            override suspend fun getAll(): List<ContactEntity> = emptyList()
            override suspend fun getById(id: Long): ContactEntity? = null
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val viewModel = ContactViewModel(ContactRepository(failingDao))
            viewModel.updateName("AliasPrueba")
            viewModel.updateEmail("prueba@example.com")
            viewModel.updateReason(ContactReason.QUERY)
            viewModel.updateMessage("Mensaje ficticio para probar un error.")
            viewModel.continueToReview()
            viewModel.confirmReview()
            assertEquals(ContactSaveStatus.ERROR, viewModel.uiState.saveStatus)
            assertNull(viewModel.uiState.savedContactId)
            assertEquals("AliasPrueba", viewModel.uiState.nameOrAlias)
            assertEquals("prueba@example.com", viewModel.uiState.email)
            assertEquals(ContactReason.QUERY, viewModel.uiState.reason)
            assertEquals("Mensaje ficticio para probar un error.", viewModel.uiState.message)
            viewModel.continueToReview()
            assertTrue(viewModel.uiState.reviewVisible)
        }
    }

    @Test
    fun validContactCanBeInsertedAndQueried() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        try {
            val repository = ContactRepository(database.contactDao())
            val request = ContactRequest(
                " AliasPrueba ", " prueba@example.com ", ContactReason.QUERY,
                " Consulta ficticia para MAIN-003. "
            )
            val id = repository.save(request)
            val stored = requireNotNull(repository.getById(id))
            assertTrue(id > 0)
            assertEquals("AliasPrueba", stored.alias)
            assertEquals("prueba@example.com", stored.email)
            assertEquals("QUERY", stored.reason)
            assertEquals("Consulta ficticia para MAIN-003.", stored.message)
            assertTrue(stored.createdAt > 0)
            assertEquals(1, repository.getAll().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun invalidContactDoesNotReachSQLite() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        try {
            val repository = ContactRepository(database.contactDao())
            val invalidRequests = listOf(
                ContactRequest("", "prueba@example.com", ContactReason.QUERY, "Mensaje ficticio"),
                ContactRequest("AliasPrueba", "correo-invalido", ContactReason.QUERY, "Mensaje ficticio"),
                ContactRequest("AliasPrueba", "prueba@example.com", null, "Mensaje ficticio"),
                ContactRequest("AliasPrueba", "prueba@example.com", ContactReason.QUERY, "corto")
            )
            invalidRequests.forEach { request ->
                try {
                    repository.save(request)
                    fail("No se deben guardar datos inválidos")
                } catch (expected: IllegalArgumentException) {
                    assertTrue(repository.getAll().isEmpty())
                }
            }
        } finally {
            database.close()
        }
    }
}
