package com.duoc.plomoredsocial

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.notifications.ActivityCreatedNotifier
import com.duoc.plomoredsocial.ui.activities.*
import com.duoc.plomoredsocial.ui.components.MygosScreen
import com.duoc.plomoredsocial.ui.theme.PlomoRedSocialTheme
import com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ActivityManagementUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: MygosDatabase
    private lateinit var model: ActivitiesViewModel
    private val store = ViewModelStore()
    private val owner = AuthUser("owner", "Organizadora", "owner@example.com")
    private val guest = AuthUser("guest", "Participante", "guest@example.com")

    @Before fun prepare() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        (instrumentation.targetContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, MygosDatabase::class.java).build()
        // La cuenta de esta fixture existe en Room, igual que en el recorrido real.
        runBlocking { db.userDao().insert(UserEntity(owner.id, owner.name, owner.email, owner.email,
            "test-hash", "test-salt", "test-only", 1, 256, 1, false)) }
        instrumentation.runOnMainSync {
            model = ActivitiesViewModel(instrumentation.targetContext.applicationContext as Application, db.activityDao())
            store.put("activities", model)
        }
    }

    @After fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        (InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        db.close()
    }

    private fun event(id: String, organizer: String = owner.id) = ActivityEntity(id, "Título $id", "Maipú",
        "Plaza", "31-12-2099, 18:00", null, null, organizer)

    private fun createWithCapacity(capacity: Int) {
        var dismissed by mutableStateOf(false)
        compose.setContent { PlomoRedSocialTheme {
            if (!dismissed) ActivityCreation(owner, model) { dismissed = true }
        } }
        compose.onNodeWithText("Capacidad máxima: 10 (incluye al organizador)").assertExists()
        compose.onNodeWithTag("create_capacity").performSemanticsAction(SemanticsActions.SetProgress) { it(capacity.toFloat()) }
        compose.onNodeWithText("Capacidad máxima: $capacity (incluye al organizador)").assertExists()
        compose.onNodeWithTag("create_title").performScrollTo().performTextInput("Capacidad $capacity")
        compose.onNodeWithTag("create_commune").performScrollTo().performTextInput("Maipú")
        compose.onNodeWithTag("create_date").performScrollTo().performTextInput("31122099")
        compose.onNodeWithTag("create_time").performScrollTo().performTextInput("1800")
        compose.onNodeWithTag("create_place").performScrollTo().performTextInput("Plaza")
        compose.onNodeWithText("Guardar actividad").performScrollTo().performClick()
        compose.waitUntil(10000) { dismissed }
        val saved = runBlocking { db.activityDao().observeActivities().first().single() }
        assertEquals(capacity, saved.capacity)
        assertEquals(owner.id, saved.organizerId)
        assertNull(saved.latitude); assertNull(saved.longitude)
        val manager = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(1, manager.activeNotifications.count { it.id == saved.id.hashCode() })
    }

    @Test fun createCapacityOne() = createWithCapacity(1)
    @Test fun createCapacityFive() = createWithCapacity(5)
    @Test fun createCapacityTen() = createWithCapacity(10)

    @Test fun repeatedSaveCallCreatesOneActivityAndOneNotification() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = event("double-save")
        val callbacks = AtomicInteger(0)
        val done = CountDownLatch(1)
        instrumentation.runOnMainSync {
            model.create(activity) {
                ActivityCreatedNotifier(context).show(activity)
                callbacks.incrementAndGet()
                done.countDown()
            }
            model.create(activity) { callbacks.incrementAndGet() }
        }
        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertEquals(1, callbacks.get())
        assertNotNull(runBlocking { db.activityDao().find(activity.id) })
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(1, manager.activeNotifications.count { it.id == activity.id.hashCode() })
    }

    @Test fun deleteRequiresConfirmationCancelPreservesDataAndThirdPartyHasNoButton() {
        val activity = event("delete")
        runBlocking {
            db.activityDao().create(activity)
            db.activityDao().join(ActivityMember(activity.id, guest.id))
        }
        var user by mutableStateOf(guest)
        var dismissed by mutableStateOf(false)
        compose.setContent { PlomoRedSocialTheme {
            model.activities.firstOrNull { it.id == activity.id }?.let {
                ActivityDetail(it, user, model, { dismissed = true }, {}, {})
            }
        } }
        compose.waitUntil(10000) { model.activities.isNotEmpty() }
        compose.onNodeWithTag("delete_activity").assertDoesNotExist()
        compose.runOnIdle { user = owner }
        compose.onNodeWithTag("delete_activity").performScrollTo().performClick()
        compose.onNodeWithText("Eliminar definitivamente").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        assertNotNull(runBlocking { db.activityDao().find(activity.id) })
        assertEquals(1, runBlocking { db.activityDao().observeMembers().first().size })
        compose.onNodeWithTag("delete_activity").performClick()
        compose.onNodeWithText("Eliminar definitivamente").performClick()
        compose.waitUntil(10000) { dismissed && model.activities.isEmpty() && model.members.isEmpty() }
        assertNull(runBlocking { db.activityDao().find(activity.id) })
    }

    @Test fun profileChangesImmediatelyAndNeverShowsPreviousAccountsLists() {
        val owned = event("created")
        val other = event("joined", guest.id)
        runBlocking { db.activityDao().create(owned); db.activityDao().create(other) }
        var user by mutableStateOf(owner)
        compose.setContent { PlomoRedSocialTheme { MygosScreen {
            key(user.id) { ProfileActivities(user, model) }
        } } }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("profile_created_created").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Aún no participas en actividades.").assertExists()
        runBlocking { db.activityDao().join(ActivityMember(other.id, owner.id)) }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("profile_participating_joined").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("profile_participating_created").assertDoesNotExist()
        compose.onNodeWithTag("profile_participating_joined").performScrollTo().performClick()
        compose.onNodeWithText("Salir de la actividad").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("activity_detail").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Aún no participas en actividades.").assertExists()
        compose.runOnIdle { user = guest }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("profile_created_joined").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("profile_created_created").assertDoesNotExist()
        compose.onNodeWithTag("profile_participating_joined").assertDoesNotExist()
        compose.runOnIdle { user = AuthUser("empty-account", "Vacía", "empty@example.com") }
        compose.onNodeWithText("Aún no has creado actividades.").assertExists()
        compose.onNodeWithText("Aún no participas en actividades.").assertExists()
        compose.onNodeWithTag("profile_created_joined").assertDoesNotExist()
    }
}
