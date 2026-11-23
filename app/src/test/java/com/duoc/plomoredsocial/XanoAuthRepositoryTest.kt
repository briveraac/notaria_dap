package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.data.remote.xano.XanoApiClient
import com.duoc.plomoredsocial.repository.AccountCreatedSessionUnavailableException
import com.duoc.plomoredsocial.repository.DuplicateEmailException
import com.duoc.plomoredsocial.repository.AuthNetworkException
import com.duoc.plomoredsocial.repository.AuthServerException
import com.duoc.plomoredsocial.repository.InvalidAuthResponseException
import com.duoc.plomoredsocial.repository.InvalidCredentialsException
import com.duoc.plomoredsocial.repository.XanoAuthRepository
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class XanoAuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: XanoAuthRepository

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        repository = XanoAuthRepository(XanoApiClient.create(
            server.url("/api:6kDB2GLk/").toString(), OkHttpClient()
        ))
    }

    @After fun tearDown() { server.close() }

    @Test fun loginUsesTokenThenLoadsRemoteProfile() = runBlocking {
        server.enqueue(json("""{"authToken":"fixture-token","user_id":42}"""))
        server.enqueue(json("""{"id":42,"Nombre":"Ana","correo":"ANA@example.com","administrador":false}"""))

        val session = repository.authenticateSession("  ANA@EXAMPLE.COM  ", "test password")

        assertEquals("fixture-token", session.token)
        assertEquals("42", session.user.id)
        assertEquals("Ana", session.user.name)
        assertEquals("ana@example.com", session.user.email)
        assertFalse(session.user.isAdmin)
        val login = server.takeRequest()
        assertEquals("POST", login.method)
        assertEquals("/api:6kDB2GLk/auth/login", login.url.encodedPath)
        assertTrue(login.body!!.utf8().contains("\"email\":\"ana@example.com\""))
        val me = server.takeRequest()
        assertEquals("GET", me.method)
        assertEquals("/api:6kDB2GLk/auth/me", me.url.encodedPath)
        assertEquals("Bearer fixture-token", me.headers["Authorization"])
    }

    @Test fun signupUsesOnlyUserInputsAndConfirmsWithMe() = runBlocking {
        server.enqueue(json("""{"authToken":"new-token","user_id":"57"}"""))
        server.enqueue(json("""{"id":57,"Nombre":"Vecina","correo":"vecina@example.com"}"""))

        val session = repository.registerSession(" Vecina ", " VECINA@example.com ", "long test password")

        assertEquals("57", session.user.id)
        assertEquals("new-token", session.token)
        val signup = server.takeRequest()
        assertEquals("/api:6kDB2GLk/auth/signup", signup.url.encodedPath)
        val body = signup.body!!.utf8()
        assertTrue(body.contains("\"name\":\"Vecina\""))
        assertTrue(body.contains("\"email\":\"vecina@example.com\""))
        assertTrue(body.contains("\"password\":\"long test password\""))
        assertFalse(body.contains("administrador"))
        assertEquals("Bearer new-token", server.takeRequest().headers["Authorization"])
    }

    @Test fun duplicateAndInvalidCredentialsAreDistinct() = runBlocking {
        server.enqueue(MockResponse.Builder().code(400)
            .body("""{"message":"Email already registered"}""").build())
        assertThrows(DuplicateEmailException::class.java) {
            runBlocking { repository.registerSession("Vecina", "v@example.com", "password") }
        }
        server.enqueue(MockResponse.Builder().code(401).body("{}") .build())
        assertThrows(InvalidCredentialsException::class.java) {
            runBlocking { repository.authenticateSession("v@example.com", "wrong") }
        }
        Unit
    }

    @Test fun expiredTokenCannotRestoreAccount() = runBlocking {
        server.enqueue(MockResponse.Builder().code(401).body("{}") .build())
        assertEquals(null, repository.getUserByToken("expired"))
        assertEquals("Bearer expired", server.takeRequest().headers["Authorization"])
    }

    @Test fun loginRejectsMissingTokenAndMismatchedUserId() = runBlocking {
        server.enqueue(json("""{"user_id":42}"""))
        assertThrows(InvalidAuthResponseException::class.java) {
            runBlocking { repository.authenticateSession("v@example.com", "password") }
        }
        server.takeRequest()

        server.enqueue(json("""{"authToken":"fixture-token","user_id":42}"""))
        server.enqueue(json("""{"id":43,"Nombre":"Vecina","correo":"v@example.com"}"""))
        assertThrows(InvalidAuthResponseException::class.java) {
            runBlocking { repository.authenticateSession("v@example.com", "password") }
        }
        Unit
    }

    @Test fun successfulSignupWithUnavailableProfileDoesNotInviteDuplicateSignup() = runBlocking {
        server.enqueue(json("""{"authToken":"new-token","user_id":57}"""))
        server.enqueue(MockResponse.Builder().code(401).body("{}") .build())
        assertThrows(AccountCreatedSessionUnavailableException::class.java) {
            runBlocking { repository.registerSession("Vecina", "v@example.com", "password") }
        }
        Unit
    }

    @Test fun networkTimeoutAndServerFailureAreNotInvalidCredentials() = runBlocking {
        server.enqueue(MockResponse.Builder().code(503).body("{}") .build())
        val serverError = assertThrows(AuthServerException::class.java) {
            runBlocking { repository.authenticateSession("v@example.com", "password") }
        }
        assertEquals(503, serverError.statusCode)

        val slowRepository = XanoAuthRepository(XanoApiClient.create(
            server.url("/api:6kDB2GLk/").toString(),
            OkHttpClient.Builder().readTimeout(50, TimeUnit.MILLISECONDS)
                .callTimeout(1, TimeUnit.SECONDS).build()
        ))
        server.enqueue(MockResponse.Builder().headersDelay(300, TimeUnit.MILLISECONDS)
            .body("{}") .build())
        assertThrows(AuthNetworkException::class.java) {
            runBlocking { slowRepository.authenticateSession("v@example.com", "password") }
        }
        Unit
    }

    private fun json(body: String) = MockResponse.Builder()
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}
