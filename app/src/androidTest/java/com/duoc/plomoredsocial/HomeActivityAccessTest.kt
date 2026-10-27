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
    @get:Rule(order = 1) val activities = HomeActivitiesRule()
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun waitForHome() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("home_feed").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag("home_loading").fetchSemanticsNodes().isEmpty()
        }
    }

    // Busca la tarjeta en el listado y pulsa su botón de consulta.
    private fun viewActivity(id: String) {
        compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("view_activity_${activities.id(id)}"))
        compose.onNodeWithTag("view_activity_${activities.id(id)}").performClick()
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

    // Con sesión, consulta la misma actividad guardada que aparece en la sección Actividades.
    @Test fun authenticatedUserCanReadStoredActivitiesInBothSections() {
        viewActivity("park")
        dialogText("Iniciar sesión").performClick()
        compose.onNodeWithTag("login_email").performScrollTo().performTextInput("demo@mygos.cl")
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput("Mygos2026!")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Hola, Alex").fetchSemanticsNodes().isNotEmpty()
        }
        listOf("park", "volley", "games").forEach { id ->
            viewActivity(id)
            compose.onNodeWithTag("home_activity_dialog").assertIsDisplayed()
            compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
            compose.onNode(hasText("Prueba MAIN-004 $id") and hasAnyAncestor(hasTestTag("home_activity_dialog"))).assertIsDisplayed()
            compose.onNode(hasText("Entrada de la plaza") and hasAnyAncestor(hasTestTag("home_activity_dialog"))).assertIsDisplayed()
            compose.onNode(hasText("Personas inscritas: 0") and hasAnyAncestor(hasTestTag("home_activity_dialog"))).assertIsDisplayed()
            compose.onNodeWithText("Cerrar").performClick()
            compose.onNodeWithTag("home_activity_dialog").assertDoesNotExist()
        }
        compose.onNodeWithTag("nav_actividades").performClick()
        compose.onNodeWithText("Prueba MAIN-004 park").performScrollTo().assertIsDisplayed()
    }
}
