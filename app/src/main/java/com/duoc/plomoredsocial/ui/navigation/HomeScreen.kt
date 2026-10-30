package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.ui.components.*

// Inicio orienta; las actividades reales se consultan únicamente en Explorar.
@Composable
internal fun HomeScreen(user: AuthUser?, onLogin: () -> Unit,
    onProfile: () -> Unit, onExplore: () -> Unit) {
    MygosScreen {
        MygosBrand()
        MygosHero {
            Text("UN PLAN EMPIEZA CONTIGO", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Más cerca de tu próximo encuentro", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Descubre actividades, comparte tus intereses y encuentra una buena razón para juntarte.",
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            MygosPrimaryButton("Explorar actividades", onExplore)
        }
        MygosCard {
            Icon(Icons.Filled.AccountCircle, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Text(if (user == null) stringResource(R.string.nav_account) else stringResource(R.string.home_greeting, user.name),
                style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (user == null) R.string.home_login_description else R.string.home_session_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            MygosPrimaryButton(stringResource(if (user == null) R.string.nav_login else R.string.home_profile),
                if (user == null) onLogin else onProfile)
        }
        MygosHeader("Así funciona Mygos", "Explora a tu ritmo. Tú eliges cuándo dar el siguiente paso.")
        MygosStep("01", "Encuentra tu plan", "Busca por título, comuna o punto de encuentro. Consulta los detalles antes de participar.")
        MygosStep("02", "Dale forma a una idea", "Crea una actividad desde Explorar. Define fecha, capacidad y lugar; puedes usar una dirección sin elegir coordenadas.")
        MygosStep("03", "Súmate y organiza", "Inicia sesión para inscribirte, liberar tu cupo o administrar las actividades que creaste desde Perfil.")
        MygosNotice("Las cuentas, las actividades y las inscripciones se guardan solo en este dispositivo. También puedes usar la cuenta de demostración.")
    }
}
