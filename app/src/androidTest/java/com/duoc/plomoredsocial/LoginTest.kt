package com.duoc.plomoredsocial

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.model.LoginError
import com.duoc.plomoredsocial.repository.AuthNetworkException
import com.duoc.plomoredsocial.repository.AuthSession
import com.duoc.plomoredsocial.repository.InvalidCredentialsException
import com.duoc.plomoredsocial.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test

/** Exercises the real ViewModel with a fake server; no demo account or live Xano call. */
class LoginTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val remoteUser = AuthUser("42", "Vecina remota", "vecina@example.com")

    @Test fun normalizedLoginStoresTokenOnceAndLogoutClearsIt() {
        val repository = FakeRemoteAuthRepository()
        val store = MemoryTokenStore()
        val owner = ViewModelStore()
        var attempts = 0
        var submittedEmail: String? = null
        var submittedPassword: String? = null
        repository.login = { email, password ->
            attempts++
            submittedEmail = email
            submittedPassword = password
            delay(100)
            AuthSession(remoteUser, "token-fixture")
        }
        try {
            val vm = onMain { ViewModelProvider(owner, LoginViewModel.Factory(repository, store))[LoginViewModel::class.java] }
            awaitAuthState { !vm.uiState.restoringSession }
            onMain {
                vm.updateEmail(" VECINA@EXAMPLE.COM ")
                vm.updatePassword(" frase con espacios ")
                vm.login()
                vm.login()
            }
            awaitAuthState { !vm.uiState.loading }
            assertEquals(1, attempts)
            assertEquals("vecina@example.com", submittedEmail)
            assertEquals(" frase con espacios ", submittedPassword)
            assertEquals("token-fixture", store.token)
            assertNull(store.userId)
            assertEquals(remoteUser, vm.uiState.user)
            assertEquals("", vm.uiState.password)
            onMain { vm.logout() }
            awaitAuthState { vm.uiState.user == null && !vm.uiState.loading }
            assertNull(store.token)
            assertEquals(1, store.clearCount)
        } finally { onMain { owner.clear() } }
    }

    @Test fun invalidCredentialsAndNetworkFailureNeverSaveASession() {
        val repository = FakeRemoteAuthRepository()
        val store = MemoryTokenStore()
        val owner = ViewModelStore()
        try {
            val vm = onMain { ViewModelProvider(owner, LoginViewModel.Factory(repository, store))[LoginViewModel::class.java] }
            awaitAuthState { !vm.uiState.restoringSession }
            repository.login = { _, _ -> throw InvalidCredentialsException() }
            onMain {
                vm.updateEmail("vecina@example.com")
                vm.updatePassword("incorrecta")
                vm.login()
            }
            awaitAuthState { !vm.uiState.loading && vm.uiState.error != null }
            assertEquals(LoginError.INVALID_CREDENTIALS, vm.uiState.error)
            assertNull(store.token)
            repository.login = { _, _ -> throw AuthNetworkException() }
            onMain { vm.updatePassword("otra contraseña"); vm.login() }
            awaitAuthState { !vm.uiState.loading && vm.uiState.error != null }
            assertEquals(LoginError.UNAVAILABLE, vm.uiState.error)
            assertNull(store.token)
        } finally { onMain { owner.clear() } }
    }

    @Test fun tokenRestoreUsesRemoteProfileAndInvalidTokenClearsLocalSession() {
        val repository = FakeRemoteAuthRepository()
        val store = MemoryTokenStore(token = "restored-token")
        val owner = ViewModelStore()
        var lookedUpToken: String? = null
        repository.lookup = { token -> lookedUpToken = token; remoteUser }
        try {
            val vm = onMain { ViewModelProvider(owner, LoginViewModel.Factory(repository, store))[LoginViewModel::class.java] }
            awaitAuthState { !vm.uiState.restoringSession }
            assertEquals("restored-token", lookedUpToken)
            assertEquals(remoteUser, vm.uiState.user)
        } finally { onMain { owner.clear() } }

        val invalidStore = MemoryTokenStore(token = "expired-token")
        val invalidOwner = ViewModelStore()
        repository.lookup = { null }
        try {
            val vm = onMain { ViewModelProvider(invalidOwner, LoginViewModel.Factory(repository, invalidStore))[LoginViewModel::class.java] }
            awaitAuthState { !vm.uiState.restoringSession }
            assertNull(vm.uiState.user)
            assertNull(invalidStore.token)
            assertEquals(1, invalidStore.clearCount)
        } finally { onMain { invalidOwner.clear() } }
    }

    @Test fun tokenWriteFailureNeverReportsSuccessfulLogin() {
        val repository = FakeRemoteAuthRepository()
        val store = MemoryTokenStore().apply { failSave = true }
        val owner = ViewModelStore()
        repository.login = { _, _ -> AuthSession(remoteUser, "test-token") }
        try {
            val vm = onMain { ViewModelProvider(owner, LoginViewModel.Factory(repository, store))[LoginViewModel::class.java] }
            awaitAuthState { !vm.uiState.restoringSession }
            onMain {
                vm.updateEmail(remoteUser.email)
                vm.updatePassword("test-only password")
                vm.login()
            }
            awaitAuthState { !vm.uiState.loading && vm.uiState.error != null }
            assertNull(vm.uiState.user)
            assertEquals(LoginError.UNAVAILABLE, vm.uiState.error)
            assertEquals("", vm.uiState.password)
            assertNull(store.token)
        } finally { onMain { owner.clear() } }
    }

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
