package com.duoc.plomoredsocial.data.remote.xano

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** Read-only calls that are confirmed in the supplied Xano contract. */
interface XanoApiService {
    @GET("actividades")
    suspend fun getActivities(): Response<JsonElement>

    @GET("actividades/{actividades_id}")
    suspend fun getActivity(@Path("actividades_id") id: String): Response<JsonElement>
}
