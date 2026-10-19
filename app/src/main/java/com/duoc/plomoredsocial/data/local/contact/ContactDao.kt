// Consultas que Room convierte en operaciones SQLite.
package com.duoc.plomoredsocial.data.local.contact

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {
    // Devuelve el ID solo cuando termina la inserción.
    @Insert
    suspend fun insert(contact: ContactEntity): Long

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?
}
