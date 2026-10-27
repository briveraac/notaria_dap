package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Comprueba qué ocurre al consultar una actividad con y sin una sesión iniciada.
class HomeActivityAccessTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun waitForHome() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("home_feed").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Busca la tarjeta en el listado y pulsa su botón de consulta.
    private fun viewActivity(id: String) {
        compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("view_activity_$id"))
        compose.onNodeWithTag("view_activity_$id").performClick()
    }

    private fun dialogText(text: String) =
        compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag("home_access_dialog")))

    // Sin sesión se puede cancelar o ir a las pantallas de Login y Registro existentes.
    @Test fun guestCanCancelAndOpenExistingLoginAndRegistrationRoutes() {
        viewActivity("park")
        compose.onNodeWithTag("home_access_dialog").assertIsDisplayed()
        dialogText("Cancelar").performClick()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
        compose.onNodeWithTag("nav_inicio").assertIsSelected()

        viewActivity("park")
        dialogText("Iniciar sesión").performClick()
        compose.onNodeWithTag("login_email").assertExists()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
        pressBack()

        viewActivity("park")
        dialogText("Crear cuenta").performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        // La ruta existente repite el título en la barra superior y en el contenido.
        compose.onAllNodesWithText("Crear cuenta").assertCountEquals(2)
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_inicio").assertIsSelected()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
    }

    // Con una cuenta de demostración, comprueba la consulta de los tres estados y sus cupos.
    @Test fun authenticatedUserCanReadOpenInProgressAndCompleteActivities() {
        viewActivity("park")
        dialogText("Iniciar sesión").performClick()
        compose.onNodeWithTag("login_email").performScrollTo().performTextInput("demo@mygos.cl")
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput("Mygos2026!")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Hola, Alex").fetchSemanticsNodes().isNotEmpty()
        }
        listOf(
            Triple("park", "Abierta", "1/5 participantes"),
            Triple("volley", "En progreso", "6/10 participantes"),
            Triple("games", "Completa", "10/10 participantes")
        ).forEach { (id, status, count) ->
            viewActivity(id)
            compose.onNodeWithTag("home_activity_dialog").assertIsDisplayed()
            compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
            compose.onNode(hasText(status) and hasAnyAncestor(hasTestTag("home_activity_dialog"))).assertIsDisplayed()
            compose.onNode(hasText(count) and hasAnyAncestor(hasTestTag("home_activity_dialog"))).assertIsDisplayed()
            compose.onNodeWithText("Cerrar").performClick()
            compose.onNodeWithTag("home_activity_dialog").assertDoesNotExist()
        }
    }
}
