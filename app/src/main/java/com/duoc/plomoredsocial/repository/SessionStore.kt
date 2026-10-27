package com.duoc.plomoredsocial.repository

interface SessionStore {
    suspend fun readUserId(): String?
    suspend fun saveUserId(userId: String)
    suspend fun clear()
}
