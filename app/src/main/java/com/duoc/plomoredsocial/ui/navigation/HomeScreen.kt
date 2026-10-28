package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.ui.components.*

// Inicio presenta Mygos; el catálogo vive únicamente en Explorar.
@Composable
internal fun HomeScreen(user: AuthUser?, onLogin: () -> Unit,
    onProfile: () -> Unit, onExplore: () -> Unit) {
    MygosScreen {
        Text("Mygos", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Surface(shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Place, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("Los mejores planes empiezan con un encuentro",
                    style = MaterialTheme.typography.headlineLarge)
                Text("Mygos te ayuda a descubrir actividades y conectar con personas para compartir tus intereses.")
                MygosPrimaryButton("Explorar actividades", onExplore)
            }
        }
        // El saludo usa exclusivamente los datos de la sesión.
        MygosCard {
            Icon(Icons.Filled.AccountCircle, null, Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(if (user == null) stringResource(R.string.nav_account)
                else stringResource(R.string.home_greeting, user.name),
                style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (user == null) R.string.home_login_description
                else R.string.home_session_description))
            MygosPrimaryButton(stringResource(if (user == null) R.string.nav_login else R.string.home_profile),
                if (user == null) onLogin else onProfile)
        }
        MygosCard {
            Text("Tu próximo plan, paso a paso", style = MaterialTheme.typography.titleLarge)
            Text("1. Explora actividades\nBusca un título, una comuna o un punto de encuentro.")
            Text("2. Conoce el lugar\nAbre el detalle de una actividad para consultar el mapa y las indicaciones.")
            Text("3. Participa\nInicia sesión para crear actividades desde Explorar o inscribirte y retirarte desde su detalle.")
        }
        MygosNotice("Las cuentas, las actividades y las inscripciones se guardan solo en este dispositivo. También puedes usar la cuenta de demostración.")
    }
}
