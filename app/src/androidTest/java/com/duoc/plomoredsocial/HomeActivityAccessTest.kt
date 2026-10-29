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
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_buscar").performClick()
    }

    // Busca la tarjeta en el listado y pulsa su botón de consulta.
    private fun viewActivity(id: String) {
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasTestTag("view_activity_${activities.id(id)}"))
        compose.onNodeWithTag("view_activity_${activities.id(id)}").performClick()
    }

    private fun dialogText(text: String) =
        compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag("home_access_dialog")))

    // Sin sesión se puede cancelar o ir a las pantallas de Login y Registro existentes.
    @Test fun guestCanCancelAndOpenExistingLoginAndRegistrationRoutes() {
        // El detalle se puede consultar sin sesión.
        viewActivity("park")
        compose.onNodeWithTag("activity_detail").assertIsDisplayed()
        compose.onNodeWithText("Iniciar sesión para participar").performScrollTo().performClick()
        compose.onNodeWithTag("home_access_dialog").assertIsDisplayed()
        dialogText("Cancelar").performClick()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()

        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Crear actividad").performClick()
        dialogText("Iniciar sesión").performClick()
        compose.onNodeWithTag("login_email").assertExists()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
        pressBack()

        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Crear actividad").performClick()
        dialogText("Crear cuenta").performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        // Comprueba el formulario real, independientemente de su título.
        compose.onNodeWithTag("register_name").assertExists()
        compose.onNodeWithTag("register_email").assertExists()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
    }

    // Con sesión, consulta la misma actividad guardada que aparece en la sección Actividades.
    @Test fun authenticatedUserCanReadStoredActivitiesInBothSections() {
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Crear actividad").performClick()
        dialogText("Iniciar sesión").performClick()
        compose.onNodeWithTag("login_email").performScrollTo().performTextInput("demo@mygos.cl")
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput("Mygos2026!")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Hola, Alex").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_buscar").performClick()
        listOf("park", "volley", "games").forEach { id ->
            viewActivity(id)
            compose.onNodeWithTag("activity_detail").assertIsDisplayed()
            compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
            compose.onNode(hasText("Prueba MAIN-004 $id") and hasAnyAncestor(hasTestTag("activity_detail"))).assertIsDisplayed()
            compose.onNode(hasText("Entrada de la plaza") and hasAnyAncestor(hasTestTag("activity_detail"))).assertIsDisplayed()
            compose.onNode(hasText("Participantes: 1/10") and hasAnyAncestor(hasTestTag("activity_detail"))).assertIsDisplayed()
            compose.onNodeWithText("Cerrar").performClick()
            compose.onNodeWithTag("activity_detail").assertDoesNotExist()
        }
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Ver mapa").performClick()
        compose.onNodeWithTag("activity_map_dialog").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
    }
}
