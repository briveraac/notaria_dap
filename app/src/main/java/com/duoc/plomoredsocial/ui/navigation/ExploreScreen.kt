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

// Explorar unifica las actividades comunitarias con Xano como fuente central de verdad.
@Composable
internal fun ExploreScreen(
    user: AuthUser?,
    state: HomeUiState,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    activitiesViewModel: ActivitiesViewModel = viewModel()
) {
    val onlineActivities: XanoActivitiesViewModel = viewModel(factory = XanoActivitiesViewModel.Factory())
    val remoteState = onlineActivities.uiState
    val listState = rememberLazyListState()

    var selectedRemoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var accessRequested by rememberSaveable { mutableStateOf(false) }
    var filterCommune by rememberSaveable { mutableStateOf("") }
    var diagnosticExpanded by rememberSaveable { mutableStateOf(false) }

    val rawList = remoteState.activities ?: emptyList()
    val filteredActivities = if (filterCommune.isBlank()) rawList else rawList.filter {
        it.commune.contains(filterCommune, ignoreCase = true) || it.title.contains(filterCommune, ignoreCase = true)
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

    if (creating && user != null) {
        ActivityForm(
            saving = remoteState.actionInProgress || awaitingPermission,
            failed = remoteState.actionError != null,
            onDismiss = { if (!remoteState.actionInProgress && !awaitingPermission) creating = false },
            onSave = { title, commune, place, schedule, description, location, capacity ->
                val validCupos = capacity.coerceIn(1, 10)
                val entity = ActivityEntity(UUID.randomUUID().toString(), title, commune, place, schedule, location?.latitude, location?.longitude, user.id, description, validCupos)
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
                ) { _ ->
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
            text = { Text("Inicia sesión para crear una actividad compartida en Xano.") },
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
                    MygosHeader("Explorar", "Actividades comunitarias compartidas en Xano (Fuente Central).")
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MygosPrimaryButton("Crear actividad", {
                            if (user == null) accessRequested = true else creating = true
                        }, modifier = Modifier.weight(1f).fillMaxHeight())
                        OutlinedButton(onClick = { onlineActivities.refresh() },
                            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp)) { Text("Actualizar") }
                    }
                }
            }
            item(key = "filters") {
                MygosCard {
                    OutlinedTextField(
                        value = filterCommune,
                        onValueChange = { filterCommune = it },
                        label = { Text("Filtrar por título o comuna") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("explore_commune"),
                        leadingIcon = { Icon(Icons.Filled.Search, null) }
                    )
                    if (filterCommune.isNotEmpty()) {
                        TextButton(onClick = { filterCommune = "" }, modifier = Modifier.testTag("explore_clear")) {
                            Text("Limpiar filtro")
                        }
                    }
                }
            }
            when {
                remoteState.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().testTag("explore_loading"), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                remoteState.error != null -> item(key = "error") {
                    MygosNotice(remoteState.error ?: "Error al conectar con Xano", isError = true)
                }
                filteredActivities.isEmpty() -> item(key = "empty") {
                    MygosNotice("No hay actividades publicadas en Xano con este filtro.")
                }
            }

            // Catálogo unificado de actividades provenientes de Xano en tarjetas estructuradas Material 3
            items(filteredActivities, key = { it.id }, contentType = { "xano_activity" }) { activity ->
                XanoActivityCard(activity) {
                    selectedRemoteId = activity.id
                }
            }

            item(key = "diagnostic_toggle") {
                OutlinedButton(
                    onClick = { diagnosticExpanded = !diagnosticExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (diagnosticExpanded) "Ocultar panel de 7 Endpoints REST (Rúbrica)" else "Mostrar panel de 7 Endpoints REST (Rúbrica)")
                }
            }

            if (diagnosticExpanded) {
                item(key = "online_activities_diagnostic") {
                    OnlineActivitiesSection(onlineActivities)
                }
            }
        }
    }
}

