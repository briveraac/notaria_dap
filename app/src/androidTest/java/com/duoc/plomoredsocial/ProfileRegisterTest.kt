package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.MygosDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ProfileRegisterTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun profileContactRegistrationAndSessionRecreation() {
        val email = "registro-${UUID.randomUUID()}@example.com"
        val password = "Una frase segura 2026!"
        val database = MygosDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            compose.waitUntil(10000) {
                compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("nav_perfil").performClick()
            compose.onNodeWithText("Próximamente").assertDoesNotExist()
            compose.onNodeWithText("¡Bienvenido a Mygos!").assertExists()
            contactAndReturn()
            compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
            compose.onNodeWithTag("register_submit").performScrollTo().performClick()
            compose.onNodeWithText("Ingresa tu nombre o alias.").assertExists()
            compose.onNodeWithTag("register_name").performScrollTo().performTextInput("Persona de prueba")
            compose.onNodeWithTag("register_email").performScrollTo().performTextInput(email)
            compose.onNodeWithTag("register_password").performScrollTo().performTextInput(password)
            compose.onNodeWithTag("register_confirmation").performScrollTo().performTextInput("No coincide")
            compose.onNodeWithTag("register_submit").performScrollTo().performClick()
            compose.onNodeWithText("Las contraseñas no coinciden.").assertExists()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithTag("register_name").assertTextContains("Persona de prueba")
            compose.onNodeWithTag("register_password").performScrollTo().assert(
                SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.InputText,
                    androidx.compose.ui.text.AnnotatedString("")))
            compose.onNodeWithTag("register_password").performTextInput(password)
            compose.onNodeWithTag("register_confirmation").performScrollTo().performTextInput(password)
            compose.onNodeWithTag("register_submit").performScrollTo().performClick()
            compose.waitUntil(30000) {
                compose.onAllNodesWithText("Cuenta creada").fetchSemanticsNodes().isNotEmpty()
            }
            assertNotNull(runBlocking { database.userDao().findByEmail(email) })
            assertNull(runBlocking {
                com.duoc.plomoredsocial.repository.LocalSessionStore(
                    InstrumentationRegistry.getInstrumentation().targetContext.noBackupFilesDir).readUserId()
            })
            compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
            compose.onNodeWithTag("login_email").assertTextContains(email)
            compose.onNodeWithTag("login_password").performScrollTo().performTextInput(password)
            compose.onNodeWithTag("login_submit").performScrollTo().performClick()
            compose.waitUntil(30000) {
                compose.onAllNodesWithText("Hola, Persona de prueba").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("nav_perfil").performClick()
            compose.onNodeWithText(email).assertExists()
            compose.onNodeWithText("Cuenta local: tus datos se guardan en este dispositivo.").assertExists()
            compose.onNodeWithText("Sesión de demostración: estos datos corresponden a la cuenta de prueba.").assertDoesNotExist()
            contactAndReturn()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(email).assertExists()
            compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
            compose.waitUntil(10000) {
                compose.onAllNodesWithText("¡Bienvenido a Mygos!").fetchSemanticsNodes().isNotEmpty()
            }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("¡Bienvenido a Mygos!").assertExists()
            compose.onNodeWithText(email).assertDoesNotExist()
        } finally {
            database.openHelper.writableDatabase.execSQL("DELETE FROM users WHERE normalizedEmail = ?", arrayOf(email))
        }
    }

    private fun contactAndReturn() {
        compose.onNodeWithText("Contáctanos").performScrollTo().performClick()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_perfil").assertExists()
    }

    @Test fun duplicateDemoEmailIsRejectedWithoutCreatingASession() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("register_name").performTextInput("Otra persona")
        compose.onNodeWithTag("register_email").performScrollTo().performTextInput(" DEMO@MYGOS.CL ")
        compose.onNodeWithTag("register_password").performScrollTo().performTextInput("Una contraseña larga")
        compose.onNodeWithTag("register_confirmation").performScrollTo().performTextInput("Una contraseña larga")
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Este correo ya tiene una cuenta.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Cuenta creada").assertDoesNotExist()
        assertNull(runBlocking {
            com.duoc.plomoredsocial.repository.LocalSessionStore(
                InstrumentationRegistry.getInstrumentation().targetContext.noBackupFilesDir).readUserId()
        })
    }
}
