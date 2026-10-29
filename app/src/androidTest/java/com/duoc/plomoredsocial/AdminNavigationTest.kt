package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.MygosDatabase
import com.duoc.plomoredsocial.repository.RoomAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class AdminNavigationTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private fun login(email: String, password: String) {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("bottom_navigation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.onNodeWithTag("login_email").performTextInput(email)
        compose.onNodeWithTag("login_password").performScrollTo().performTextInput(password)
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("login_submit").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("nav_perfil").performClick()
    }

    @Test fun adminOpensPanelFromProfileAndReturnsWithoutBreakingNavigation() {
        login("demo@mygos.cl", "Mygos2026!")
        compose.onNodeWithTag("profile_admin").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("admin_panel").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("admin_users_tab").assertIsSelected()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("admin_panel").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("admin_activities_tab").performClick().assertIsSelected()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("nav_perfil").assertIsSelected()
    }

    @Test fun registeredUserHasNoAdminEntryAndKeepsContactAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = MygosDatabase.getInstance(context)
        val account = runBlocking { RoomAuthRepository(db, context.assets).register("Prueba de acceso",
            "admin-nav-${UUID.randomUUID()}@example.com", "Contraseña de navegación larga") }
        try {
            login(account.email, "Contraseña de navegación larga")
            compose.onNodeWithTag("profile_admin").assertDoesNotExist()
            compose.onNodeWithText("Contáctanos").performScrollTo().assertIsDisplayed()
        } finally {
            db.openHelper.writableDatabase.execSQL("DELETE FROM users WHERE id=?", arrayOf(account.id))
        }
    }
}
