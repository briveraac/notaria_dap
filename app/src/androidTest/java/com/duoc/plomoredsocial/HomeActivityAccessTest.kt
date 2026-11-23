package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

/** Navigation from the public catalogue remains available without a Xano account. */
class HomeActivityAccessTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun guestCanReachRegisterFromExploreCreateGate() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_buscar").performClick()
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Crear actividad").performClick()
        compose.onNodeWithTag("home_access_dialog").assertIsDisplayed()
        compose.onNode(hasText("Crear cuenta") and hasAnyAncestor(hasTestTag("home_access_dialog"))).performClick()
        compose.onNodeWithTag("register_name").assertExists()
        compose.onNodeWithTag("register_email").assertExists()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
    }

    @Test fun guestCanReturnFromLoginWithoutCreatingASession() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("nav_buscar").performClick()
        compose.onNodeWithTag("explore_feed").performScrollToIndex(0)
        compose.onNodeWithText("Crear actividad").performClick()
        compose.onNode(hasText("Iniciar sesión") and hasAnyAncestor(hasTestTag("home_access_dialog"))).performClick()
        compose.onNodeWithTag("login_email").assertExists()
        pressBack()
        compose.onNodeWithTag("nav_buscar").assertIsSelected()
        compose.onNodeWithTag("home_access_dialog").assertDoesNotExist()
    }
}
