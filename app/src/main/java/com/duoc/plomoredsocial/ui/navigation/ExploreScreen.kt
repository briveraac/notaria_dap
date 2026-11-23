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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.data.remote.xano.redactedForDisplay
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.HomeUiState
import com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel
import com.duoc.plomoredsocial.viewmodel.XanoActivitiesViewModel
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
    val onlineActivities: XanoActivitiesViewModel = viewModel(factory = XanoActivitiesViewModel.Factory())
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
            item(key = "online_activities") {
                OnlineActivitiesSection(onlineActivities)
            }
        }
    }
}

/** Xano is displayed as a separate source; it is never inserted into Room. */
@Composable
private fun OnlineActivitiesSection(viewModel: XanoActivitiesViewModel) {
    val state = viewModel.uiState
    var detailId by rememberSaveable { mutableStateOf("") }
    var searchComuna by rememberSaveable { mutableStateOf("") }
    var createTitulo by rememberSaveable { mutableStateOf("") }
    var createComuna by rememberSaveable { mutableStateOf("") }
    var createCupos by rememberSaveable { mutableStateOf("10") }
    var updateId by rememberSaveable { mutableStateOf("") }
    var updateCupos by rememberSaveable { mutableStateOf("20") }
    var deleteId by rememberSaveable { mutableStateOf("") }

    MygosCard(Modifier.testTag("xano_activities_section")) {
        Text("Actividades en línea (Xano API)", style = MaterialTheme.typography.titleLarge)
        Text("Fuente: Xano. 7 Endpoints REST consumidos independientemente de Room.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Notification / Status messages from Xano actions
        state.actionMessage?.let { MygosNotice(it) }
        state.actionError?.let { MygosNotice(it, isError = true) }

        if (state.loading || state.actionInProgress) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("Conectando con Xano…")
            }
        }
        state.error?.let { MygosNotice(it, isError = true) }
        when {
            !state.loading && state.activities?.isEmpty() == true ->
                MygosNotice("Xano respondió correctamente y no hay actividades publicadas.")
            !state.loading && state.activities != null ->
                state.activities.forEachIndexed { index, activity ->
                    MygosCard {
                        Text("Actividad remota ${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Text(activity.raw.redactedForDisplay().toString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("xano_activity_$index"))
                    }
                }
        }

        // Endpoint 1: GET /actividades
        OutlinedButton(onClick = viewModel::refresh, enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().testTag("xano_retry")) {
            Text("1. Actualizar catálogo remoto (GET /actividades)")
        }

        // Endpoint 3: GET /actividades?comuna={comuna}
        OutlinedTextField(value = searchComuna, onValueChange = { searchComuna = it },
            label = { Text("Filtrar Xano por comuna") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("xano_search_comuna"))
        OutlinedButton(onClick = { viewModel.searchByComuna(searchComuna) },
            enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text("3. Buscar por comuna (GET /actividades?comuna)")
        }

        HorizontalDivider()

        // Endpoint 2: GET /actividades/{actividades_id}
        Text("Consultar detalle remoto (GET /actividades/{id})", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = detailId, onValueChange = { detailId = it },
            label = { Text("ID de Xano") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("xano_detail_id"))
        OutlinedButton(onClick = { viewModel.getDetail(detailId) },
            enabled = detailId.isNotBlank() && !state.detailLoading,
            modifier = Modifier.fillMaxWidth().testTag("xano_detail_load")) {
            if (state.detailLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            else Text("2. Consultar detalle (GET /actividades/{id})")
        }
        state.detailError?.let { MygosNotice(it, isError = true) }
        state.detail?.let {
            Text(it.redactedForDisplay().toString(), style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("xano_activity_detail"))
        }

        HorizontalDivider()

        // Endpoint 4: POST /actividades
        Text("Crear actividad remota (POST /actividades)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = createTitulo, onValueChange = { createTitulo = it },
            label = { Text("Título remoto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = createComuna, onValueChange = { createComuna = it },
            label = { Text("Comuna remota") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = createCupos, onValueChange = { createCupos = it },
            label = { Text("Cupos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = {
                val cuposInt = createCupos.toIntOrNull() ?: 10
                viewModel.createRemoteActivity(createTitulo.ifBlank { "Evento Xano" }, "Descripción remota", createComuna.ifBlank { "Santiago" }, "Ubicación remota", "Organizador Mygos", cuposInt)
            },
            enabled = !state.actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("4. Crear en Xano (POST /actividades)")
        }

        HorizontalDivider()

        // Endpoint 5: PATCH /actividades/{id}
        Text("Actualizar actividad remota (PATCH /actividades/{id})", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = updateId, onValueChange = { updateId = it },
            label = { Text("ID a actualizar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = updateCupos, onValueChange = { updateCupos = it },
            label = { Text("Nuevos cupos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = { viewModel.updateRemoteActivity(updateId, updateCupos.toIntOrNull() ?: 20) },
            enabled = updateId.isNotBlank() && !state.actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("5. Actualizar en Xano (PATCH /actividades/{id})")
        }

        HorizontalDivider()

        // Endpoint 6: DELETE /actividades/{id}
        Text("Eliminar actividad remota (DELETE /actividades/{id})", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = deleteId, onValueChange = { deleteId = it },
            label = { Text("ID a eliminar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = { viewModel.deleteRemoteActivity(deleteId) },
            enabled = deleteId.isNotBlank() && !state.actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("6. Eliminar en Xano (DELETE /actividades/{id})")
        }

        HorizontalDivider()

        // Endpoint 7: GET /auth/me
        Text("Verificar Auth Endpoint (GET /auth/me)", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = { viewModel.checkAuthMe() },
            enabled = !state.actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("7. Verificar Auth Remote (GET /auth/me)")
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
