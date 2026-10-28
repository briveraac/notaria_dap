package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class ProfileRegisterTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun profileContactRegistrationAndSessionRecreation() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("Próximamente").assertDoesNotExist()
        compose.onNodeWithText("¡Bienvenido a Mygos!").assertExists()
        contactAndReturn()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Validar demostración").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa tu nombre o alias.").assertExists()
        compose.onNodeWithTag("register_name").performScrollTo().performTextInput("Persona de prueba")
        compose.onNodeWithTag("register_email").performScrollTo().performTextInput("persona@example.com")
        compose.onNodeWithText("Validar demostración").performScrollTo().performClick()
        compose.onNodeWithText("Formulario válido. La demostración terminó: no se creó una cuenta ni se inició sesión.").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("register_name").assertTextContains("Persona de prueba")
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.onNodeWithTag("login_email").performScrollTo().performTextInput("demo@mygos.cl")
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput("Mygos2026!")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Hola, Alex").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("demo@mygos.cl").assertExists()
        contactAndReturn()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("demo@mygos.cl").assertExists()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("¡Bienvenido a Mygos!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("¡Bienvenido a Mygos!").assertExists()
        compose.onNodeWithText("demo@mygos.cl").assertDoesNotExist()
    }

    private fun contactAndReturn() {
        compose.onNodeWithText("Contáctanos").performScrollTo().performClick()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_perfil").assertExists()
    }
}
