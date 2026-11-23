package com.duoc.plomoredsocial

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.repository.AuthRepository
import com.duoc.plomoredsocial.repository.AuthSession
import com.duoc.plomoredsocial.repository.AuthNetworkException
import com.duoc.plomoredsocial.repository.DuplicateEmailException
import com.duoc.plomoredsocial.viewmodel.RegisterViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class RegisterViewModelTest {
    @Test fun remoteSignupNormalizesEmailStoresTokenAndPublishesUserOnce() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val owner = ViewModelStore()
        val repository = FakeRemoteAuthRepository()
        val store = MemoryTokenStore()
        val user = AuthUser("57", "Vecina", "vecina@example.com")
        var calls = 0
        var sentName = ""
        var sentEmail = ""
        var sentPassword = ""
        repository.signup = { name, email, password ->
            calls++
            sentName = name; sentEmail = email; sentPassword = password
            delay(100)
            AuthSession(user, "signup-token")
        }
        try {
            lateinit var vm: RegisterViewModel
            instrumentation.runOnMainSync {
                vm = ViewModelProvider(owner, RegisterViewModel.Factory(repository, store))[RegisterViewModel::class.java]
                vm.updateName(" Vecina ")
                vm.updateEmail(" VECINA@EXAMPLE.COM ")
                vm.updatePassword("frase larga segura")
                vm.updateConfirmation("frase larga segura")
                vm.register()
                vm.register()
            }
            awaitAuthState { !vm.loading }
            assertEquals(1, calls)
            assertEquals("Vecina", sentName)
            assertEquals("vecina@example.com", sentEmail)
            assertEquals("frase larga segura", sentPassword)
            assertEquals("signup-token", store.token)
            assertNull(store.userId)
            assertEquals("vecina@example.com", vm.registeredEmail)
            assertEquals(user, vm.consumeRegisteredUser())
            assertNull(vm.consumeRegisteredUser())
            assertEquals("", vm.password)
            assertEquals("", vm.confirmation)
        } finally { instrumentation.runOnMainSync { owner.clear() } }
    }

    @Test fun duplicateAndNetworkFailuresDoNotClaimSuccessfulRegistration() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val owner = ViewModelStore()
        val repository = FakeRemoteAuthRepository()
        val store = MemoryTokenStore()
        try {
            lateinit var vm: RegisterViewModel
            instrumentation.runOnMainSync {
                vm = ViewModelProvider(owner, RegisterViewModel.Factory(repository, store))[RegisterViewModel::class.java]
                vm.updateName("Vecina")
                vm.updateEmail("vecina@example.com")
                vm.updatePassword("frase larga segura")
                vm.updateConfirmation("frase larga segura")
            }
            repository.signup = { _, _, _ -> throw DuplicateEmailException() }
            instrumentation.runOnMainSync { vm.register() }
            awaitAuthState { !vm.loading && vm.duplicateEmail }
            assertNull(vm.registeredEmail)
            assertNull(store.token)
            assertEquals("Este correo ya tiene una cuenta.", vm.emailError)

            repository.signup = { _, _, _ -> throw AuthNetworkException() }
            instrumentation.runOnMainSync { vm.updateEmail("otra@example.com"); vm.register() }
            awaitAuthState { !vm.loading && vm.error != null }
            assertNull(vm.registeredEmail)
            assertNull(store.token)
            assertTrue(vm.error!!.contains("conectar con Xano"))
        } finally { instrumentation.runOnMainSync { owner.clear() } }
    }

    @Test fun accountCreatedButTokenStorageFailsDoesNotOpenASession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val owner = ViewModelStore()
        val repository = FakeRemoteAuthRepository()
        val store = MemoryTokenStore().apply { failSave = true }
        val user = AuthUser("58", "Vecina", "vecina@example.com")
        repository.signup = { _, _, _ -> AuthSession(user, "signup-token") }
        try {
            lateinit var vm: RegisterViewModel
            instrumentation.runOnMainSync {
                vm = ViewModelProvider(owner, RegisterViewModel.Factory(repository, store))[RegisterViewModel::class.java]
                vm.updateName("Vecina")
                vm.updateEmail(user.email)
                vm.updatePassword("frase larga segura")
                vm.updateConfirmation("frase larga segura")
                vm.register()
            }
            awaitAuthState { !vm.loading }
            assertEquals(user.email, vm.registeredEmail)
            assertNull(vm.consumeRegisteredUser())
            assertNull(store.token)
            assertTrue(vm.error!!.contains("no pudimos guardar la sesión"))
        } finally { instrumentation.runOnMainSync { owner.clear() } }
    }

    @Test fun storageFailureDoesNotReportSuccessAndRepeatedSubmitDoesNotDuplicateWork() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val owner = ViewModelStore()
        var fail = true
        var attempts = 0
        val repository = object : AuthRepository {
            override suspend fun authenticate(email: String, password: String): AuthUser? = null
            override suspend fun getUserById(id: String): AuthUser? = null
            override suspend fun register(name: String, email: String, password: String): AuthUser {
                attempts++
                delay(100)
                if (fail) throw IOException("Fallo de almacenamiento simulado.")
                return AuthUser("local-test", name, email)
            }
        }
        lateinit var vm: RegisterViewModel
        fun await(condition: () -> Boolean) = runBlocking {
            withTimeout(10000) {
                var ready = false
                while (!ready) {
                    instrumentation.runOnMainSync { ready = condition() }
                    if (!ready) delay(20)
                }
            }
        }
        try {
            instrumentation.runOnMainSync {
                vm = ViewModelProvider(owner, RegisterViewModel.Factory(repository))[RegisterViewModel::class.java]
                vm.updateName("Vecina")
                vm.updateEmail("vecina@example.com")
                vm.updatePassword("Una contraseña larga")
                vm.updateConfirmation("Una contraseña larga")
                vm.register()
                vm.register()
            }
            await { !vm.loading }
            instrumentation.runOnMainSync {
                assertEquals(1, attempts)
                assertNull(vm.registeredEmail)
                assertNotNull(vm.error)
                fail = false
                vm.register()
            }
            await { !vm.loading }
            instrumentation.runOnMainSync {
                assertEquals(2, attempts)
                assertEquals("vecina@example.com", vm.registeredEmail)
                assertEquals("", vm.password)
                assertEquals("", vm.confirmation)
                assertNull(vm.error)
            }
        } finally { instrumentation.runOnMainSync { owner.clear() } }
    }
}
