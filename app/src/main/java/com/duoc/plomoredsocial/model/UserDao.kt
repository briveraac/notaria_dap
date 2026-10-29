package com.duoc.plomoredsocial.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT id, name, email, isDemo, role FROM users ORDER BY name COLLATE NOCASE, id")
    fun observeAccounts(): kotlinx.coroutines.flow.Flow<List<UserAccount>>

    @Query("SELECT id, name, email, isDemo, role FROM users WHERE id = :id")
    fun observeAccount(id: String): kotlinx.coroutines.flow.Flow<UserAccount?>

    @Query("UPDATE users SET name = :name, email = :email, normalizedEmail = :normalized WHERE id = :id")
    suspend fun updateProfile(id: String, name: String, email: String, normalized: String): Int

    @Query("UPDATE users SET passwordHash = :hash, salt = :salt, algorithm = :algorithm, iterations = :iterations, keyLength = :keyLength, formatVersion = :formatVersion WHERE id = :id")
    suspend fun updatePassword(id: String, hash: String, salt: String, algorithm: String,
        iterations: Int, keyLength: Int, formatVersion: Int): Int

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND id = 'demo-001' AND isDemo = 1")
    suspend fun countAdministrators(): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM users WHERE normalizedEmail = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    // ABORT protege también los registros concurrentes; nunca reemplaza otra cuenta.
    @Insert
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDemo(user: UserEntity)
}
