package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.model.RegisterValidator as V
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class RegisterValidatorTest {
    @Test fun validatesNameEmailPasswordAndConfirmation() {
        assertNotNull(V.nameError(" "))
        assertNotNull(V.nameError("a".repeat(81)))
        assertNull(V.nameError(" Vecina "))
        assertNotNull(V.emailError("no-es-correo"))
        assertNull(V.emailError(" Vecina@MYGOS.cl "))
        assertNotNull(V.passwordError("corta"))
        assertNotNull(V.passwordError(" ".repeat(12)))
        assertNotNull(V.passwordError("a".repeat(129)))
        assertNull(V.passwordError("Una frase con espacios"))
        assertNotNull(V.confirmationError("Una frase larga", "Una frase larga "))
        assertNull(V.confirmationError(" Una frase larga ", " Una frase larga "))
    }

    @Test fun emailNormalizationDoesNotDependOnLanguageOrRemoveAliases() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("vecina+planes@mygos.cl", V.normalizeEmail(" VECINA+planes@MYGOS.CL "))
            assertEquals("i@example.com", V.normalizeEmail("I@EXAMPLE.COM"))
        } finally { Locale.setDefault(previous) }
    }
}
