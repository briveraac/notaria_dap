package com.duoc.plomoredsocial.repository

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Recibe noBackupFilesDir: archivo privado, persistente y excluido de backups.
// Solo se guarda el ID; el perfil se consulta nuevamente al restaurar la sesión.
class LocalSessionStore(directory: File) : SessionStore {
    private val file = AtomicFile(File(directory, "session_user_id"))
    private val mutex = Mutex()

    override suspend fun readUserId(): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                file.readFully().toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
            } catch (missing: FileNotFoundException) {
                null
            }
        }
    }

    override suspend fun saveUserId(userId: String) {
        require(userId.isNotBlank())
        write(userId)
    }

    // Se persiste el cierre antes de actualizar la pantalla.
    override suspend fun clear() = write("")

    private suspend fun write(value: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val stream = file.startWrite()
            try {
                stream.write(value.toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (error: Exception) {
                file.failWrite(stream)
                throw error
            }
        }
    }
}
