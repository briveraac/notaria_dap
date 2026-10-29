package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.model.AuthUser

// Contrato reemplazable por una implementación con base de datos o API.
interface AuthRepository {
    fun observeUser(id: String): kotlinx.coroutines.flow.Flow<AuthUser?> = kotlinx.coroutines.flow.flow {
        emit(getUserById(id))
    }
    suspend fun authenticate(email: String, password: String): AuthUser?
    suspend fun getUserById(id: String): AuthUser?
    suspend fun register(name: String, email: String, password: String): AuthUser =
        throw UnsupportedOperationException("Este repositorio no registra cuentas.")
}

class DuplicateEmailException : Exception("El correo ya está registrado.")
