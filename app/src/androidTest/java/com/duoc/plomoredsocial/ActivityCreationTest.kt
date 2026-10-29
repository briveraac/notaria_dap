package com.duoc.plomoredsocial

import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.MygosDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.*
import java.util.UUID

// Recorrido real de creación sin Maps, borrador recuperable y selectores nativos.
@OptIn(ExperimentalTestApi::class)
class ActivityCreationTest {
    @get:Rule(order = 0) val session = IsolatedSessionRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun openForm() {
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
        compose.onNodeWithText("Crear actividad").performClick()
    }

    @Test fun manualLocationDescriptionAndNumericFormattingSurviveRecreationAndSave() {
        // La creación manual debe funcionar tanto con Maps configurado como sin clave.
        val title = "Creación UI " + UUID.randomUUID()
        val description = "d".repeat(2000)
        val database = MygosDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            compose.onNodeWithTag("create_title").performTextInput(title)
            compose.onNodeWithTag("create_commune").performScrollTo().performTextInput("Maipú")
            compose.onNodeWithTag("create_date").performScrollTo().performTextInput("31122099")
            compose.onNodeWithTag("create_date").assertTextContains("31-12-2099")
            compose.onNodeWithTag("create_time").performScrollTo().performTextInput("1920")
            compose.onNodeWithTag("create_time").assertTextContains("19:20")
            compose.onNodeWithTag("create_description").performScrollTo().performTextInput(description + "x")
            compose.onNodeWithTag("create_description").assertTextContains(description)
            compose.onNodeWithText("2000 / 2000 caracteres").assertExists()
            compose.onNodeWithTag("create_place").performScrollTo().performTextInput("Entrada de la plaza")
            compose.activityRule.scenario.recreate()
            compose.onNodeWithTag("create_date").performScrollTo().assertTextContains("31-12-2099")
            compose.onNodeWithTag("create_time").performScrollTo().assertTextContains("19:20")
            compose.onNodeWithTag("create_description").performScrollTo().assertTextContains(description)
            compose.onNodeWithText("Guardar actividad").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10000) {
                compose.onAllNodesWithTag("activity_form").fetchSemanticsNodes().isEmpty()
            }
            val saved = runBlocking { database.activityDao().observeActivities().first().first { it.title == title } }
            Assert.assertNull(saved.latitude)
            Assert.assertNull(saved.longitude)
            Assert.assertEquals(description, saved.description)
            Assert.assertEquals("31-12-2099, 19:20", saved.schedule)
            compose.onNodeWithTag("explore_feed").performScrollToNode(hasTestTag("view_activity_${saved.id}"))
            compose.onNodeWithTag("view_activity_${saved.id}").performClick()
            compose.onNodeWithTag("activity_description").assertTextContains(description)
            compose.onNode(hasText("Ver mapa") and hasAnyAncestor(hasTestTag("activity_detail")))
                .performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText("Cómo llegar con Google Maps").performScrollTo().assertIsNotEnabled()
        } finally {
            database.openHelper.writableDatabase.execSQL("DELETE FROM activities WHERE title = ?", arrayOf(title))
        }
    }

    @Test fun nativeDateAnd24HourPickersPopulateTheFields() {
        compose.onNodeWithTag("create_date").performScrollTo()
        compose.onNodeWithContentDescription("Elegir fecha").performClick()
        onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(DatePicker::class.java)
            override fun getDescription() = "Elegir fecha futura"
            override fun perform(uiController: UiController, view: View) {
                (view as DatePicker).updateDate(2099, 11, 31)
                uiController.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).perform(click())
        compose.onNodeWithTag("create_date").assertTextContains("31-12-2099")
        compose.onNodeWithTag("create_time").performScrollTo()
        compose.onNodeWithContentDescription("Elegir hora").performClick()
        onView(isAssignableFrom(TimePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(TimePicker::class.java)
            override fun getDescription() = "Elegir hora en formato de 24 horas"
            override fun perform(uiController: UiController, view: View) {
                (view as TimePicker).apply { hour = 19; minute = 20 }
                uiController.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).perform(click())
        compose.onNodeWithTag("create_time").assertTextContains("19:20")
    }

    private fun assertCursor(tag: String, offset: Int) {
        Assert.assertEquals(TextRange(offset), compose.onNodeWithTag(tag).fetchSemanticsNode()
            .config[SemanticsProperties.TextSelectionRange])
    }

    @Test fun eachTypedDigitKeepsItsCursorAndIntermediateEditsRemainInPlace() {
        val date = compose.onNodeWithTag("create_date").performScrollTo()
        "30122026".forEachIndexed { index, digit ->
            date.performTextInput(digit.toString())
            assertCursor("create_date", index + 1)
        }
        date.assertTextContains("30-12-2026")
        date.performTextInputSelection(TextRange(3, 4))
        date.performTextInput("1")
        date.assertTextContains("30-11-2026")
        assertCursor("create_date", 4)
        val time = compose.onNodeWithTag("create_time").performScrollTo()
        "1920".forEachIndexed { index, digit ->
            time.performTextInput(digit.toString())
            assertCursor("create_time", index + 1)
        }
        time.assertTextContains("19:20")
        time.performTextInputSelection(TextRange(1, 2))
        time.performTextInput("8")
        time.assertTextContains("18:20")
        assertCursor("create_time", 2)
        compose.activityRule.scenario.recreate()
        assertCursor("create_time", 2)
    }

    @Test fun deletingNextToSeparatorsAndPastingFormattedValuesPreservesSelection() {
        val date = compose.onNodeWithTag("create_date").performScrollTo()
        date.performTextInput("30-12-2026")
        date.assertTextContains("30-12-2026")
        assertCursor("create_date", 8)
        // Posiciones visuales antes y después del primer guion.
        for (visualOffset in listOf(2, 3)) {
            date.performTextReplacement("30-12-2026")
            date.performTextInputSelection(TextRange(visualOffset), relativeToOriginalText = false)
            date.performKeyInput { pressKey(Key.Backspace) }
            date.assertTextContains("31-22-026")
            assertCursor("create_date", 1)
        }
        date.performTextReplacement("30122026")
        date.performTextInputSelection(TextRange(2))
        date.performKeyInput { pressKey(Key.Delete) }
        date.assertTextContains("30-22-026")
        assertCursor("create_date", 2)
        val time = compose.onNodeWithTag("create_time").performScrollTo()
        for (visualOffset in listOf(2, 3)) {
            time.performTextReplacement("19:20")
            time.performTextInputSelection(TextRange(visualOffset), relativeToOriginalText = false)
            time.performKeyInput { pressKey(Key.Backspace) }
            time.assertTextContains("12:0")
            assertCursor("create_time", 1)
        }
        time.performTextReplacement("19::20")
        time.assertTextContains("19:20")
        assertCursor("create_time", 4)
        time.performTextInput("99")
        assertCursor("create_time", 4)
    }

    @Test fun incompleteValuesDoNotShowErrorsUntilTheFieldIsFinished() {
        val date = compose.onNodeWithTag("create_date").performScrollTo()
        date.performTextInput("30")
        compose.onNodeWithText("Ingresa una fecha válida en formato DD-MM-AAAA.").assertDoesNotExist()
        compose.onNodeWithTag("create_time").performScrollTo().performClick()
        compose.onAllNodesWithText("Ingresa una fecha válida en formato DD-MM-AAAA.")
            .onFirst().assertExists()
        compose.onNodeWithTag("create_time").performTextInput("19")
        compose.onNodeWithText("Ingresa una hora válida de 00:00 a 23:59.").assertDoesNotExist()
        compose.onNodeWithTag("create_description").performScrollTo().performClick()
        compose.onAllNodesWithText("Ingresa una hora válida de 00:00 a 23:59.")
            .onFirst().assertExists()
    }
}
