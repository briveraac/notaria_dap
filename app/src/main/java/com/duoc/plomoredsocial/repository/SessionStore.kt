package com.duoc.plomoredsocial.repository

interface SessionStore {
    suspend fun readUserId(): String?
    suspend fun saveUserId(userId: String)
    suspend fun readToken(): String? = null
    suspend fun saveToken(token: String): Unit =
        throw UnsupportedOperationException("Este almacén no guarda tokens.")
    suspend fun clear()
}
