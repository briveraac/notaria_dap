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

/** Remote Xano repository for 7 REST operations. Xano data is kept separate from Room. */
class XanoActivityRepository(private val service: XanoApiService) {

    /** 1. GET /actividades */
    suspend fun getActivities(): XanoResult<List<XanoActivityDto>> {
        val payload = when (val response = request { service.getActivities() }) {
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

    /** 3. GET /actividades?comuna={comuna} */
    suspend fun searchActivitiesByComuna(comuna: String): XanoResult<List<XanoActivityDto>> {
        if (comuna.isBlank()) return getActivities()
        val payload = when (val response = request { service.searchActivitiesByComuna(comuna.trim()) }) {
            is XanoResult.Success -> response.value
            is XanoResult.HttpFailure -> return response
            XanoResult.EmptyBody -> return XanoResult.EmptyBody
            XanoResult.InvalidPayload -> return XanoResult.InvalidPayload
            XanoResult.NetworkFailure -> return XanoResult.NetworkFailure
        }
        return parseActivityList(payload)
    }

    /** 4. POST /actividades */
    suspend fun createActivity(
        titulo: String,
        descripcion: String,
        comuna: String,
        ubicacion: String,
        organizador: String,
        cupos: Int
    ): XanoResult<XanoActivityDto> {
        val body = JsonObject().apply {
            addProperty("titulo", titulo)
            addProperty("descripcion", descripcion)
            addProperty("comuna", comuna)
            addProperty("ubicacion", ubicacion)
            addProperty("organizador", organizador)
            addProperty("cupos", cupos)
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

    /** 5. PATCH /actividades/{actividades_id} */
    suspend fun updateActivity(id: String, fields: JsonObject): XanoResult<JsonElement> {
        if (id.isBlank()) return XanoResult.InvalidPayload
        return request { service.updateActivity(id.trim(), fields) }
    }

    /** 6. DELETE /actividades/{actividades_id} */
    suspend fun deleteActivity(id: String): XanoResult<JsonElement> {
        if (id.isBlank()) return XanoResult.InvalidPayload
        return request { service.deleteActivity(id.trim()) }
    }

    /** 7. GET /auth/me */
    suspend fun getAuthMe(token: String = ""): XanoResult<JsonElement> {
        val authHeader = if (token.isBlank()) "Bearer guest" else "Bearer ${token.trim()}"
        return request { service.getAuthMe(authHeader) }
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
