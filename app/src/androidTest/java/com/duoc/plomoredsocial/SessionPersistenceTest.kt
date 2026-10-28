package com.duoc.plomoredsocial

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.LoginError
import com.duoc.plomoredsocial.repository.LocalSessionStore
import com.duoc.plomoredsocial.repository.SessionStore
import com.duoc.plomoredsocial.repository.RoomAuthRepository
import com.duoc.plomoredsocial.model.MygosDatabase
import com.duoc.plomoredsocial.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID

class SessionPersistenceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun registeredLocalAccountRestoresInFreshViewModelsAndLogoutPersists() {
        val directory = File(context.cacheDir, "local-session-test-${UUID.randomUUID()}")
        directory.mkdirs()
        val database = MygosDatabase.getInstance(context)
        val email = "local-session-${UUID.randomUUID()}@example.com"
        val user = runBlocking {
            RoomAuthRepository(database, context.assets).register("Vecina local", email, "Una contraseña local larga")
        }
        val instances = mutableListOf<SessionInstance>()
        try {
            runBlocking { LocalSessionStore(directory).saveUserId(user.id) }
            val first = open(LocalSessionStore(directory)).also { instances.add(it) }
            assertEquals(user.id, first.viewModel.uiState.user?.id)
            assertEquals(email, first.viewModel.uiState.user?.email)
            assertEquals(false, first.viewModel.uiState.user?.isDemo)
            close(first)
            val fresh = open(LocalSessionStore(directory)).also { instances.add(it) }
            assertEquals(user.id, fresh.viewModel.uiState.user?.id)
            instrumentation.runOnMainSync { fresh.viewModel.logout() }
            await { !fresh.viewModel.uiState.loading }
            close(fresh)
            val loggedOut = open(LocalSessionStore(directory)).also { instances.add(it) }
            assertNull(loggedOut.viewModel.uiState.user)
            assertNull(runBlocking { LocalSessionStore(directory).readUserId() })
        } finally {
            instances.forEach { close(it) }
            directory.deleteRecursively()
            database.openHelper.writableDatabase.execSQL("DELETE FROM users WHERE id = ?", arrayOf(user.id))
        }
    }

    private class SessionInstance(val owner: ViewModelStore, val viewModel: LoginViewModel)

    private fun open(store: SessionStore): SessionInstance {
        lateinit var instance: SessionInstance
        instrumentation.runOnMainSync {
            val owner = ViewModelStore()
            val vm = ViewModelProvider(owner, LoginViewModel.Factory(
                RoomAuthRepository(MygosDatabase.getInstance(context), context.assets), store
            ))[LoginViewModel::class.java]
            instance = SessionInstance(owner, vm)
        }
        await { !instance.viewModel.uiState.restoringSession }
        return instance
    }

    private fun close(instance: SessionInstance) {
        instrumentation.runOnMainSync { instance.owner.clear() }
    }

    private fun await(condition: () -> Boolean) = runBlocking {
        withTimeout(10000) {
            var ready = false
            while (!ready) {
                instrumentation.runOnMainSync { ready = condition() }
                if (!ready) delay(10)
            }
        }
    }

    @Test fun loginSurvivesFreshInstancesAndLogoutStaysLoggedOut() {
        val directory = File(context.cacheDir, "session-test-${UUID.randomUUID()}")
        directory.mkdirs()
        val instances = mutableListOf<SessionInstance>()
        try {
            val first = open(LocalSessionStore(directory)).also { instances.add(it) }
            instrumentation.runOnMainSync {
                first.viewModel.updateEmail("demo@mygos.cl")
                first.viewModel.updatePassword("Mygos2026!")
                first.viewModel.login()
            }
            await { first.viewModel.uiState.user != null }
            assertEquals("demo-001", runBlocking { LocalSessionStore(directory).readUserId() })
            assertEquals("", first.viewModel.uiState.password)
            assertEquals("demo-001", File(directory, "session_user_id").readText())
            close(first)

            // Nuevo ViewModel y nueva fuente: se recupera del disco, no del estado anterior.
            val reopened = open(LocalSessionStore(directory)).also { instances.add(it) }
            assertEquals("Alex", reopened.viewModel.uiState.user?.name)
            assertEquals("demo@mygos.cl", reopened.viewModel.uiState.user?.email)
            instrumentation.runOnMainSync { reopened.viewModel.logout() }
            await { !reopened.viewModel.uiState.loading }
            assertNull(reopened.viewModel.uiState.user)
            close(reopened)

            val loggedOut = open(LocalSessionStore(directory)).also { instances.add(it) }
            assertNull(loggedOut.viewModel.uiState.user)
            assertNull(runBlocking { LocalSessionStore(directory).readUserId() })
        } finally {
            instances.forEach { close(it) }
            directory.deleteRecursively()
        }
    }

    @Test fun removedAccountDoesNotRestoreAndItsSessionIsCleared() {
        val directory = File(context.cacheDir, "session-test-${UUID.randomUUID()}")
        directory.mkdirs()
        try {
            runBlocking { LocalSessionStore(directory).saveUserId("removed-account") }
            val instance = open(LocalSessionStore(directory))
            try {
                assertNull(instance.viewModel.uiState.user)
                assertNull(runBlocking { LocalSessionStore(directory).readUserId() })
            } finally { close(instance) }
        } finally { directory.deleteRecursively() }
    }

    @Test fun storageFailureDoesNotReportSuccessfulLogin() {
        val failing = object : SessionStore {
            override suspend fun readUserId(): String? = null
            override suspend fun saveUserId(userId: String) { throw IOException("Disco no disponible") }
            override suspend fun clear() = Unit
        }
        val instance = open(failing)
        try {
            instrumentation.runOnMainSync {
                instance.viewModel.updateEmail("demo@mygos.cl")
                instance.viewModel.updatePassword("Mygos2026!")
                instance.viewModel.login()
            }
            await { !instance.viewModel.uiState.loading }
            assertNull(instance.viewModel.uiState.user)
            assertEquals(LoginError.UNAVAILABLE, instance.viewModel.uiState.error)
            assertEquals("", instance.viewModel.uiState.password)
        } finally { close(instance) }
    }

    @Test fun logoutFailureKeepsAccountVisibleAndAllowsRetry() {
        var fail = true
        var id: String? = "demo-001"
        val store = object : SessionStore {
            override suspend fun readUserId() = id
            override suspend fun saveUserId(userId: String) { id = userId }
            override suspend fun clear() {
                if (fail) throw IOException("Disco no disponible")
                id = null
            }
        }
        val instance = open(store)
        try {
            instrumentation.runOnMainSync { instance.viewModel.logout() }
            await { !instance.viewModel.uiState.loading }
            assertTrue(instance.viewModel.uiState.logoutFailed)
            assertNotNull(instance.viewModel.uiState.user)
            fail = false
            instrumentation.runOnMainSync { instance.viewModel.logout() }
            await { !instance.viewModel.uiState.loading }
            assertNull(instance.viewModel.uiState.user)
            assertNull(id)
        } finally { close(instance) }
    }
}
