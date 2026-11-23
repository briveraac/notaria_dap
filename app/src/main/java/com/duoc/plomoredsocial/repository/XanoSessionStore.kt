package com.duoc.plomoredsocial.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only an encrypted Xano token in the app-private no-backup directory. */
class XanoSessionStore(directory: File) : SessionStore {
    private val file = AtomicFile(File(directory, "xano_session_token"))
    private val mutex = Mutex()

    // A Room user ID cannot restore a remote identity.
    override suspend fun readUserId(): String? = null

    override suspend fun saveUserId(userId: String): Unit =
        throw UnsupportedOperationException("Xano sessions require a token.")

    override suspend fun readToken(): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = try {
                file.readFully()
            } catch (_: FileNotFoundException) {
                return@withLock null
            }
            try {
                decrypt(bytes).takeIf { it.isNotBlank() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Corrupt ciphertext or a missing AndroidKeyStore key cannot authenticate.
                file.delete()
                null
            }
        }
    }

    override suspend fun saveToken(token: String) {
        require(token.isNotBlank())
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val bytes = encrypt(token)
                val stream = file.startWrite()
                try {
                    stream.write(bytes)
                    file.finishWrite(stream)
                } catch (error: Exception) {
                    file.failWrite(stream)
                    throw error
                }
            }
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock { file.delete() }
    }

    private fun encrypt(token: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return ByteBuffer.allocate(2 + iv.size + ciphertext.size)
            .put(FORMAT_VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()
    }

    private fun decrypt(bytes: ByteArray): String {
        if (bytes.size < 2 + 12 + 16) throw IllegalArgumentException("Invalid session data")
        val buffer = ByteBuffer.wrap(bytes)
        if (buffer.get() != FORMAT_VERSION) throw IllegalArgumentException("Unknown session format")
        val ivSize = buffer.get().toInt() and 0xff
        if (ivSize !in 12..16 || buffer.remaining() < ivSize + 16) {
            throw IllegalArgumentException("Invalid session data")
        }
        val iv = ByteArray(ivSize).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "mygos_xano_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_VERSION: Byte = 1
    }
}
