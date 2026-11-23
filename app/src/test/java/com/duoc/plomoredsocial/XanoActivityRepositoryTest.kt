package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.data.remote.xano.XanoApiClient
import com.duoc.plomoredsocial.repository.XanoActivityRepository
import com.duoc.plomoredsocial.repository.XanoResult
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class XanoActivityRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: XanoActivityRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = repositoryWith(OkHttpClient.Builder()
            .callTimeout(2, TimeUnit.SECONDS)
            .build())
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun endpoint1_getActivitiesParsesEmptyArrayAndUsesConfirmedPath() = runBlocking {
        server.enqueue(jsonResponse("[]"))

        val result = repository.getActivities()

        assertEquals(0, (result as XanoResult.Success).value.size)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api:6kDB2GLk/actividades", request.url.encodedPath)
    }

    @Test
    fun getActivitiesPreservesUnknownObjectFieldsWithoutMappingAssumptions() = runBlocking {
        server.enqueue(jsonResponse("[{\"fixture_marker\":17,\"nested\":{\"ok\":true}}]"))

        val result = repository.getActivities()

        assertTrue(result is XanoResult.Success)
        val records = (result as XanoResult.Success).value
        assertEquals(1, records.size)
        assertEquals(17, records.single().raw.get("fixture_marker").asInt)
    }

    @Test
    fun endpoint2_getActivityUsesConfirmedDetailRouteAndEncodesTheIdentifier() = runBlocking {
        server.enqueue(jsonResponse("{\"fixture_marker\":\"ok\"}"))

        val result = repository.getActivity("id with space")

        assertTrue(result is XanoResult.Success)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api:6kDB2GLk/actividades/id%20with%20space", request.url.encodedPath)
    }

    @Test
    fun endpoint3_createActivitySendsPostWithBodyAndRestrictsCuposToRange() = runBlocking {
        server.enqueue(jsonResponse("{\"id\":10,\"titulo\":\"Nuevo Evento\",\"cupos\":10}"))

        // Cupos 50 is passed, but repository restricts it to 10 (range 1..10)
        val result = repository.createActivity("Nuevo Evento", "Desc", "Santiago", "Plaza", "Org", 50)

        assertTrue(result is XanoResult.Success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api:6kDB2GLk/actividades", request.url.encodedPath)
        val bodyText = request.body?.utf8() ?: ""
        assertTrue(bodyText.contains("Nuevo Evento"))
        assertTrue(bodyText.contains("\"cupos\":10"))
    }

    @Test
    fun endpoint4_updateActivitySendsPatchRequestAndRestrictsCupos() = runBlocking {
        server.enqueue(jsonResponse("{\"id\":10,\"cupos\":10}"))

        val fields = JsonObject().apply { addProperty("cupos", 25) }
        val result = repository.updateActivity("10", fields)

        assertTrue(result is XanoResult.Success)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api:6kDB2GLk/actividades/10", request.url.encodedPath)
        val bodyText = request.body?.utf8() ?: ""
        assertTrue(bodyText.contains("\"cupos\":10"))
    }

    @Test
    fun endpoint5_deleteActivitySendsDeleteRequest() = runBlocking {
        server.enqueue(MockResponse.Builder().code(200).body("null").build())

        val result = repository.deleteActivity("10")

        assertTrue(result is XanoResult.Success || result is XanoResult.EmptyBody)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api:6kDB2GLk/actividades/10", request.url.encodedPath)
    }

    @Test
    fun endpoint6_loginRemoteSendsPostToAuthLogin() = runBlocking {
        server.enqueue(MockResponse.Builder().code(400).body("{\"message\":\"Bad Request\"}").build())

        val result = repository.loginRemote("demo@mygos.cl", "pass")

        assertEquals(XanoResult.HttpFailure(400), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api:6kDB2GLk/auth/login", request.url.encodedPath)
        assertTrue(request.body?.utf8()?.contains("demo@mygos.cl") == true)
    }

    @Test
    fun endpoint7_getAuthMeSendsHeaderAndQueryRoute() = runBlocking {
        server.enqueue(MockResponse.Builder().code(401).body("{\"message\":\"Unauthorized\"}").build())

        val result = repository.getAuthMe()

        assertEquals(XanoResult.HttpFailure(401), result)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api:6kDB2GLk/auth/me", request.url.encodedPath)
        assertEquals("Bearer guest", request.headers["Authorization"])
    }

    @Test
    fun endpoint8_signupRemoteSendsPostToAuthSignup() = runBlocking {
        server.enqueue(MockResponse.Builder().code(400).body("{\"message\":\"Bad Request\"}").build())

        val result = repository.signupRemote("Usuario Test", "test@mygos.cl", "pass")

        assertEquals(XanoResult.HttpFailure(400), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api:6kDB2GLk/auth/signup", request.url.encodedPath)
        assertTrue(request.body?.utf8()?.contains("Usuario Test") == true)
    }

    @Test
    fun mapsAuthorizationAndServerHttpErrorsWithoutExposingBodies() = runBlocking {
        listOf(401, 403, 404, 500).forEach { status ->
            server.enqueue(MockResponse.Builder().code(status).body("private fixture body").build())
            val result = repository.getActivities()
            assertEquals(XanoResult.HttpFailure(status), result)
        }
    }

    @Test
    fun distinguishesSuccessfulEmptyBodyAndInvalidJsonShape() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertEquals(XanoResult.EmptyBody, repository.getActivities())

        server.enqueue(jsonResponse("{\"not\":\"an array\"}"))
        assertEquals(XanoResult.InvalidPayload, repository.getActivities())
    }

    @Test
    fun turnsReadTimeoutIntoNetworkFailure() = runBlocking {
        val slowRepository = repositoryWith(OkHttpClient.Builder()
            .readTimeout(50, TimeUnit.MILLISECONDS)
            .callTimeout(1, TimeUnit.SECONDS)
            .build())
        server.enqueue(MockResponse.Builder()
            .headersDelay(300, TimeUnit.MILLISECONDS)
            .body("[]")
            .build())

        assertEquals(XanoResult.NetworkFailure, slowRepository.getActivities())
    }

    private fun repositoryWith(client: OkHttpClient): XanoActivityRepository {
        val base = server.url("/api:6kDB2GLk/").toString()
        return XanoActivityRepository(XanoApiClient.create(base, client))
    }

    private fun jsonResponse(body: String) = MockResponse.Builder()
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}
