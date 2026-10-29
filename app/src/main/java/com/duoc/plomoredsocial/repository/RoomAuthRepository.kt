package com.duoc.plomoredsocial.repository

import android.content.res.AssetManager
import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.duoc.plomoredsocial.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

// Todas las cuentas se consultan en Room; los datos demo solo se importan una vez.
class RoomAuthRepository(
    private val database: MygosDatabase,
    private val assets: AssetManager,
    private val passwords: PasswordDeriver = PasswordDeriver()
) : AuthRepository {
    private val users = database.userDao()
    @Volatile private var initialized = false

    private suspend fun ensureDemoUsers() {
        if (initialized) return
        val seeds = withContext(Dispatchers.IO) {
            assets.open("temporary_auth/users.json").bufferedReader().use { reader ->
                val records = JSONArray(reader.readText())
                (0 until records.length()).map { index ->
                    val record = records.getJSONObject(index)
                    UserEntity(record.getString("id"), record.getString("name"), record.getString("email"),
                        RegisterValidator.normalizeEmail(record.getString("email")), record.getString("passwordHash"),
                        record.getString("salt"), PasswordDeriver.LEGACY_ALGORITHM,
                        record.getInt("iterations"), 256, 0, true,
                        if (record.getString("id") == PRIMARY_DEMO_ID) "ADMIN" else "USER")
                }
            }
        }
        database.withTransaction {
            seeds.forEach { user ->
                val existing = users.findById(user.id)
                if (existing != null) {
                    // La cuenta ya importada conserva nombre, correo, contraseña y rol editados.
                    check(existing.isDemo) { "El ID demo pertenece a otro origen." }
                } else {
                    // Un correo coincidente nunca convierte una cuenta normal en administradora.
                    check(users.findByEmail(user.normalizedEmail) == null) { "Conflicto al importar la cuenta demo." }
                    users.insertDemo(user)
                    check(users.findById(user.id)?.isDemo == true)
                }
            }
        }
        initialized = true
    }

    override suspend fun getUserById(id: String): AuthUser? {
        ensureDemoUsers()
        return users.findById(id)?.toAuthUser()
    }

    override fun observeUser(id: String): kotlinx.coroutines.flow.Flow<AuthUser?> = kotlinx.coroutines.flow.flow {
        ensureDemoUsers()
        emitAll(users.observeAccount(id).map { it?.toAuthUser() })
    }

    override suspend fun authenticate(email: String, password: String): AuthUser? {
        if (password.length > 128) return null
        ensureDemoUsers()
        val user = users.findByEmail(RegisterValidator.normalizeEmail(email)) ?: return null
        val matches = withContext(Dispatchers.Default) { passwords.verify(password, user) }
        return if (matches) user.toAuthUser() else null
    }

    override suspend fun register(name: String, email: String, password: String): AuthUser {
        require(RegisterValidator.nameError(name) == null && RegisterValidator.emailError(email) == null &&
            RegisterValidator.passwordError(password) == null)
        ensureDemoUsers()
        val normalized = RegisterValidator.normalizeEmail(email)
        if (users.findByEmail(normalized) != null) throw DuplicateEmailException()
        // El cálculo costoso queda fuera del hilo principal y de la transacción de escritura.
        val record = withContext(Dispatchers.Default) { passwords.create(password) }
        val user = UserEntity("local-" + UUID.randomUUID(), name.trim(), email.trim(), normalized,
            record.hash, record.salt, record.algorithm, record.iterations, record.keyLength, record.formatVersion, false)
        try { users.insert(user) }
        catch (duplicate: SQLiteConstraintException) {
            if (users.findByEmail(normalized) != null) throw DuplicateEmailException()
            throw duplicate
        }
        return user.toAuthUser()
    }
}
