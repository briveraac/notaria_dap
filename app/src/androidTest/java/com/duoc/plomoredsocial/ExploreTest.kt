package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

// Comprueba filtros reales, restauración y acceso a la pantalla funcional.
class ExploreTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val activities = HomeActivitiesRule()
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun combinedSearchEmptyResultsAndFiltersSurviveRecreation() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Explorar actividades").performScrollTo().performClick()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
        compose.onNodeWithTag("explore_query").performScrollTo().performTextInput("  PARK  ")
        compose.onNodeWithTag("explore_commune").performScrollTo().performTextInput("  MAI  ")
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasTestTag("activity_${activities.id("park")}"))
        compose.onAllNodesWithTag("activity_${activities.id("park")}").assertCountEquals(1)
        compose.onNodeWithTag("activity_${activities.id("park")}").assertIsDisplayed()
        compose.onNodeWithTag("activity_${activities.id("volley")}").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("explore_query").performScrollTo().assertTextContains("  PARK  ")
        compose.onNodeWithTag("explore_commune").performScrollTo().assertTextContains("  MAI  ")
        compose.onNodeWithTag("explore_commune").performTextReplacement("Providencia")
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasText("No encontramos actividades con esos filtros."))
        compose.onNodeWithText("No encontramos actividades con esos filtros.").assertIsDisplayed()
        compose.onNodeWithTag("explore_clear").performScrollTo().performClick()
        compose.onNodeWithTag("explore_query").assertTextContains("")
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasTestTag("activity_${activities.id("volley")}"))
        compose.onNodeWithTag("activity_${activities.id("volley")}").assertIsDisplayed()
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onAllNodesWithTag("explore_query").assertCountEquals(1)
        compose.onAllNodesWithTag("explore_commune").assertCountEquals(1)
        compose.onNodeWithTag("activities_commune").assertDoesNotExist()
        compose.onNodeWithText("Actividades y mapa").assertDoesNotExist()
        compose.onNodeWithText("Ver mapa").performClick()
        compose.onNodeWithText("Cerrar mapa").performClick()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
        compose.onNodeWithText("Ver mapa").performClick()
        pressBack()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
    }
}
