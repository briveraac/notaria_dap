package com.duoc.plomoredsocial

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import androidx.compose.ui.test.onAllNodesWithTag

// Prueba las rutas reales de MAIN-006 sin sustituir Room ni guardar consultas de prueba.
class MygosNavigationTest {
    @get:Rule(order = 0)
    val session = IsolatedSessionRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before fun waitForSessionRestoration() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun bottomSectionsSelectAndRepeatedTapsDoNotDuplicateTheRoute() {
        compose.onNodeWithTag("nav_inicio").assertIsSelected()
        listOf("buscar", "perfil", "inicio").forEach { route ->
            compose.onNodeWithTag("nav_$route").performClick().assertIsSelected()
        }
        repeat(4) { compose.onNodeWithTag("nav_buscar").performClick() }
        pressBack()
        compose.onNodeWithTag("nav_inicio").assertIsSelected()
    }

    @Test
    fun profileOpensContactAndReturningPreservesTheForm() {
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("Contáctanos").performScrollTo().performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        compose.onNodeWithText("Nombre o alias").performTextInput("AliasNavegacion")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_perfil").assertIsSelected()
        compose.onNodeWithText("Contáctanos").performClick()
        compose.onNodeWithText("AliasNavegacion").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("nav_perfil").assertIsSelected()
    }

    @Test
    fun loginAndRegistrationHideTheBarAndReturnToProfile() {
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        pressBack()
        compose.onNodeWithTag("nav_perfil").assertIsSelected()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_perfil").assertIsSelected()
    }

    @Test
    fun selectedSectionSurvivesActivityRecreation() {
        compose.onNodeWithTag("nav_actividades").assertDoesNotExist()
        compose.onNodeWithTag("nav_buscar").performClick()
        compose.onNodeWithText("Ver mapa").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Cerrar mapa").performClick()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
        compose.onNodeWithText("Ver mapa").performClick()
        pressBack()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
    }
}
