package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.data.remote.xano.XanoApiService
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.model.PRIMARY_DEMO_ID
import com.duoc.plomoredsocial.model.UserRole
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.util.Locale

/** Xano owns identities, with fallback support for local demo and test accounts. */
class XanoAuthRepository(
    private val service: XanoApiService,
    private val roomRepository: RoomAuthRepository? = null
) : AuthRepository {
    override suspend fun authenticate(email: String, password: String): AuthUser? =
        authenticateSession(email, password)?.user

    override suspend fun register(name: String, email: String, password: String): AuthUser =
        registerSession(name, email, password).user

    override suspend fun getUserById(id: String): AuthUser? {
        if (id == PRIMARY_DEMO_ID) {
            return AuthUser(PRIMARY_DEMO_ID, "Alex", "demo@mygos.cl", isDemo = true, role = UserRole.ADMIN)
        }
        return roomRepository?.getUserById(id)
    }

    override suspend fun authenticateSession(email: String, password: String): AuthSession {
        if (email.isBlank() || password.isEmpty()) throw InvalidCredentialsException()
        val normalized = normalizeEmail(email)
        if (normalized == "demo@mygos.cl" && password == "Mygos2026!") {
            val user = AuthUser(PRIMARY_DEMO_ID, "Alex", "demo@mygos.cl", isDemo = true, role = UserRole.ADMIN)
            return AuthSession(user, "demo-token")
        }
        val body = JsonObject().apply {
            addProperty("email", normalized)
            addProperty("password", password)
        }
        try {
            val response = request { service.login(body) }
            if (response.isSuccessful) {
                val credential = parseCredential(response.body())
                val user = getUserByToken(credential.token) ?: throw InvalidAuthResponseException()
                if (user.id != credential.userId) throw InvalidAuthResponseException()
                return AuthSession(user, credential.token)
            }
            if (response.code() in listOf(400, 401, 403, 500)) {
                roomRepository?.authenticate(email, password)?.let { localUser ->
                    return AuthSession(localUser, "local-token-${localUser.id}")
                }
                throw InvalidCredentialsException()
            }
            throw AuthServerException(response.code())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (invalid: InvalidCredentialsException) {
            throw invalid
        } catch (err: Exception) {
            roomRepository?.authenticate(email, password)?.let { localUser ->
                return AuthSession(localUser, "local-token-${localUser.id}")
            }
            throw err
        }
    }

    override suspend fun registerSession(name: String, email: String, password: String): AuthSession {
        val normalized = normalizeEmail(email)
        val body = JsonObject().apply {
            addProperty("name", name.trim())
            addProperty("email", normalized)
            addProperty("password", password)
        }
        try {
            val response = request { service.signup(body) }
            if (response.isSuccessful) {
                try {
                    val credential = parseCredential(response.body())
                    val user = getUserByToken(credential.token) ?: throw AccountCreatedSessionUnavailableException()
                    if (user.id != credential.userId) throw AccountCreatedSessionUnavailableException()
                    return AuthSession(user, credential.token)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    throw AccountCreatedSessionUnavailableException()
                }
            }
            val duplicate = response.code() == 409 || isDuplicateEmail(response)
            response.errorBody()?.close()
            if (duplicate) throw DuplicateEmailException()
            if (roomRepository != null) {
                val localUser = roomRepository.register(name, email, password)
                return AuthSession(localUser, "local-token-${localUser.id}")
            }
            throw AuthServerException(response.code())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (dup: DuplicateEmailException) {
            throw dup
        } catch (acc: AccountCreatedSessionUnavailableException) {
            throw acc
        } catch (err: Exception) {
            if (roomRepository != null) {
                val localUser = roomRepository.register(name, email, password)
                return AuthSession(localUser, "local-token-${localUser.id}")
            }
            throw err
        }
    }

    override suspend fun getUserByToken(token: String): AuthUser? {
        if (token.isBlank()) return null
        if (token == "demo-token") {
            return AuthUser(PRIMARY_DEMO_ID, "Alex", "demo@mygos.cl", isDemo = true, role = UserRole.ADMIN)
        }
        if (token.startsWith("local-token-")) {
            val localId = token.removePrefix("local-token-")
            return roomRepository?.getUserById(localId)
        }
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
        val body = try { response.errorBody()?.string()?.take(4096)?.lowercase(Locale.ROOT) }
            catch (_: IOException) { null } ?: return false
        return listOf("already", "exist", "duplicat", "registrad", "en uso", "in use")
            .any(body::contains)
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    private data class Credential(val token: String, val userId: String)
}
