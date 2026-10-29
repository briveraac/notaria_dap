package com.duoc.plomoredsocial.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.duoc.plomoredsocial.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

data class AdminSnapshot(val users: List<UserAccount>, val activities: List<ActivityEntity>,
    val members: List<ActivityMember>)

class AdminOperationException(message: String) : Exception(message)

// La autorización se obtiene de la sesión persistida y se revalida en Room en cada operación.
// La interfaz no puede pasar otro ID de administrador para saltarse esta comprobación.
class AdminRepository(private val database: MygosDatabase, private val session: SessionStore,
    private val passwords: PasswordDeriver = PasswordDeriver()) {
    private val users = database.userDao()
    private val activities = database.activityDao()

    private suspend fun requireAdmin(): UserEntity {
        val id = session.readUserId() ?: throw SecurityException("Acceso administrativo requerido.")
        val user = users.findById(id)
        if (user?.toAuthUser()?.isAdmin != true) throw SecurityException("Acceso administrativo requerido.")
        return user
    }

    fun observePanel(): Flow<AdminSnapshot> = flow {
        requireAdmin()
        emitAll(combine(users.observeAccounts(), activities.observeActivities(), activities.observeMembers()) {
                accounts, events, members ->
            requireAdmin()
            AdminSnapshot(accounts, events, members)
        })
    }

    suspend fun updateUser(id: String, name: String, email: String) {
        requireAdmin()
        RegisterValidator.nameError(name)?.let { throw AdminOperationException(it) }
        RegisterValidator.emailError(email)?.let { throw AdminOperationException(it) }
        try {
            database.withTransaction {
                requireAdmin()
                val normalized = RegisterValidator.normalizeEmail(email)
                val other = users.findByEmail(normalized)
                if (other != null && other.id != id) throw DuplicateEmailException()
                if (users.updateProfile(id, name.trim(), email.trim(), normalized) != 1)
                    throw AdminOperationException("La cuenta ya no está disponible.")
            }
        } catch (_: SQLiteConstraintException) { throw DuplicateEmailException() }
    }

    suspend fun resetPassword(id: String, password: String) {
        requireAdmin()
        RegisterValidator.passwordError(password)?.let { throw AdminOperationException(it) }
        // La derivación costosa queda fuera de la transacción; nunca se almacena texto plano.
        val record = withContext(Dispatchers.Default) { passwords.create(password) }
        database.withTransaction {
            requireAdmin()
            if (users.updatePassword(id, record.hash, record.salt, record.algorithm, record.iterations,
                    record.keyLength, record.formatVersion) != 1)
                throw AdminOperationException("La cuenta ya no está disponible.")
        }
    }

    suspend fun deleteUser(id: String) {
        database.withTransaction {
            requireAdmin()
            val target = users.findById(id) ?: throw AdminOperationException("La cuenta ya no está disponible.")
            if (target.toAuthUser().isAdmin && users.countAdministrators() <= 1)
                throw AdminOperationException("No puedes eliminar al último administrador.")
            val organized = activities.countOrganized(id)
            if (organized > 0) throw AdminOperationException(
                "Esta cuenta organiza $organized actividad(es). Debes resolverlas primero en Actividades.")
            activities.removeUserMemberships(id)
            check(users.delete(id) == 1)
        }
        // El archivo de sesión no comparte transacción con SQLite. Un ID borrado nunca autoriza
        // operaciones; el observador de sesión y la restauración descartan cualquier ID inválido.
        if (session.readUserId() == id) session.clear()
    }

    suspend fun deleteActivity(id: String) {
        database.withTransaction {
            requireAdmin()
            val activity = activities.find(id) ?: throw AdminOperationException("La actividad ya no está disponible.")
            // Reutiliza la eliminación atómica de MAIN-005 sin duplicar su SQL de limpieza.
            check(activities.deleteAsOrganizer(id, activity.organizerId))
        }
    }
}
