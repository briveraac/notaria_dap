package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.data.remote.xano.XanoApiService
import com.duoc.plomoredsocial.model.AuthUser
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.util.Locale

/** Xano owns identities. Room users and demo credentials are never consulted here. */
class XanoAuthRepository(private val service: XanoApiService) : AuthRepository {
    override suspend fun authenticate(email: String, password: String): AuthUser? =
        authenticateSession(email, password)?.user

    override suspend fun register(name: String, email: String, password: String): AuthUser =
        registerSession(name, email, password).user

    // A remote ID alone cannot authenticate a request. Restore only through a bearer token.
    override suspend fun getUserById(id: String): AuthUser? = null

    override suspend fun authenticateSession(email: String, password: String): AuthSession {
        if (email.isBlank() || password.isEmpty()) throw InvalidCredentialsException()
        val body = JsonObject().apply {
            addProperty("email", normalizeEmail(email))
            addProperty("password", password)
        }
        val response = request { service.login(body) }
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            if (response.code() in listOf(400, 401, 403)) throw InvalidCredentialsException()
            throw AuthServerException(response.code())
        }
        val credential = parseCredential(response.body())
        val user = getUserByToken(credential.token) ?: throw InvalidAuthResponseException()
        if (user.id != credential.userId) throw InvalidAuthResponseException()
        return AuthSession(user, credential.token)
    }

    override suspend fun registerSession(name: String, email: String, password: String): AuthSession {
        val body = JsonObject().apply {
            addProperty("name", name.trim())
            addProperty("email", normalizeEmail(email))
            addProperty("password", password)
        }
        val response = request { service.signup(body) }
        if (!response.isSuccessful) {
            val duplicate = response.code() == 409 || isDuplicateEmail(response)
            response.errorBody()?.close()
            if (duplicate) throw DuplicateEmailException()
            throw AuthServerException(response.code())
        }
        // The account can already exist after HTTP 2xx. Never tell the user to retry
        // signup if the follow-up profile lookup or response parsing fails.
        try {
            val credential = parseCredential(response.body())
            val user = getUserByToken(credential.token) ?: throw InvalidAuthResponseException()
            if (user.id != credential.userId) throw InvalidAuthResponseException()
            return AuthSession(user, credential.token)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw AccountCreatedSessionUnavailableException()
        }
    }

    override suspend fun getUserByToken(token: String): AuthUser? {
        if (token.isBlank()) return null
        val response = request { service.getAuthMe("Bearer ${token.trim()}") }
        if (response.code() in listOf(401, 403)) {
            response.errorBody()?.close()
            return null
        }
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw AuthServerException(response.code())
        }
        return parseUser(response.body())
    }

    private suspend fun request(call: suspend () -> Response<JsonElement>): Response<JsonElement> =
        try {
            call()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            throw AuthNetworkException()
        } catch (_: Exception) {
            throw InvalidAuthResponseException()
        }

    private fun parseCredential(body: JsonElement?): Credential {
        val obj = body?.takeIf { it.isJsonObject }?.asJsonObject ?: throw InvalidAuthResponseException()
        val token = obj.text("authToken") ?: throw InvalidAuthResponseException()
        val userId = obj.text("user_id") ?: throw InvalidAuthResponseException()
        return Credential(token, userId)
    }

    private fun parseUser(body: JsonElement?): AuthUser {
        val root = body?.takeIf { it.isJsonObject }?.asJsonObject ?: throw InvalidAuthResponseException()
        val obj = when {
            root.has("user") && root.get("user").isJsonObject -> root.getAsJsonObject("user")
            root.has("data") && root.get("data").isJsonObject -> root.getAsJsonObject("data")
            else -> root
        }
        val id = obj.text("id") ?: throw InvalidAuthResponseException()
        val name = obj.text("Nombre") ?: obj.text("nombre") ?: obj.text("name")
            ?: throw InvalidAuthResponseException()
        val email = obj.text("correo") ?: obj.text("email") ?: throw InvalidAuthResponseException()
        return AuthUser(id = id, name = name, email = normalizeEmail(email))
    }

    private fun JsonObject.text(name: String): String? = try {
        get(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun isDuplicateEmail(response: Response<JsonElement>): Boolean {
        if (response.code() !in listOf(400, 422)) return false
        // The body is examined only for classification and is never logged or displayed.
        val body = try { response.errorBody()?.string()?.take(4096)?.lowercase(Locale.ROOT) }
            catch (_: IOException) { null } ?: return false
        return listOf("already", "exist", "duplicat", "registrad", "en uso", "in use")
            .any(body::contains)
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    private data class Credential(val token: String, val userId: String)
}
