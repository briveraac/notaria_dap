// Une los datos del formulario con Room, sin depender de la pantalla.
package com.duoc.plomoredsocial.data.contact

import com.duoc.plomoredsocial.data.local.contact.ContactDao
import com.duoc.plomoredsocial.data.local.contact.ContactEntity
import com.duoc.plomoredsocial.domain.contact.ContactRequest
import com.duoc.plomoredsocial.domain.contact.ContactValidator

class ContactRepository(private val dao: ContactDao) {
    // También validamos aquí para que otro punto de entrada no guarde datos inválidos.
    suspend fun save(request: ContactRequest): Long {
        require(ContactValidator.validate(request).isValid) { "Datos de contacto inválidos" }
        val contact = ContactEntity(
            alias = request.nameOrAlias.trim(),
            email = request.email.trim(),
            reason = requireNotNull(request.reason).name,
            message = request.message.trim(),
            createdAt = System.currentTimeMillis()
        )
        return dao.insert(contact).also { check(it > 0) }
    }

    suspend fun getAll(): List<ContactEntity> = dao.getAll()
    suspend fun getById(id: Long): ContactEntity? = dao.getById(id)
}
