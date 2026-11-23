package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.model.AuthUser

/** The credential never belongs in Compose state or an [AuthUser]. */
class AuthSession(val user: AuthUser, val token: String? = null) {
    override fun toString(): String = "AuthSession(user=$user, token=[redacted])"
}

// Contrato reemplazable por una implementación con base de datos o API.
interface AuthRepository {
    fun observeUser(id: String): kotlinx.coroutines.flow.Flow<AuthUser?> = kotlinx.coroutines.flow.flow {
        emit(getUserById(id))
    }
    suspend fun authenticate(email: String, password: String): AuthUser?
    suspend fun getUserById(id: String): AuthUser?
    suspend fun register(name: String, email: String, password: String): AuthUser =
        throw UnsupportedOperationException("Este repositorio no registra cuentas.")

    // Los repositorios locales conservan su contrato; Xano aporta el token.
    suspend fun authenticateSession(email: String, password: String): AuthSession? =
        authenticate(email, password)?.let(::AuthSession)

    suspend fun registerSession(name: String, email: String, password: String): AuthSession =
        AuthSession(register(name, email, password))

    suspend fun getUserByToken(token: String): AuthUser? = null
}

class DuplicateEmailException : Exception("El correo ya está registrado.")
class InvalidCredentialsException : Exception("Credenciales inválidas.")
class AuthNetworkException : Exception("No se pudo conectar con Xano.")
class InvalidAuthResponseException : Exception("La respuesta de autenticación no es válida.")
class AuthServerException(val statusCode: Int) : Exception("Xano respondió HTTP $statusCode.")
class AccountCreatedSessionUnavailableException : Exception("La cuenta se creó, pero no se pudo iniciar la sesión.")
