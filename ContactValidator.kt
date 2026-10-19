// Reglas para revisar los datos antes de abrir la confirmación.
package com.duoc.plomoredsocial.domain.contact

// Errores que luego mostramos junto a cada campo.
enum class ContactError {
    REQUIRED, NAME_LENGTH, EMAIL_FORMAT, MESSAGE_LENGTH
}

// Si un campo no tiene error, su valor queda en null.
data class ContactErrors(
    val name: ContactError? = null,
    val email: ContactError? = null,
    val reason: ContactError? = null,
    val message: ContactError? = null
) {
    // El formulario está listo cuando no quedan errores.
    val isValid: Boolean
        get() = name == null && email == null && reason == null && message == null
}

// Aquí se revisan los datos; los mensajes de error están en los recursos de texto.
object ContactValidator {
    const val NAME_MAX = 60
    const val MESSAGE_MAX = 2000

    // No contamos los espacios al inicio o al final.
    fun validate(request: ContactRequest): ContactErrors {
        val name = request.nameOrAlias.trim()
        val email = request.email.trim()
        val message = request.message.trim()
        return ContactErrors(
            // Nombre o alias: obligatorio, entre 2 y 60 caracteres.
            name = when {
                name.isEmpty() -> ContactError.REQUIRED
                name.length !in 2..NAME_MAX -> ContactError.NAME_LENGTH
                else -> null
            },
            // Correo: obligatorio, hasta 254 caracteres y sin espacios internos.
            // Se revisa el formato, no si la cuenta existe.
            email = when {
                email.isEmpty() -> ContactError.REQUIRED
                email.length > 254 || !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email) ->
                    ContactError.EMAIL_FORMAT
                else -> null
            },
            // Debe haber un motivo seleccionado.
            reason = if (request.reason == null) ContactError.REQUIRED else null,
            // Mensaje: obligatorio, entre 10 y 2.000 caracteres.
            message = when {
                message.isEmpty() -> ContactError.REQUIRED
                message.length !in 10..MESSAGE_MAX -> ContactError.MESSAGE_LENGTH
                else -> null
            }
        )
    }
}
