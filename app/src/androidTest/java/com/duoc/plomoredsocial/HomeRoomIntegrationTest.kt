package com.duoc.plomoredsocial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.ActivityMember
import com.duoc.plomoredsocial.model.MygosDatabase
import com.duoc.plomoredsocial.repository.RoomActivityRepository
import com.duoc.plomoredsocial.ui.navigation.ExploreScreen
import com.duoc.plomoredsocial.ui.theme.PlomoRedSocialTheme
import com.duoc.plomoredsocial.viewmodel.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Usa una base temporal en memoria: comprueba Room sin modificar los datos del teléfono.
class HomeRoomIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: MygosDatabase
    private val store = ViewModelStore()
    private lateinit var home: HomeViewModel

    @Before fun openHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        database = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, MygosDatabase::class.java).build()
        instrumentation.runOnMainSync {
            home = ViewModelProvider(store, HomeViewModel.Factory(
                RoomActivityRepository(database.activityDao())))[HomeViewModel::class.java]
        }
        compose.setContent {
            PlomoRedSocialTheme {
                ExploreScreen(null, home.uiState, onLogin = {}, onRegister = {})
            }
        }
        compose.waitUntil(10000) { !home.uiState.loading }
    }

    @After fun closeDatabase() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        database.close()
    }

    @Test fun emptyDatabaseShowsEmptyStateWithoutInventedActivities() {
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasText(
            "Todavía no hay actividades guardadas en este dispositivo. Puedes crearlas aquí al iniciar sesión."))
        compose.onNodeWithText("Caminata por el parque").assertDoesNotExist()
        compose.onNodeWithText("Todavía no hay actividades guardadas en este dispositivo. Puedes crearlas aquí al iniciar sesión.").assertIsDisplayed()
    }

    @Test fun roomChangesUpdateHomeAndItsRealMemberCount() = runBlocking<Unit> {
        // Un filtro activo también debe reaccionar a una nueva actividad de Room.
        compose.onNodeWithTag("explore_query").performScrollTo().performTextInput("Actividad desde Room")
        compose.onNodeWithTag("explore_commune").performScrollTo().performTextInput("MAI")
        val dao = database.activityDao()
        dao.create(ActivityEntity("room-live", "Actividad desde Room", "Maipú",
            "Entrada de la plaza", "30/10/2026, 18:00", -33.51, -70.76, "organizer"))
        compose.waitUntil(10000) { home.uiState.activities.any { it.id == "room-live" } }
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasTestTag("activity_room-live"))
        compose.onNodeWithTag("activity_room-live").assertIsDisplayed()
        compose.onNodeWithTag("explore_feed").performScrollToNode(hasText("Personas inscritas: 0"))
        dao.join(ActivityMember("room-live", "neighbor"))
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Personas inscritas: 1").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Personas inscritas: 1").assertIsDisplayed()
        dao.leave("room-live", "neighbor")
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Personas inscritas: 0").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Personas inscritas: 0").assertIsDisplayed()
    }
}
