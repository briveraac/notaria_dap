package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** UI validation stays local; successful signup is covered with a fake repository. */
class ProfileRegisterTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun guestProfileOpensRegisterAndRejectsInvalidInputWithoutRemoteSignup() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("¡Bienvenido a Mygos!").assertExists()
        compose.onNodeWithText("Contáctanos").performScrollTo().performClick()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_perfil").assertExists()

        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa tu nombre o alias.").assertExists()
        compose.onNodeWithText("Cuenta creada").assertDoesNotExist()

        compose.onNodeWithTag("register_name").performScrollTo().performTextInput("Persona de prueba")
        compose.onNodeWithTag("register_email").performScrollTo().performTextInput("correo-invalido")
        compose.onNodeWithTag("register_password").performScrollTo().performTextInput("Una frase segura 2026!")
        compose.onNodeWithTag("register_confirmation").performScrollTo().performTextInput("No coincide")
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        compose.onNodeWithText("Las contraseñas no coinciden.").assertExists()
        compose.onNodeWithText("Cuenta creada").assertDoesNotExist()

        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("register_name").assertTextContains("Persona de prueba")
        compose.onNodeWithTag("register_password").performScrollTo().assert(
            SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.InputText,
                androidx.compose.ui.text.AnnotatedString("")
            )
        )
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_perfil").assertExists()
    }
}
