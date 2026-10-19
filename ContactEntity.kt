// Cada fila es una consulta guardada en este dispositivo.
package com.duoc.plomoredsocial.data.local.contact

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val email: String,
    val reason: String,
    val message: String,
    // Fecha de creación en milisegundos desde el inicio de Unix.
    val createdAt: Long
)
