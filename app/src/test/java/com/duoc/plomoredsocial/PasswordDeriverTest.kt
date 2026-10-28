package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.model.UserEntity
import com.duoc.plomoredsocial.repository.PasswordDeriver
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordDeriverTest {
    @Test fun unsupportedFormatsAndLegacyForLocalUsersAreRejected() {
        val user = UserEntity("a", "Vecina", "v@example.com", "v@example.com", "0".repeat(64),
            "0".repeat(32), PasswordDeriver.ALGORITHM, 600_000, 256, 1, false)
        val deriver = PasswordDeriver()
        assertTrue(runCatching { deriver.verify("Una frase larga", user.copy(iterations = 1)) }.isFailure)
        assertTrue(runCatching { deriver.verify("Una frase larga", user.copy(iterations = Int.MAX_VALUE)) }.isFailure)
        assertTrue(runCatching { deriver.verify("Una frase larga", user.copy(formatVersion = 9)) }.isFailure)
        assertTrue(runCatching { deriver.verify("Una frase larga", user.copy(algorithm = PasswordDeriver.LEGACY_ALGORITHM)) }.isFailure)
    }

    @Test fun randomSaltsAndHashMatchIndependentJcaImplementation() {
        val deriver = PasswordDeriver()
        val password = " Una contraseña larga ñ "
        val first = deriver.create(password)
        val second = deriver.create(password)
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
        assertEquals(32, first.salt.length)
        assertEquals(64, first.hash.length)
        assertEquals(600_000, first.iterations)
        val salt = first.salt.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(password.toCharArray(), salt, first.iterations, 256)
        val expected = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
            finally { spec.clearPassword() }
        val actual = first.hash.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertArrayEquals(expected, actual)
        val user = UserEntity("a", "Vecina", "v@example.com", "v@example.com", first.hash,
            first.salt, first.algorithm, first.iterations, first.keyLength, first.formatVersion, false)
        assertTrue(deriver.verify(password, user))
        assertFalse(deriver.verify(password.trim(), user))
        assertFalse(deriver.verify("incorrecta", user))
    }
}
