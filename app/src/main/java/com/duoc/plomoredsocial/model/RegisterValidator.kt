package com.duoc.plomoredsocial.model

import java.util.Locale

object RegisterValidator {
    fun normalizeEmail(email: String) = email.trim().lowercase(Locale.ROOT)
    fun nameError(name: String): String? = when {
        name.isBlank() -> "Ingresa tu nombre o alias."
        name.trim().length > 80 -> "Usa hasta 80 caracteres."
        else -> null
    }
    fun emailError(email: String): String? =
        if (LoginValidator.emailError(email) != null) "Ingresa un correo electrónico válido." else null

    // Se permiten frases y espacios; la contraseña no se recorta ni se normaliza.
    fun passwordError(password: String): String? = when {
        password.length < 12 -> "Usa al menos 12 caracteres."
        password.length > 128 -> "Usa hasta 128 caracteres."
        password.isBlank() -> "La contraseña no puede contener solo espacios."
        else -> null
    }
    fun confirmationError(password: String, confirmation: String): String? =
        if (confirmation.isEmpty()) "Confirma tu contraseña."
        else if (password != confirmation) "Las contraseñas no coinciden." else null
}
