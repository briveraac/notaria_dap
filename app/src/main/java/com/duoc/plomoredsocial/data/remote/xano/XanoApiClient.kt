package com.duoc.plomoredsocial.data.remote.xano

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object XanoApiClient {
    const val BASE_URL = "https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    val service: XanoApiService by lazy { create(BASE_URL, client) }

    fun create(baseUrl: String, httpClient: OkHttpClient): XanoApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(XanoApiService::class.java)
}
