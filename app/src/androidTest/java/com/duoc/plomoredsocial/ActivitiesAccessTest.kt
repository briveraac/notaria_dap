package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

// Protege las acciones que dejaron de tener pestaña propia.
class ActivitiesAccessTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val activities = HomeActivitiesRule()
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun membershipAndCreationFormRemainAccessible() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.onNodeWithTag("login_email").performTextInput("demo@mygos.cl")
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput("Mygos2026!")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Hola, Alex").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_buscar").performClick()
        compose.onNodeWithTag("explore_commune").performScrollTo().performTextInput(" MAI ")
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasTestTag("view_activity_${activities.id("park")}"))
        compose.onNodeWithTag("view_activity_${activities.id("park")}").performClick()
        compose.onNodeWithTag("activity_detail").assertIsDisplayed()
        compose.onNodeWithText("Cómo llegar con Google Maps").assertExists()
        compose.onNodeWithText("Unirme a la actividad").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Salir de la actividad").fetchSemanticsNodes().isNotEmpty()
        }
        val dao = com.duoc.plomoredsocial.model.MygosDatabase.getInstance(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext).activityDao()
        org.junit.Assert.assertTrue(kotlinx.coroutines.runBlocking {
            dao.observeMembers().first().any { it.activityId == activities.id("park") && it.userId == "demo-001" }
        })
        compose.onNodeWithText("Salir de la actividad").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Salir de la actividad").fetchSemanticsNodes().isEmpty()
        }
        org.junit.Assert.assertFalse(kotlinx.coroutines.runBlocking {
            dao.observeMembers().first().any { it.activityId == activities.id("park") && it.userId == "demo-001" }
        })
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("activity_detail").assertIsDisplayed()
        val detailMap = compose.onNode(hasText("Ver mapa") and hasAnyAncestor(hasTestTag("activity_detail")))
        if (BuildConfig.MAPS_CONFIGURED) detailMap.assertIsEnabled() else detailMap.assertIsNotEnabled()
        compose.onNodeWithText("Cerrar").performScrollTo().performClick()
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Ver mapa").performClick()
        compose.onNodeWithTag("activity_map_dialog").assertIsDisplayed()
        if (!BuildConfig.MAPS_CONFIGURED) {
            compose.onNodeWithText("El mapa aún no está disponible. Podrás elegir el punto de encuentro cuando se habilite el servicio.").assertExists()
            compose.onNodeWithTag("activities_map").assertDoesNotExist()
        } else compose.onNodeWithTag("activities_map").assertExists()
        compose.onNodeWithText("Cerrar mapa").performClick()
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Crear actividad").performClick()
        compose.onNodeWithText("Nombre de la actividad").performTextInput("Prueba de formulario")
        compose.onNodeWithText("Comuna", substring = false).performScrollTo().performTextInput("Maipú")
        compose.onNodeWithText("Guardar actividad").performScrollTo().assertIsNotEnabled()
        if (!BuildConfig.MAPS_CONFIGURED) {
            compose.onNodeWithText("Elegir punto en el mapa").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText("Google Maps no está configurado. Puedes guardar con comuna y dirección o referencia, sin coordenadas.").assertExists()
        } else {
            compose.onNodeWithText("Elegir punto en el mapa").performScrollTo().assertIsEnabled()
            compose.onNodeWithText("Elegir punto en el mapa").performClick()
            compose.onNodeWithText("Usar este punto").assertIsNotEnabled()
            compose.onNodeWithText("Cancelar").performClick()
        }
        compose.onNodeWithText("Cancelar").performScrollTo().performClick()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
    }
}
