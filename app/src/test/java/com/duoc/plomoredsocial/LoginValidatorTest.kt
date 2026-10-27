package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.model.LoginError
import com.duoc.plomoredsocial.model.LoginValidator
import org.junit.Assert.*
import org.junit.Test

class LoginValidatorTest {
    @Test fun requiresAnEmail() {
        assertEquals(LoginError.EMAIL_REQUIRED, LoginValidator.emailError("   "))
    }

    @Test fun rejectsMalformedEmails() {
        listOf("demo", "demo@", "@mygos.cl", "demo @mygos.cl", "demo@mygos").forEach {
            assertEquals(LoginError.EMAIL_FORMAT, LoginValidator.emailError(it))
        }
    }

    @Test fun acceptsOuterWhitespaceAndMixedCase() {
        assertNull(LoginValidator.emailError(" Demo@Mygos.cl "))
    }
}
