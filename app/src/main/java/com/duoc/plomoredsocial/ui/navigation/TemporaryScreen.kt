package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R

// Solo cubre destinos sin pantalla propia. Reemplazar cada destino al integrar su tarea.
@Composable
internal fun TemporaryScreen(
    destination: MygosDestination,
    onNavigate: (MygosDestination) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(destination.title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.nav_temporary))
        if (destination == MygosDestination.PROFILE) {
            TextButton(onClick = { onNavigate(MygosDestination.CONTACT) }) {
                Text(stringResource(R.string.contact_title))
            }
            // Accesos temporales para integrar y probar las futuras pantallas de acceso.
            TextButton(onClick = { onNavigate(MygosDestination.LOGIN) }) {
                Text(stringResource(R.string.nav_login))
            }
            TextButton(onClick = { onNavigate(MygosDestination.REGISTER) }) {
                Text(stringResource(R.string.nav_register))
            }
        }
        if (destination == MygosDestination.LOGIN) {
            TextButton(onClick = { onNavigate(MygosDestination.REGISTER) }) {
                Text(stringResource(R.string.nav_register))
            }
        }
    }
}
