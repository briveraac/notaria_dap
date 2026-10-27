package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.model.AuthUser

// Solo cubre destinos sin pantalla propia. No inventa perfiles ni actividades.
@Composable
internal fun TemporaryScreen(
    destination: MygosDestination,
    user: AuthUser? = null,
    onLogout: () -> Unit = {},
    onNavigate: (MygosDestination) -> Unit
) {
    val icon = when (destination) {
        MygosDestination.HOME -> Icons.Filled.Home
        MygosDestination.SEARCH -> Icons.Filled.Search
        MygosDestination.ACTIVITIES -> Icons.Filled.DateRange
        MygosDestination.PROFILE -> Icons.Filled.Person
        else -> Icons.Filled.AccountCircle
    }
    MygosScreen {
        Text(
            stringResource(R.string.nav_brand),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    icon,
                    null,
                    Modifier
                        .padding(14.dp)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(stringResource(destination.title), style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            stringResource(
                when (destination) {
                    MygosDestination.HOME -> R.string.nav_home_description
                    MygosDestination.SEARCH -> R.string.nav_search_description
                    MygosDestination.ACTIVITIES -> R.string.nav_activities_description
                    MygosDestination.PROFILE -> R.string.nav_profile_description
                    else -> R.string.nav_access_description
                }
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MygosCard {
            Text(
                stringResource(R.string.nav_pending_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.nav_temporary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (destination == MygosDestination.PROFILE) {
            MygosCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.nav_contact_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                MygosPrimaryButton(
                    stringResource(R.string.contact_title),
                    { onNavigate(MygosDestination.CONTACT) })
            }
            if (user != null) {
                MygosCard {
                    Text(stringResource(R.string.session_account), style = MaterialTheme.typography.titleMedium)
                    Text(user.name, style = MaterialTheme.typography.titleLarge)
                    Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.session_logout))
                    }
                }
            } else {
            MygosCard {
                Text(
                    stringResource(R.string.nav_account),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = { onNavigate(MygosDestination.LOGIN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small
                ) { Text(stringResource(R.string.nav_login)) }
                TextButton(
                    onClick = { onNavigate(MygosDestination.REGISTER) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.nav_register))
                }
            }
            }
        }
    }
}
