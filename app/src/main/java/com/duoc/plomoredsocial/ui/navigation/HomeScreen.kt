package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.HomeUiState

// Pantalla Inicio: reúne la bienvenida, el acceso a la cuenta y las actividades.
@Composable
internal fun HomeScreen(
    user: AuthUser?,
    state: HomeUiState,
    onLogin: () -> Unit,
    onProfile: () -> Unit,
    onRegister: () -> Unit
) {
    // Recuerda dónde quedó el listado al cambiar de sección o girar el teléfono.
    val listState = rememberLazyListState()
    // Recuerda qué actividad se eligió para mostrar su ventana de información.
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedActivity = state.activities.firstOrNull { it.id == selectedId }
    if (selectedActivity != null) {
        if (user == null) {
            // Sin sesión, ofrece los accesos existentes o permite seguir explorando.
            AlertDialog(
                modifier = Modifier.testTag("home_access_dialog"),
                onDismissRequest = { selectedId = null },
                title = { Text(stringResource(R.string.home_access_title)) },
                text = { Text(stringResource(R.string.home_access_description)) },
                confirmButton = {
                    TextButton(onClick = { selectedId = null; onLogin() }) {
                        Text(stringResource(R.string.nav_login))
                    }
                },
                dismissButton = {
                    Column {
                        TextButton(onClick = { selectedId = null; onRegister() }) {
                            Text(stringResource(R.string.nav_register))
                        }
                        TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.home_cancel)) }
                    }
                }
            )
        } else {
            // Con sesión, muestra los datos de la actividad. No realiza inscripciones.
            AlertDialog(
                modifier = Modifier.testTag("home_activity_dialog"),
                onDismissRequest = { selectedId = null },
                title = { Text(selectedActivity.title) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(categoryIcon(selectedActivity.category), null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text(stringResource(categoryLabel(selectedActivity.category)))
                        Text(selectedActivity.description)
                        ActivityAvailability(selectedActivity)
                        Text(stringResource(R.string.home_demo_detail))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.home_close)) }
                }
            )
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Todo Inicio se desplaza en un solo listado, que prepara las tarjetas según se necesitan.
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("home_feed"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Presentación de Mygos al comenzar el recorrido por Inicio.
            item(key = "welcome", contentType = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.nav_brand), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.home_eyebrow), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
                    Text(stringResource(R.string.home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Muestra el saludo y lleva a Login o Perfil según la sesión actual.
            item(key = "account", contentType = "account") {
                MygosCard {
                    Icon(Icons.Filled.AccountCircle, null, Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(if (user == null) stringResource(R.string.nav_account)
                        else stringResource(R.string.home_greeting, user.name),
                        style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (user == null) R.string.home_login_description else R.string.home_session_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MygosPrimaryButton(stringResource(if (user == null) R.string.nav_login else R.string.home_profile),
                        if (user == null) onLogin else onProfile)
                }
            }
            // Invitación a explorar y explicación del acceso necesario para participar.
            item(key = "catalog", contentType = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.home_feed_title), style = MaterialTheme.typography.headlineSmall)
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.home_discover_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.home_discover_description), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            // Informa si las actividades están cargando, hubo un problema o no hay datos.
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                state.failed -> item(key = "error") { MygosNotice(stringResource(R.string.home_feed_error), isError = true) }
                state.activities.isEmpty() -> item(key = "empty") { MygosNotice(stringResource(R.string.home_feed_empty)) }
            }
            // Crea una tarjeta por actividad y registra cuál se eligió al pulsar el botón.
            items(state.activities, key = { it.id }, contentType = { "activity" }) { activity ->
                ActivityCard(activity, onView = { selectedId = activity.id })
            }
        }
    }
}

// Resumen de una actividad: imagen, categoría, descripción, estado, cupos y botón de consulta.
@Composable
private fun ActivityCard(activity: HomeActivity, onView: () -> Unit) {
    MygosCard {
        // Usa un icono incluido en la app; no necesita descargar imágenes de Internet.
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.height(112.dp), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(activity.category), contentDescription = null,
                    modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(stringResource(categoryLabel(activity.category)), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(activity.title, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("activity_${activity.id}"))
        Text(activity.description, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ActivityAvailability(activity)
        OutlinedButton(onClick = onView,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("view_activity_${activity.id}")) {
            Text(stringResource(R.string.home_view_activity))
        }
    }
}

// Comparte el mismo estado y contador entre la tarjeta y la ventana de información.
@Composable
private fun ActivityAvailability(activity: HomeActivity) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(when (activity.status) {
            ActivityStatus.OPEN -> R.string.home_status_open
            ActivityStatus.IN_PROGRESS -> R.string.home_status_in_progress
            ActivityStatus.COMPLETE -> R.string.home_status_complete
        }), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.home_participants, activity.participants, activity.capacity),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Convierte cada categoría en el nombre que verá la persona.
private fun categoryLabel(category: InterestCategory): Int = when (category) {
    InterestCategory.OUTDOORS -> R.string.interest_outdoors
    InterestCategory.SPORTS -> R.string.interest_sports
    InterestCategory.CULTURE -> R.string.interest_culture
    InterestCategory.GAMES -> R.string.interest_games
    InterestCategory.TECHNOLOGY -> R.string.interest_technology
    InterestCategory.COMMUNITY -> R.string.interest_community
}

// Asigna un icono a cada categoría para reconocerla más fácilmente.
private fun categoryIcon(category: InterestCategory): ImageVector = when (category) {
    InterestCategory.OUTDOORS -> Icons.Filled.Place
    InterestCategory.SPORTS -> Icons.Filled.Star
    InterestCategory.CULTURE -> Icons.Filled.Create
    InterestCategory.GAMES -> Icons.Filled.Face
    InterestCategory.TECHNOLOGY -> Icons.Filled.Build
    InterestCategory.COMMUNITY -> Icons.Filled.Favorite
}
