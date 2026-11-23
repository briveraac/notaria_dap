package com.duoc.plomoredsocial

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.repository.AuthSession
import com.duoc.plomoredsocial.repository.XanoSessionStore
import com.duoc.plomoredsocial.viewmodel.LoginViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Uses a throwaway private directory and fake server; never reads the app's real token. */
class XanoSessionStoreTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun encryptedTokenRestoresRemoteProfileAcrossViewModelsAndLogoutRemovesIt() {
        val directory = File(context.cacheDir, "xano-session-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        val user = AuthUser("501", "Vecina remota", "vecina@example.com")
        val repository = FakeRemoteAuthRepository()
        repository.login = { _, _ -> AuthSession(user, "fixture-token-501") }
        repository.lookup = { token -> if (token == "fixture-token-501") user else null }
        val firstOwner = ViewModelStore()
        val secondOwner = ViewModelStore()
        try {
            val first = onMain {
                ViewModelProvider(firstOwner, LoginViewModel.Factory(repository, XanoSessionStore(directory)))[LoginViewModel::class.java]
            }
            awaitAuthState { !first.uiState.restoringSession }
            onMain {
                first.updateEmail(user.email)
                first.updatePassword("test-only password")
                first.login()
            }
            awaitAuthState { !first.uiState.loading && first.uiState.user != null }
            assertEquals(user, first.uiState.user)
            val sessionFile = File(directory, "xano_session_token")
            assertTrue(sessionFile.exists())
            assertFalse(sessionFile.readBytes().toString(Charsets.ISO_8859_1).contains("fixture-token-501"))
            assertEquals("fixture-token-501", runBlocking { XanoSessionStore(directory).readToken() })
            assertNull(runBlocking { XanoSessionStore(directory).readUserId() })
            onMain { firstOwner.clear() }

            val restored = onMain {
                ViewModelProvider(secondOwner, LoginViewModel.Factory(repository, XanoSessionStore(directory)))[LoginViewModel::class.java]
            }
            awaitAuthState { !restored.uiState.restoringSession }
            assertEquals(user, restored.uiState.user)
            onMain { restored.logout() }
            awaitAuthState { !restored.uiState.loading && restored.uiState.user == null }
            assertNull(runBlocking { XanoSessionStore(directory).readToken() })
            assertFalse(sessionFile.exists())
        } finally {
            onMain { firstOwner.clear(); secondOwner.clear() }
            directory.deleteRecursively()
        }
    }

    @Test fun damagedCiphertextCannotRestoreASession() = runBlocking {
        val directory = File(context.cacheDir, "xano-damaged-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        try {
            val store = XanoSessionStore(directory)
            store.saveToken("fixture-token")
            val file = File(directory, "xano_session_token")
            file.writeBytes(byteArrayOf(1, 2, 3))
            assertNull(XanoSessionStore(directory).readToken())
            assertFalse(file.exists())
        } finally { directory.deleteRecursively() }
    }

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
