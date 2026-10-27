package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.model.AuthUser

// Contrato reemplazable por una implementación con base de datos o API.
interface AuthRepository {
    suspend fun authenticate(email: String, password: String): AuthUser?
    suspend fun getUserById(id: String): AuthUser?
}
