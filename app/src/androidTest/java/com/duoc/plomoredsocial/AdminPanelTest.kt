package com.duoc.plomoredsocial

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.repository.*
import com.duoc.plomoredsocial.ui.admin.*
import com.duoc.plomoredsocial.ui.theme.PlomoRedSocialTheme
import com.duoc.plomoredsocial.viewmodel.AdminViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class AdminPanelTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: MygosDatabase
    private lateinit var admin: AuthUser
    private lateinit var normal: AuthUser
    private lateinit var session: AdminTestSession
    private lateinit var model: AdminViewModel
    private val store = ViewModelStore()

    @Before fun prepare() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, MygosDatabase::class.java).build()
        val auth = RoomAuthRepository(db, instrumentation.targetContext.assets)
        admin = requireNotNull(auth.getUserById(PRIMARY_DEMO_ID))
        normal = auth.register("Participante", "panel@example.com", "Contraseña de prueba larga")
        session = AdminTestSession(admin.id)
        instrumentation.runOnMainSync {
            model = AdminViewModel(AdminRepository(db, session)); store.put("admin", model)
        }
    }
    @After fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        db.close()
    }
    private fun open() {
        compose.setContent { PlomoRedSocialTheme { AdminScreen(admin, model) } }
        compose.waitUntil(10000) { model.snapshot != null }
    }

    @Test fun directRouteRejectsUserAndLoggedOutState() {
        var user by mutableStateOf<AuthUser?>(normal)
        compose.setContent { PlomoRedSocialTheme { AdminRoute(user) } }
        compose.onNodeWithText("Acceso restringido a administradores.").assertIsDisplayed()
        compose.onNodeWithTag("admin_panel").assertDoesNotExist()
        compose.runOnIdle { user = null }
        compose.onNodeWithTag("admin_panel").assertDoesNotExist()
    }

    @Test fun userCannotReadPanelEvenWithForgedAdminUiState() {
        session.id = normal.id
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            store.clear()
            model = AdminViewModel(AdminRepository(db, session)); store.put("denied", model)
        }
        compose.setContent { PlomoRedSocialTheme { AdminScreen(admin, model) } }
        compose.waitUntil(10000) { model.denied }
        compose.onNodeWithText("Acceso restringido a administradores.").assertIsDisplayed()
        compose.onNodeWithTag("admin_panel").assertDoesNotExist()
    }

    @Test fun editsValidateAndNoCredentialsAreDisplayed() {
        open()
        compose.onNodeWithTag("admin_panel").performScrollToNode(hasTestTag("admin_edit_${normal.id}"))
        compose.onNodeWithTag("admin_edit_${normal.id}").performClick()
        compose.onNodeWithTag("admin_email").performTextReplacement("invalido")
        compose.onNodeWithTag("admin_save").assertIsNotEnabled()
        compose.onNodeWithTag("admin_name").performTextReplacement("Nombre actualizado")
        compose.onNodeWithTag("admin_email").performTextReplacement(" EDITADO@example.com ")
        compose.onNodeWithTag("admin_save").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("admin_editor").fetchSemanticsNodes().isEmpty() }
        assertEquals("editado@example.com", runBlocking { db.userDao().findById(normal.id)?.normalizedEmail })
        val record = requireNotNull(runBlocking { db.userDao().findById(normal.id) })
        compose.onNodeWithText(record.passwordHash).assertDoesNotExist()
        compose.onNodeWithText(record.salt).assertDoesNotExist()
        compose.onNodeWithTag("admin_delete_user_${admin.id}").assertDoesNotExist()
    }

    @Test fun organizerBlockCancelAndIndividualActivityRemovalUpdateLists() {
        val activity = ActivityEntity("owned", "Actividad a resolver", "Maipú", "Plaza",
            "31-12-2099, 18:00", null, null, normal.id)
        runBlocking {
            db.activityDao().create(activity)
            db.activityDao().join(ActivityMember(activity.id, admin.id))
        }
        open()
        compose.onNodeWithTag("admin_panel").performScrollToNode(hasTestTag("admin_delete_user_${normal.id}"))
        compose.onNodeWithTag("admin_delete_user_${normal.id}").performClick()
        compose.onNodeWithText("Esta cuenta organiza 1 actividad(es). Debes resolverlas primero en Actividades.").assertExists()
        compose.onNodeWithText("Eliminar definitivamente").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithTag("admin_panel").performScrollToIndex(0)
        compose.onNodeWithTag("admin_activities_tab").performClick()
        compose.onNodeWithTag("admin_delete_activity_owned").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertNotNull(runBlocking { db.activityDao().find("owned") })
        compose.onNodeWithTag("admin_delete_activity_owned").performClick()
        compose.onNodeWithText("Eliminar definitivamente").performClick()
        compose.waitUntil(10000) { model.snapshot?.activities?.isEmpty() == true }
        assertTrue(runBlocking { db.activityDao().observeMembers().first().isEmpty() })
        compose.onNodeWithTag("admin_users_tab").performClick()
        compose.onNodeWithTag("admin_panel").performScrollToNode(hasTestTag("admin_delete_user_${normal.id}"))
        compose.onNodeWithTag("admin_delete_user_${normal.id}").performClick()
        compose.onNodeWithText("Eliminar definitivamente").performClick()
        compose.waitUntil(10000) { model.snapshot?.users?.none { it.id == normal.id } == true }
        assertNull(runBlocking { db.userDao().findById(normal.id) })
    }
}
