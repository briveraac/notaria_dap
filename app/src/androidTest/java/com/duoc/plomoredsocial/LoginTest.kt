package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.repository.TemporaryAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class LoginTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun waitForSessionRestoration() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun temporaryCredentialsAuthenticateAndRejectWrongPasswords() = runBlocking {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val repository = TemporaryAuthRepository(assets)
        assertEquals("Alex", repository.authenticate(" Demo@MYGOS.cl ", "Mygos2026!")?.name)
        assertNull(repository.authenticate("demo@mygos.cl", "incorrecta"))
        assertNull(repository.authenticate("desconocido@mygos.cl", "Mygos2026!"))
        assertNull(repository.authenticate("demo@mygos.cl", " Mygos2026! "))
    }

    @Test fun homeLoginValidatesAuthenticatesAndLogsOut() {
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa tu correo electrónico.").assertExists()
        compose.onNodeWithText("Ingresa tu contraseña.").assertExists()
        compose.onNodeWithTag("login_email").performScrollTo().performTextInput("demo@mygos.cl")
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput("incorrecta")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("El correo o la contraseña no son correctos.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput("Mygos2026!")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Hola, Alex").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Hola, Alex").assertExists()
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("demo@mygos.cl").assertExists()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Iniciar sesión").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Iniciar sesión").assertExists()
        compose.onNodeWithTag("nav_inicio").performClick()
        compose.onNodeWithText("Iniciar sesión").assertExists()
    }
}
