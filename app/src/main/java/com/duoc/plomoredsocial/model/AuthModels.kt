package com.duoc.plomoredsocial.model

enum class UserRole { ADMIN, USER }

// El correo es editable; la identidad y el origen de la cuenta principal son estables.
const val PRIMARY_DEMO_ID = "demo-001"
data class AuthUser(val id: String, val name: String, val email: String, val isDemo: Boolean = false,
    val role: UserRole = UserRole.USER) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN && id == PRIMARY_DEMO_ID && isDemo
}

enum class LoginError { EMAIL_REQUIRED, EMAIL_FORMAT, PASSWORD_REQUIRED, INVALID_CREDENTIALS, UNAVAILABLE }

object LoginValidator {
    fun emailError(email: String): LoginError? = when {
        email.isBlank() -> LoginError.EMAIL_REQUIRED
        email.trim().length > 254 ||
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()) -> LoginError.EMAIL_FORMAT
        else -> null
    }
}
