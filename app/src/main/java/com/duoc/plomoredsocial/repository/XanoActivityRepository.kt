package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.data.remote.xano.XanoActivityDto
import com.duoc.plomoredsocial.data.remote.xano.XanoApiService
import com.google.gson.JsonElement
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

/** A read-only remote source. It never writes Xano data into Room. */
class XanoActivityRepository(private val service: XanoApiService) {
    suspend fun getActivities(): XanoResult<List<XanoActivityDto>> {
        val payload = when (val response = request { service.getActivities() }) {
            is XanoResult.Success -> response.value
            is XanoResult.HttpFailure -> return response
            XanoResult.EmptyBody -> return XanoResult.EmptyBody
            XanoResult.InvalidPayload -> return XanoResult.InvalidPayload
            XanoResult.NetworkFailure -> return XanoResult.NetworkFailure
        }
        if (!payload.isJsonArray) return XanoResult.InvalidPayload
        val records = mutableListOf<XanoActivityDto>()
        payload.asJsonArray.forEach { item ->
            if (!item.isJsonObject) return XanoResult.InvalidPayload
            records += XanoActivityDto(item.asJsonObject)
        }
        return XanoResult.Success(records)
    }

    suspend fun getActivity(id: String): XanoResult<JsonElement> {
        if (id.isBlank()) return XanoResult.InvalidPayload
        return request { service.getActivity(id.trim()) }
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
