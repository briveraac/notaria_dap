package com.duoc.plomoredsocial.repository

import com.duoc.plomoredsocial.model.UserEntity
import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordRecord(val hash: String, val salt: String,
    val algorithm: String = PasswordDeriver.ALGORITHM, val iterations: Int = 600_000,
    val keyLength: Int = 256, val formatVersion: Int = 1)

// API ligera de BC: mismo PBKDF2-SHA256 desde API 24, sin cambiar proveedores globales.
class PasswordDeriver {
    fun create(password: String): PasswordRecord {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return PasswordRecord(derive(password, salt, 600_000, 256).toHex(), salt.toHex())
    }

    fun verify(password: String, user: UserEntity): Boolean {
        val actual = when (user.algorithm) {
            ALGORITHM -> {
                require(user.formatVersion == 1 && user.iterations in 600_000..2_000_000 && user.keyLength == 256)
                val salt = user.salt.fromHex()
                require(salt.size == 16)
                derive(password, salt, user.iterations, user.keyLength)
            }
            LEGACY_ALGORITHM -> {
                // La excepción antigua solo se admite para cuentas ficticias conocidas.
                require(user.isDemo && user.formatVersion == 0 && user.iterations == 120_000 && user.keyLength == 256)
                val spec = PBEKeySpec(password.toCharArray(), user.salt.toByteArray(Charsets.UTF_8),
                    user.iterations, user.keyLength)
                try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
                finally { spec.clearPassword() }
            }
            else -> error("Formato de credenciales no compatible.")
        }
        val expected = user.passwordHash.fromHex()
        return try { expected.size == 32 && MessageDigest.isEqual(actual, expected) }
        finally { actual.fill(0); expected.fill(0) }
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val chars = password.toCharArray()
        val bytes = try { PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(chars) }
            finally { chars.fill('\u0000') }
        return try {
            val generator = PKCS5S2ParametersGenerator(SHA256Digest())
            generator.init(bytes, salt, iterations)
            (generator.generateDerivedParameters(keyLength) as KeyParameter).key
        } finally { bytes.fill(0) }
    }

    companion object {
        const val ALGORITHM = "PBKDF2-HMAC-SHA256"
        const val LEGACY_ALGORITHM = "PBKDF2-HMAC-SHA1-DEMO"
    }
}

private fun ByteArray.toHex() = joinToString("") { "%02x".format(it.toInt() and 255) }
private fun String.fromHex(): ByteArray {
    require(length % 2 == 0 && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
