package com.duoc.plomoredsocial

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.repository.AuthRepository
import com.duoc.plomoredsocial.viewmodel.RegisterViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class RegisterViewModelTest {
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