@Composable
private fun XanoActivityCard(activity: XanoActivityDto, onClick: () -> Unit) {
    MygosCard {
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

/** Panel secundario de diagnóstico para comprobar los 7 Endpoints REST (Rúbrica). */
@Composable
private fun OnlineActivitiesSection(viewModel: XanoActivitiesViewModel) {
    val state = viewModel.uiState
    var detailId by rememberSaveable { mutableStateOf("") }
    var filterComuna by rememberSaveable { mutableStateOf("") }
    var createTitulo by rememberSaveable { mutableStateOf("") }
    var createComuna by rememberSaveable { mutableStateOf("") }
    var createCupos by rememberSaveable { mutableStateOf("5") }
    var updateId by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf("") }
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPass by rememberSaveable { mutableStateOf("") }

    MygosCard(Modifier.testTag("xano_activities_section")) {
        Text("Diagnóstico 7 Endpoints REST (Xano)", style = MaterialTheme.typography.titleLarge)
        Text("Vista secundaria para verificar los 7 endpoints exigidos por la rúbrica.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        state.actionMessage?.let { MygosNotice(it) }
        state.actionError?.let { MygosNotice(it, isError = true) }

        if (state.loading || state.actionInProgress) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("Ejecutando en Xano…")
            }
        }
        state.error?.let { MygosNotice(it, isError = true) }

        OutlinedButton(onClick = { viewModel.refresh(filterComuna.ifBlank { null }) }, enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().testTag("xano_retry")) {
            Text("1. GET /actividades")
        }

        OutlinedTextField(value = detailId, onValueChange = { detailId = it },
            label = { Text("ID para GET /actividades/{id}") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { viewModel.getDetail(detailId) },
            enabled = detailId.isNotBlank() && !state.detailLoading,
            modifier = Modifier.fillMaxWidth()) {
            Text("2. GET /actividades/{id}")
        }
        state.detail?.let {
            Text(it.redactedForDisplay().toString(), style = MaterialTheme.typography.bodySmall)
        }

        OutlinedButton(
            onClick = {
                viewModel.createRemoteActivity(
                    createTitulo.ifBlank { "Test Rúbrica" },
                    "Diagnóstico",
                    createComuna.ifBlank { "Santiago" },
                    "Ubicación",
                    "Auditor",
                    createCupos.toIntOrNull()?.coerceIn(1, 10) ?: 5,
                    "30-10-2026, 19:20",
                    null,
                    null
                ) {}
            },
            enabled = !state.actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("3. POST /actividades")
        }

        OutlinedTextField(value = updateId, onValueChange = { updateId = it },
            label = { Text("ID para PATCH") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        MygosNotice("PATCH /actividades/{id} está deshabilitado en UI hasta que Xano valide autorización por propietario (401/403).")
        OutlinedButton(
            onClick = { /* Deshabilitado por seguridad */ },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("4. PATCH /actividades/{id} (Deshabilitado)")
        }

        OutlinedTextField(value = deleteId, onValueChange = { deleteId = it },
            label = { Text("ID para DELETE") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        MygosNotice("DELETE /actividades/{id} está deshabilitado en UI hasta que Xano valide autorización por propietario (401/403).")
        OutlinedButton(
            onClick = { /* Deshabilitado por seguridad */ },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("5. DELETE /actividades/{id} (Deshabilitado)")
        }

        OutlinedTextField(value = loginEmail, onValueChange = { loginEmail = it },
            label = { Text("Correo remoto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = loginPass, onValueChange = { loginPass = it },
            label = { Text("Contraseña remota") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = { viewModel.loginRemote(loginEmail, loginPass) },
            enabled = loginEmail.isNotBlank() && loginPass.isNotBlank() && !state.actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("6. POST /auth/login")
        }

        OutlinedButton(
            onClick = { viewModel.checkAuthMe() },
            enabled = !state.actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("7. GET /auth/me")
        }
    }
}
