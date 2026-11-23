package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

/** Guest access to shared activities; no account or activity is written to Xano. */
class ActivitiesAccessTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun guestCanFilterExploreAndReachLoginFromCreateGate() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_buscar").performClick()
        compose.onNodeWithTag("explore_commune").performScrollTo().performTextInput("Santiago")
        compose.onNodeWithTag("explore_commune").assertTextContains("Santiago")
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Crear actividad").performClick()
        compose.onNodeWithTag("home_access_dialog").assertIsDisplayed()
        compose.onNode(hasText("Cancelar") and hasAnyAncestor(hasTestTag("home_access_dialog"))).performClick()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()

        compose.onNodeWithText("Crear actividad").performClick()
        compose.onNode(hasText("Iniciar sesión") and hasAnyAncestor(hasTestTag("home_access_dialog"))).performClick()
        compose.onNodeWithTag("login_email").assertExists()
        pressBack()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
    }
}
