package com.duoc.plomoredsocial.model

data class AuthUser(val id: String, val name: String, val email: String, val isDemo: Boolean = false)

enum class LoginError { EMAIL_REQUIRED, EMAIL_FORMAT, PASSWORD_REQUIRED, INVALID_CREDENTIALS, UNAVAILABLE }

object LoginValidator {
    fun emailError(email: String): LoginError? = when {
        email.isBlank() -> LoginError.EMAIL_REQUIRED
        email.trim().length > 254 ||
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()) -> LoginError.EMAIL_FORMAT
        else -> null
    }
}
