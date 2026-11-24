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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.LatLng
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.data.remote.xano.*
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.notifications.ActivityCreatedNotifier
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.HomeUiState
import com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel
import com.duoc.plomoredsocial.viewmodel.XanoActivitiesViewModel
import com.duoc.plomoredsocial.ui.activities.*
import java.util.UUID

// Explorar unifica las actividades comunitarias con Xano como fuente central de verdad y conserva la caché de Room.
@Composable
internal fun ExploreScreen(
    user: AuthUser?,
    state: HomeUiState,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    activitiesViewModel: ActivitiesViewModel = viewModel()
) {
    val filters: com.duoc.plomoredsocial.viewmodel.ExploreViewModel = viewModel()
    val onlineActivities: XanoActivitiesViewModel = viewModel(factory = XanoActivitiesViewModel.Factory())
    val remoteState = onlineActivities.uiState
    val localResults = filters.filter(state.activities)
    val listState = rememberLazyListState()

    var selectedLocalId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRemoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var mapVisible by rememberSaveable { mutableStateOf(false) }
    var mapActivityId by rememberSaveable { mutableStateOf<String?>(null) }
    var accessRequested by rememberSaveable { mutableStateOf(false) }

    val rawList = remoteState.activities ?: emptyList()
    val filteredActivities = if (filters.commune.isBlank() && filters.query.isBlank()) rawList else rawList.filter {
        (filters.commune.isBlank() || it.commune.contains(filters.commune.trim(), ignoreCase = true)) &&
        (filters.query.isBlank() || it.title.contains(filters.query.trim(), ignoreCase = true))
    }
    val selectedRemoteActivity = rawList.firstOrNull { it.id == selectedRemoteId }

    val context = LocalContext.current
    val notifier = remember(context) { ActivityCreatedNotifier(context.applicationContext) }
    var pendingEntity by remember { mutableStateOf<ActivityEntity?>(null) }
    var awaitingPermission by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingEntity?.let { runCatching { notifier.show(it) } }
        pendingEntity = null
        awaitingPermission = false
        creating = false
    }

    if (mapVisible) {
        ActivityMapDialog(localResults, mapActivityId,
            onDismiss = { mapVisible = false },
            onDetail = { selectedLocalId = it; mapVisible = false })
    }

    state.activities.firstOrNull { it.id == selectedLocalId }?.let { activity ->
        ActivityDetail(activity, user, activitiesViewModel,
            onDismiss = { selectedLocalId = null },
            onMap = {
                selectedLocalId = null
                mapActivityId = activity.id
                mapVisible = true
            },
            onAccess = { selectedLocalId = null; accessRequested = true })
    }

    if (creating && user != null) {
        ActivityForm(
            saving = activitiesViewModel.saving || remoteState.actionInProgress || awaitingPermission,
            failed = activitiesViewModel.error || remoteState.actionError != null,
            onDismiss = { if (!activitiesViewModel.saving && !remoteState.actionInProgress && !awaitingPermission) creating = false },
            onSave = { title, commune, place, schedule, description, location, capacity ->
                val validCupos = capacity.coerceIn(1, 10)
                val entity = ActivityEntity(UUID.randomUUID().toString(), title, commune, place, schedule, location?.latitude, location?.longitude, user.id, description, validCupos)
                activitiesViewModel.create(entity) {
                    onlineActivities.createRemoteActivity(
                        titulo = title,
                        descripcion = description,
                        comuna = commune,
                        ubicacion = place,
                        organizador = user.name.ifBlank { user.email },
                        cupos = validCupos,
                        fecha = schedule,
                        latitude = location?.latitude,
                        longitude = location?.longitude
                    ) { _ -> }
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                            android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        pendingEntity = entity
                        awaitingPermission = true
                        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        runCatching { notifier.show(entity) }
                        creating = false
                    }
                }
            }
        )
    }

    selectedRemoteActivity?.let { activity ->
        RemoteActivityDetailDialog(activity,
            onDismiss = { selectedRemoteId = null }
        )
    }

    if (accessRequested) {
        AlertDialog(
            modifier = Modifier.testTag("home_access_dialog"),
            onDismissRequest = { accessRequested = false },
            title = { Text("Participa en Mygos") },
            text = { Text("Inicia sesión para crear una actividad o participar.") },
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

            when {
                state.loading && remoteState.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().testTag("explore_loading"), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.failed && remoteState.error != null -> item(key = "error") {
                    MygosNotice(stringResource(R.string.home_feed_error), isError = true)
                }
                state.activities.isEmpty() && filteredActivities.isEmpty() -> item(key = "empty") {
                    MygosNotice("Todavía no hay actividades guardadas en este dispositivo. Puedes crearlas aquí al iniciar sesión.")
                }
            }

            if (!state.loading && state.activities.isNotEmpty() && localResults.isEmpty() && filteredActivities.isEmpty()) {
                item(key = "no_results") {
                    MygosCard {
                        Text("No encontramos actividades con esos filtros.")
                        MygosPrimaryButton("Limpiar filtros", filters::clear)
                    }
                }
            }

            // Tarjetas locales de Room
            items(localResults, key = { "local_${it.id}" }, contentType = { "local_activity" }) { activity ->
                ActivityCard(activity, state.participantCounts[activity.id] ?: 0,
                    onView = { selectedLocalId = activity.id })
            }

            // Tarjetas remotas de Xano
            items(filteredActivities, key = { "xano_${it.id}" }, contentType = { "xano_activity" }) { activity ->
                XanoActivityCard(activity) {
                    selectedRemoteId = activity.id
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: ActivityEntity, participants: Int, onView: () -> Unit) {
    MygosCard(Modifier.testTag("activity_${activity.id}")) {
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

@Composable
private fun ActivityInformation(activity: ActivityEntity, participants: Int) {
    Text(activity.commune, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary)
    Text(activity.schedule, style = MaterialTheme.typography.bodyMedium)
    Text(activity.meetingPoint, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    MygosMetric("Participantes: $participants/${activity.capacity}")
}

@Composable
private fun XanoActivityCard(activity: XanoActivityDto, onClick: () -> Unit) {
    MygosCard(Modifier.testTag("xano_activity_${activity.id}")) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Place, contentDescription = null,
                    modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(activity.title, style = MaterialTheme.typography.titleLarge)
        Text(activity.commune, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Fecha del evento: ${activity.schedule}", style = MaterialTheme.typography.bodyMedium)
        Text(activity.meetingPoint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MygosMetric("Capacidad: ${activity.capacity} personas")
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Ver detalle completo")
        }
    }
}

@Composable
private fun RemoteActivityDetailDialog(
    activity: XanoActivityDto,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            MygosScreen {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
                MygosHeader(activity.title)
                Text(activity.commune, color = MaterialTheme.colorScheme.primary)
                Text("Fecha: ${activity.schedule}")
                Text("Punto de encuentro: ${activity.meetingPoint}")
                if (activity.description.isNotBlank()) {
                    Text("Descripción", style = MaterialTheme.typography.titleMedium)
                    Text(activity.description)
                }
                MygosMetric("Capacidad: ${activity.capacity} personas (Rango 1–10)")
                Spacer(Modifier.height(16.dp))
                MygosNotice("La edición (PATCH) y eliminación (DELETE) están bloqueadas en la interfaz mientras Xano no valide la autorización y propiedad del propietario del registro (HTTP 401/403).", isError = true)
            }
        }
    }
}
