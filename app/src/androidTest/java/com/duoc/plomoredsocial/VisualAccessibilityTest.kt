package com.duoc.plomoredsocial

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.ui.auth.LoginScreen
import com.duoc.plomoredsocial.ui.navigation.*
import com.duoc.plomoredsocial.ui.theme.*
import com.duoc.plomoredsocial.viewmodel.LoginUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

// Solo renderiza estados de interfaz; no inserta cuentas ni actividades en Room.
class VisualAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance(); val second = b.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }

    @Test fun normalTextAndActionLabelsMeetContrastInBothThemes() {
        for (scheme in listOf(MygosLightScheme, MygosDarkScheme)) {
            val pairs = listOf(scheme.onBackground to scheme.background,
                scheme.onSurface to scheme.surface, scheme.onSurfaceVariant to scheme.surface,
                scheme.primary to scheme.background, scheme.onPrimary to scheme.primary,
                scheme.onPrimaryContainer to scheme.primaryContainer,
                scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.error to scheme.surface, scheme.onErrorContainer to scheme.errorContainer,
                scheme.onTertiaryContainer to scheme.tertiaryContainer)
            pairs.forEach { (foreground, background) ->
                assertTrue("Texto con contraste insuficiente: ${contrast(foreground, background)}", contrast(foreground, background) >= 4.5f)
            }
        }
        assertTrue(contrast(MygosOnBlue, MygosBlue) >= 4.5f)
    }

    @Test fun homeFitsSmallWidthAndLargeTextInLightAndDarkWithoutLosingActions() {
        var dark by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                PlomoRedSocialTheme(darkTheme = dark) {
                    Surface(Modifier.requiredSize(320.dp, 600.dp).testTag("visual_viewport")) {
                        HomeScreen(null, {}, {}, { clicks++ })
                    }
                }
            }
        }
        for (mode in listOf(false, true)) {
            compose.runOnIdle { dark = mode }
            val action = compose.onNodeWithText("Explorar actividades").performScrollTo().assertIsDisplayed()
            action.assertHeightIsAtLeast(48.dp)
            val viewport = compose.onNodeWithTag("visual_viewport").getUnclippedBoundsInRoot()
            val bounds = action.getUnclippedBoundsInRoot()
            assertTrue(bounds.left >= viewport.left && bounds.right <= viewport.right)
            action.performClick()
            compose.onNodeWithText("Las cuentas, las actividades y las inscripciones se guardan solo en este dispositivo. También puedes usar la cuenta de demostración.")
                .performScrollTo().assertIsDisplayed()
        }
        assertEquals(2, clicks)
    }

    @Test fun loginRemainsScrollableWithAccessibleActionAtSmallWidth() {
        var dark by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                PlomoRedSocialTheme(darkTheme = dark) {
                    Surface(Modifier.requiredSize(320.dp, 600.dp)) {
                        LoginScreen(LoginUiState(restoringSession = false), {}, {}, {}, {})
                    }
                }
            }
        }
        for (mode in listOf(false, true)) {
            compose.runOnIdle { dark = mode }
            compose.onNodeWithTag("login_email").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("login_submit").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
    }

    @Test fun longProfileTextPreservesContactAndAccountActionsAtSmallWidth() {
        val name = "Nombre de usuario extenso para comprobar que el perfil no recorta información"
        val email = "correo.de.usuario.largo.para.revisar.el.diseno@ejemplo.cl"
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                PlomoRedSocialTheme(darkTheme = true) {
                    Surface(Modifier.requiredSize(320.dp, 600.dp)) {
                        TemporaryScreen(MygosDestination.PROFILE, AuthUser("visual-only", name, email), onNavigate = {})
                    }
                }
            }
        }
        compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(email).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Contáctanos").performScrollTo().assertIsDisplayed()
    }
}
