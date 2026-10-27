package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

// Comprueba que se llegue al final del listado y se recuerde la posición al volver.
class HomeScrollTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val activities = HomeActivitiesRule()
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun scrollReachesLastActivityAndSurvivesNavigationAndRecreation() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("home_feed").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag("home_loading").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("activity_${activities.id("park")}"))
        compose.onNodeWithTag("activity_${activities.id("park")}").assertIsDisplayed()
        compose.onNodeWithTag("home_feed").performScrollToNode(hasTestTag("activity_${activities.id("stretch")}"))
        compose.onNodeWithTag("activity_${activities.id("stretch")}").assertIsDisplayed()
        compose.onNodeWithTag("nav_perfil").performClick()
        compose.onNodeWithTag("nav_inicio").performClick()
        compose.onNodeWithTag("activity_${activities.id("stretch")}").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("home_feed").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag("home_loading").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("activity_${activities.id("stretch")}").assertIsDisplayed()
        compose.onNodeWithTag("home_feed").performScrollToIndex(0)
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.onNodeWithTag("login_email").assertExists()
    }
}
