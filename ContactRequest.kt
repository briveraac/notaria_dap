// Datos que se completan en Contáctanos.
package com.duoc.plomoredsocial.domain.contact

// Motivos que puede elegir el usuario.
enum class ContactReason {
    QUERY, SUGGESTION, PROBLEM, OTHER
}

// Datos del formulario. El motivo queda en null hasta que el usuario elige una opción.
data class ContactRequest(
    val nameOrAlias: String,
    val email: String,
    val reason: ContactReason?,
    val message: String
)