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

/** Confirmed distinct REST endpoints in the Xano API group. */
interface XanoApiService {
    /** 1. GET /actividades - List remote activities (supports optional comuna query param). */
    @GET("actividades")
    suspend fun getActivities(@Query("comuna") comuna: String? = null): Response<JsonElement>

    /** 2. GET /actividades/{actividades_id} - Retrieve activity detail by ID. */
    @GET("actividades/{actividades_id}")
    suspend fun getActivity(@Path("actividades_id") id: String): Response<JsonElement>

    /** 3. POST /actividades - Create a new activity in Xano. */
    @POST("actividades")
    suspend fun createActivity(@Body body: JsonObject): Response<JsonElement>

    /** 4. PATCH /actividades/{actividades_id} - Update an activity in Xano. */
    @PATCH("actividades/{actividades_id}")
    suspend fun updateActivity(
        @Path("actividades_id") id: String,
        @Body body: JsonObject
    ): Response<JsonElement>

    /** 5. DELETE /actividades/{actividades_id} - Delete an activity in Xano. */
    @DELETE("actividades/{actividades_id}")
    suspend fun deleteActivity(@Path("actividades_id") id: String): Response<JsonElement>

    /** 6. POST /auth/login - Remote login authentication endpoint. */
    @POST("auth/login")
    suspend fun login(@Body body: JsonObject): Response<JsonElement>

    /** 7. GET /auth/me - Retrieve current authenticated user profile. */
    @GET("auth/me")
    suspend fun getAuthMe(@Header("Authorization") token: String): Response<JsonElement>

    /** 8. POST /auth/signup - Remote user registration endpoint. */
    @POST("auth/signup")
    suspend fun signup(@Body body: JsonObject): Response<JsonElement>
}
