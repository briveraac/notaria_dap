package com.duoc.plomoredsocial.data.remote.xano

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Confirmed endpoints in the Xano REST API (7 endpoints). */
interface XanoApiService {
    /** 1. GET /actividades - List all remote activities. */
    @GET("actividades")
    suspend fun getActivities(): Response<JsonElement>

    /** 2. GET /actividades/{actividades_id} - Retrieve activity detail by ID. */
    @GET("actividades/{actividades_id}")
    suspend fun getActivity(@Path("actividades_id") id: String): Response<JsonElement>

    /** 3. GET /actividades?comuna={comuna} - Search/filter activities by comuna query parameter. */
    @GET("actividades")
    suspend fun searchActivitiesByComuna(@Query("comuna") comuna: String): Response<JsonElement>

    /** 4. POST /actividades - Create a new activity in Xano. */
    @POST("actividades")
    suspend fun createActivity(@Body body: JsonObject): Response<JsonElement>

    /** 5. PATCH /actividades/{actividades_id} - Update an activity in Xano. */
    @PATCH("actividades/{actividades_id}")
    suspend fun updateActivity(
        @Path("actividades_id") id: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    /** 6. DELETE /actividades/{actividades_id} - Delete an activity in Xano. */
    @DELETE("actividades/{actividades_id}")
    suspend fun deleteActivity(@Path("actividades_id") id: String): Response<JsonElement>

    /** 7. GET /auth/me - Verify session or auth status against Xano remote auth. */
    @GET("auth/me")
    suspend fun getAuthMe(@Header("Authorization") token: String): Response<JsonElement>
}
