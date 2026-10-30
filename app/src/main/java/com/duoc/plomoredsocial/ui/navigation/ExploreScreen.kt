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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.HomeUiState
import com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel
import com.duoc.plomoredsocial.ui.activities.*

// Explorar mantiene el catálogo reactivo y sus accesos según la sesión.
@Composable
internal fun ExploreScreen(
    user: AuthUser?,
    state: HomeUiState,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    activitiesViewModel: ActivitiesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // El ViewModel conserva filtros y los aplica a cada actualización de Room.
    val filters: com.duoc.plomoredsocial.viewmodel.ExploreViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val results = filters.filter(state.activities)
    // Recuerda dónde quedó el listado al cambiar de sección o girar el teléfono.
    val listState = rememberLazyListState()
    // Recuerda qué actividad se eligió para mostrar su ventana de información.
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var mapVisible by rememberSaveable { mutableStateOf(false) }
    var mapActivityId by rememberSaveable { mutableStateOf<String?>(null) }
    var accessRequested by rememberSaveable { mutableStateOf(false) }

    // Los diálogos no agregan otro listado: Explorar es la única fuente visual del catálogo.
    if (mapVisible) ActivityMapDialog(results, mapActivityId,
        onDismiss = { mapVisible = false },
        onDetail = { selectedId = it; mapVisible = false })
    state.activities.firstOrNull { it.id == selectedId }?.let { activity ->
        ActivityDetail(activity, user, activitiesViewModel,
            onDismiss = { selectedId = null },
            onMap = {
                selectedId = null
                mapActivityId = activity.id
                mapVisible = true
            },
            onAccess = { selectedId = null; accessRequested = true })
    }
    if (creating && user != null) ActivityCreation(user, activitiesViewModel) { creating = false }
    if (accessRequested) {
        AlertDialog(
            modifier = Modifier.testTag("home_access_dialog"),
            onDismissRequest = { accessRequested = false },
            title = { Text("Participa en Mygos") },
            text = { Text("Inicia sesión para crear una actividad o participar. También puedes crear una cuenta local.") },
            confirmButton = {
                TextButton(onClick = { accessRequested = false; onLogin() }) { Text("Iniciar sesión") }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { accessRequested = false; onRegister() }) { Text("Crear cuenta") }
                    TextButton(onClick = { accessRequested = false }) { Text("Cancelar") }
                }
            })
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Un único LazyColumn evita anidar desplazamientos verticales.
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().imePadding().testTag("explore_feed"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "explore_header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MygosHeader("Explorar", "Encuentra un plan cerca de ti. Busca por título, comuna o punto de encuentro.")
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MygosPrimaryButton("Crear actividad", {
                            if (user == null) accessRequested = true else creating = true
                        }, modifier = Modifier.weight(1f).fillMaxHeight())
                        OutlinedButton(onClick = { mapActivityId = null; mapVisible = true },
                            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp)) { Text("Ver mapa") }
                    }
                }
            }
            item(key = "filters") {
                MygosCard {
                    OutlinedTextField(value = filters.query, onValueChange = filters::updateQuery,
                        label = { Text("Buscar actividades") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("explore_query"),
                        leadingIcon = { Icon(Icons.Filled.Search, null) })
                    OutlinedTextField(value = filters.commune, onValueChange = filters::updateCommune,
                        label = { Text("Filtrar por comuna") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("explore_commune"))
                    if (filters.query.isNotEmpty() || filters.commune.isNotEmpty()) {
                        TextButton(onClick = filters::clear,
                            modifier = Modifier.testTag("explore_clear")) { Text("Limpiar filtros") }
                    }
                }
            }
            // Informa si las actividades están cargando, hubo un problema o no hay datos.
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().testTag("explore_loading"), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                state.failed -> item(key = "error") { MygosNotice(stringResource(R.string.home_feed_error), isError = true) }
                state.activities.isEmpty() -> item(key = "empty") { MygosNotice("Todavía no hay actividades guardadas en este dispositivo. Puedes crearlas aquí al iniciar sesión.") }
            }
            if (!state.loading && !state.failed && state.activities.isNotEmpty() && results.isEmpty()) {
                item(key = "no_results") {
                    MygosCard {
                        Text("No encontramos actividades con esos filtros.")
                        MygosPrimaryButton("Limpiar filtros", filters::clear)
                    }
                }
            }
            // Crea una tarjeta por actividad y registra cuál se eligió al pulsar el botón.
            items(results, key = { it.id }, contentType = { "activity" }) { activity ->
                ActivityCard(activity, state.participantCounts[activity.id] ?: 0,
                    onView = { selectedId = activity.id })
            }
        }
    }
}

// Resumen de una actividad guardada: punto de encuentro, datos disponibles y botón de consulta.
@Composable
private fun ActivityCard(activity: ActivityEntity, participants: Int, onView: () -> Unit) {
    MygosCard {
        // Usa un icono incluido en la app; no necesita descargar imágenes de Internet.
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Place, contentDescription = null,
                    modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(activity.title, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("activity_${activity.id}"))
        ActivityInformation(activity, participants)
        OutlinedButton(onClick = onView,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("view_activity_${activity.id}")) {
            Text(stringResource(R.string.home_view_activity))
        }
    }
}

// Comparte los datos reales entre la tarjeta y su ventana de información.
// La capacidad viene de Room e incluye el cupo del organizador.
@Composable
private fun ActivityInformation(activity: ActivityEntity, participants: Int) {
    Text(activity.commune, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary)
    Text(activity.schedule, style = MaterialTheme.typography.bodyMedium)
    Text(activity.meetingPoint, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    MygosMetric("Participantes: $participants/${activity.capacity}")
}
