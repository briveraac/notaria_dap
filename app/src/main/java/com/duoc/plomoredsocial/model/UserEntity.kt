package com.duoc.plomoredsocial.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Solo persistencia: las credenciales nunca se entregan a la interfaz mediante AuthUser.
@Entity(tableName = "users", indices = [Index(value = ["normalizedEmail"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val normalizedEmail: String,
    val passwordHash: String,
    val salt: String,
    val algorithm: String,
    val iterations: Int,
    val keyLength: Int,
    val formatVersion: Int,
    val isDemo: Boolean
) {
    fun toAuthUser() = AuthUser(id, name, email, isDemo)
}
