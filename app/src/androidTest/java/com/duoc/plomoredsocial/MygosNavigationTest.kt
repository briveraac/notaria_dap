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

// Prueba las rutas reales de MAIN-006 sin sustituir Room ni guardar consultas de prueba.
class MygosNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomSectionsSelectAndRepeatedTapsDoNotDuplicateTheRoute() {
        compose.onNodeWithTag("nav_inicio").assertIsSelected()
        listOf("buscar", "actividades", "perfil", "inicio").forEach { route ->
            compose.onNodeWithTag("nav_$route").performClick().assertIsSelected()
        }
        repeat(4) { compose.onNodeWithTag("nav_buscar").performClick() }
        pressBack()
        compose.onNodeWithTag("nav_inicio").assertIsSelected()
    }

    @Test
    fun profileOpensContactAndReturningPreservesTheForm() {
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithText("Contáctanos").performClick()
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
        compose.onNodeWithTag("nav_actividades").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("nav_actividades").assertIsSelected()
    }
}
