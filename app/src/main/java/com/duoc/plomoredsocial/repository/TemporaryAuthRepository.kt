package com.duoc.plomoredsocial.repository

import android.content.res.AssetManager
import com.duoc.plomoredsocial.model.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Datos de demostración empaquetados en assets/temporary_auth/users.json.
// La autenticación local es temporal: no constituye una frontera de seguridad.
class TemporaryAuthRepository(private val assets: AssetManager) : AuthRepository {
    override suspend fun getUserById(id: String): AuthUser? = withContext(Dispatchers.IO) {
        val users = assets.open("temporary_auth/users.json").bufferedReader().use {
            JSONArray(it.readText())
        }
        for (index in 0 until users.length()) {
            val record = users.getJSONObject(index)
            if (record.getString("id") == id) {
                return@withContext AuthUser(id, record.getString("name"), record.getString("email"))
            }
        }
        null
    }

    override suspend fun authenticate(email: String, password: String): AuthUser? =
        withContext(Dispatchers.IO) {
            val users = assets.open("temporary_auth/users.json").bufferedReader().use {
                JSONArray(it.readText())
            }
            for (index in 0 until users.length()) {
                val record = users.getJSONObject(index)
                if (!record.getString("email").equals(email.trim(), ignoreCase = true)) continue
                val spec = PBEKeySpec(
                    password.toCharArray(), record.getString("salt").toByteArray(Charsets.UTF_8),
                    record.getInt("iterations"), 256
                )
                val actual = try {
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
                } finally {
                    spec.clearPassword()
                }
                val expected = record.getString("passwordHash").chunked(2)
                    .map { it.toInt(16).toByte() }.toByteArray()
                return@withContext if (MessageDigest.isEqual(actual, expected)) {
                    AuthUser(record.getString("id"), record.getString("name"), record.getString("email"))
                } else null
            }
            null
        }
}
