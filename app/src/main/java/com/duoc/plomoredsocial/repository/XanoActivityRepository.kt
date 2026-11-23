package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.data.remote.xano.XanoActivityDto
import com.duoc.plomoredsocial.data.remote.xano.XanoApiService
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

sealed interface XanoResult<out T> {
    data class Success<T>(val value: T) : XanoResult<T>
    data class HttpFailure(val statusCode: Int) : XanoResult<Nothing>
    data object EmptyBody : XanoResult<Nothing>
    data object InvalidPayload : XanoResult<Nothing>
    data object NetworkFailure : XanoResult<Nothing>
}

/** Remote Xano repository for distinct REST operations. Xano data is kept separate from Room. */
class XanoActivityRepository(private val service: XanoApiService) {

    /** 1. GET /actividades (optional comuna query parameter). */
    suspend fun getActivities(comunaFilter: String? = null): XanoResult<List<XanoActivityDto>> {
        val queryParam = comunaFilter?.trim()?.ifBlank { null }
        val payload = when (val response = request { service.getActivities(queryParam) }) {
            is XanoResult.Success -> response.value
            is XanoResult.HttpFailure -> return response
            XanoResult.EmptyBody -> return XanoResult.EmptyBody
            XanoResult.InvalidPayload -> return XanoResult.InvalidPayload
            XanoResult.NetworkFailure -> return XanoResult.NetworkFailure
        }
        return parseActivityList(payload)
    }

    /** 2. GET /actividades/{actividades_id} */
    suspend fun getActivity(id: String): XanoResult<JsonElement> {
        if (id.isBlank()) return XanoResult.InvalidPayload
        return request { service.getActivity(id.trim()) }
    }

    /** 3. POST /actividades (Validates data and restricts cupos to range 1..10). */
    suspend fun createActivity(
        titulo: String,
        descripcion: String,
        comuna: String,
        ubicacion: String,
        organizador: String,
        cupos: Int
    ): XanoResult<XanoActivityDto> {
        val validCupos = cupos.coerceIn(1, 10)
        val body = JsonObject().apply {
            addProperty("titulo", titulo.trim())
            addProperty("descripcion", descripcion.trim())
            addProperty("comuna", comuna.trim())
            addProperty("ubicacion", ubicacion.trim())
            addProperty("organizador", organizador.trim())
            addProperty("cupos", validCupos)
        }
        return when (val response = request { service.createActivity(body) }) {
            is XanoResult.Success -> {
                if (response.value.isJsonObject) XanoResult.Success(XanoActivityDto(response.value.asJsonObject))
                else XanoResult.InvalidPayload
            }
            is XanoResult.HttpFailure -> response
            XanoResult.EmptyBody -> XanoResult.EmptyBody
            XanoResult.InvalidPayload -> XanoResult.InvalidPayload
            XanoResult.NetworkFailure -> XanoResult.NetworkFailure
        }
    }

    /** 4. PATCH /actividades/{actividades_id} (Restricts cupos to range 1..10 if present). */
    suspend fun updateActivity(id: String, fields: JsonObject): XanoResult<JsonElement> {
        if (id.isBlank()) return XanoResult.InvalidPayload
        val sanitized = fields.deepCopy()
        if (sanitized.has("cupos") && sanitized.get("cupos").isJsonPrimitive) {
            val rawCupos = sanitized.get("cupos").asInt
            sanitized.addProperty("cupos", rawCupos.coerceIn(1, 10))
        }
        return request { service.updateActivity(id.trim(), sanitized) }
    }

    /** 5. DELETE /actividades/{actividades_id} */
    suspend fun deleteActivity(id: String): XanoResult<JsonElement> {
        if (id.isBlank()) return XanoResult.InvalidPayload
        return request { service.deleteActivity(id.trim()) }
    }

    /** 6. POST /auth/login */
    suspend fun loginRemote(email: String, password: String): XanoResult<JsonElement> {
        val body = JsonObject().apply {
            addProperty("email", email.trim())
            addProperty("password", password)
        }
        return request { service.login(body) }
    }

    /** 7. GET /auth/me */
    suspend fun getAuthMe(token: String = ""): XanoResult<JsonElement> {
        val authHeader = if (token.isBlank()) "Bearer guest" else "Bearer ${token.trim()}"
        return request { service.getAuthMe(authHeader) }
    }

    /** 8. POST /auth/signup */
    suspend fun signupRemote(name: String, email: String, password: String): XanoResult<JsonElement> {
        val body = JsonObject().apply {
            addProperty("name", name.trim())
            addProperty("email", email.trim())
            addProperty("password", password)
        }
        return request { service.signup(body) }
    }

    private fun parseActivityList(payload: JsonElement): XanoResult<List<XanoActivityDto>> {
        if (!payload.isJsonArray) return XanoResult.InvalidPayload
        val records = mutableListOf<XanoActivityDto>()
        payload.asJsonArray.forEach { item ->
            if (!item.isJsonObject) return XanoResult.InvalidPayload
            records += XanoActivityDto(item.asJsonObject)
        }
        return XanoResult.Success(records)
    }

    private suspend fun request(call: suspend () -> Response<JsonElement>): XanoResult<JsonElement> =
        try {
            val response = call()
            if (!response.isSuccessful) XanoResult.HttpFailure(response.code())
            else response.body()?.takeUnless { it.isJsonNull }
                ?.let { XanoResult.Success(it) } ?: XanoResult.EmptyBody
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            XanoResult.NetworkFailure
        } catch (_: Exception) {
            XanoResult.InvalidPayload
        }
}
