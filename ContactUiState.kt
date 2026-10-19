// Datos y avisos que se muestran en el formulario.
package com.duoc.plomoredsocial.ui.contact

import com.duoc.plomoredsocial.domain.contact.ContactErrors
import com.duoc.plomoredsocial.domain.contact.ContactReason
import com.duoc.plomoredsocial.domain.contact.ContactRequest

// Guarda lo escrito y los errores de cada campo.
// validationAttempted activa los errores después del primer Continuar.
// reviewVisible abre el resumen. saveStatus indica si se está guardando o si terminó.
// savedContactId se asigna solo después de insertar en SQLite.
enum class ContactSaveStatus { IDLE, LOADING, SUCCESS, ERROR }

data class ContactUiState(
    val nameOrAlias: String = "",
    val email: String = "",
    val reason: ContactReason? = null,
    val message: String = "",
    val errors: ContactErrors = ContactErrors(),
    val validationAttempted: Boolean = false,
    val reviewVisible: Boolean = false,
    val saveStatus: ContactSaveStatus = ContactSaveStatus.IDLE,
    val savedContactId: Long? = null
) {
    // Quita los espacios de los extremos para validar sin cambiar lo que se ve en los campos.
    fun toRequest() = ContactRequest(
        nameOrAlias.trim(), email.trim(), reason, message.trim()
    )
}
