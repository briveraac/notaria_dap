package com.duoc.plomoredsocial

import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.repository.AuthRepository
import com.duoc.plomoredsocial.repository.AuthSession
import com.duoc.plomoredsocial.repository.SessionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.IOException

/** In-memory fixtures: instrumented auth tests never create accounts in Xano. */
internal class FakeRemoteAuthRepository : AuthRepository {
    var login: suspend (String, String) -> AuthSession? = { _, _ -> null }
    var signup: suspend (String, String, String) -> AuthSession = { _, _, _ ->
        error("Unexpected signup call")
    }
    var lookup: suspend (String) -> AuthUser? = { null }

    override suspend fun authenticate(email: String, password: String): AuthUser? =
        login(email, password)?.user

    override suspend fun getUserById(id: String): AuthUser? = null

    override suspend fun authenticateSession(email: String, password: String): AuthSession? =
        login(email, password)

    override suspend fun registerSession(name: String, email: String, password: String): AuthSession =
        signup(name, email, password)

    override suspend fun getUserByToken(token: String): AuthUser? = lookup(token)
}

internal class MemoryTokenStore(
    var token: String? = null,
    var userId: String? = null,
) : SessionStore {
    var failSave = false
    var failClear = false
    var clearCount = 0

    override suspend fun readUserId(): String? = userId
    override suspend fun saveUserId(userId: String) { this.userId = userId }
    override suspend fun readToken(): String? = token
    override suspend fun saveToken(token: String) {
        if (failSave) throw IOException("Simulated token storage failure")
        this.token = token
        userId = null
    }
    override suspend fun clear() {
        if (failClear) throw IOException("Simulated clear failure")
        clearCount++
        token = null
        userId = null
    }
}

internal fun awaitAuthState(timeoutMillis: Long = 10_000, predicate: () -> Boolean) = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    withTimeout(timeoutMillis) {
        while (true) {
            var ready = false
            instrumentation.runOnMainSync { ready = predicate() }
            if (ready) break
            delay(20)
        }
    }
}
