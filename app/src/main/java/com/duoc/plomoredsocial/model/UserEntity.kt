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
    val isDemo: Boolean,
    @androidx.room.ColumnInfo(defaultValue = "'USER'") val role: String = "USER"
) {
    fun toAuthUser() = AuthUser(id, name, email, isDemo,
        UserRole.entries.firstOrNull { it.name == role } ?: UserRole.USER)
}

// Proyección segura para listas y sesión: nunca incluye credenciales.
data class UserAccount(val id: String, val name: String, val email: String, val isDemo: Boolean, val role: String) {
    fun toAuthUser() = AuthUser(id, name, email, isDemo,
        UserRole.entries.firstOrNull { it.name == role } ?: UserRole.USER)
}
