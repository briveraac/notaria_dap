package com.duoc.plomoredsocial.ui.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.plomoredsocial.BuildConfig
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.UUID
import java.util.Locale
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.duoc.plomoredsocial.model.ActivityFormValidator
import com.duoc.plomoredsocial.model.participantCount

// La ruta antigua conserva únicamente el mapa; ya no repite el catálogo ni los filtros.
@Composable
fun ActivitiesScreen(user: AuthUser?, onLogin: () -> Unit, viewModel: ActivitiesViewModel = viewModel()) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    MygosScreen {
        Text("Mapa de actividades", style = MaterialTheme.typography.headlineMedium)
        MygosNotice(stringResource(R.string.activities_local))
        ActivityMapContent(viewModel.activities, selectedId, { selectedId = it })
    }
    selectedId?.let { id ->
        viewModel.activities.firstOrNull { it.id == id }?.let { activity ->
            ActivityDetail(activity, user, viewModel, onDismiss = { selectedId = null },
                onMap = { selectedId = null }, onAccess = { selectedId = null; onLogin() })
        }
    }
}

// Explorar abre el mismo formulario y usa las operaciones existentes del ViewModel.
@Composable
fun ActivityCreation(user: AuthUser, viewModel: ActivitiesViewModel, onDismiss: () -> Unit) {
    ActivityForm(saving = viewModel.saving, failed = viewModel.error,
        onDismiss = { if (!viewModel.saving) onDismiss() },
        onSave = { title, commune, place, schedule, description, location, capacity ->
            viewModel.create(ActivityEntity(UUID.randomUUID().toString(), title, commune,
                place, schedule, location?.latitude, location?.longitude, user.id, description, capacity), onDismiss)
        })
}

// Comparte la cámara, los marcadores y los controles entre el mapa y la ruta antigua.
@Composable
private fun ActivityMapContent(
    activities: List<ActivityEntity>, selectedId: String?, onSelect: (String) -> Unit
) {
    // Solo los puntos realmente guardados se convierten en marcadores.
    val located = activities.filter { it.latitude != null && it.longitude != null }
    if (activities.any { it.latitude == null || it.longitude == null }) {
        Text("Las actividades con dirección textual siguen disponibles en el catálogo; no tienen marcador.")
    }
    if (BuildConfig.MAPS_CONFIGURED) {
        val camera = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(-33.4489, -70.6693), 11f)
        }
        LaunchedEffect(selectedId, activities) {
            located.firstOrNull { it.id == selectedId }?.let {
                camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(requireNotNull(it.latitude), requireNotNull(it.longitude)), 15f))
            }
        }
        GoogleMap(modifier = Modifier.fillMaxWidth().height(280.dp).testTag("activities_map"),
            cameraPositionState = camera,
            uiSettings = MapUiSettings(scrollGesturesEnabled = false, zoomControlsEnabled = true)) {
            located.forEach { activity ->
                key(activity.id) {
                    Marker(state = rememberUpdatedMarkerState(LatLng(requireNotNull(activity.latitude), requireNotNull(activity.longitude))),
                        title = activity.title, snippet = activity.meetingPoint,
                        onClick = { onSelect(activity.id); false })
                }
            }
        }
    } else MygosNotice(stringResource(R.string.activities_map_unconfigured))
}

// El mapa muestra solo la selección actual, sin otro catálogo ni filtros.
@Composable
fun ActivityMapDialog(activities: List<ActivityEntity>, initialId: String?,
    onDismiss: () -> Unit, onDetail: (String) -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf(initialId) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            MygosScreen(Modifier.testTag("activity_map_dialog")) {
                Text("Mapa de actividades", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onDismiss) { Text("Cerrar mapa") }
                ActivityMapContent(activities, selectedId, { selectedId = it })
                if (activities.isEmpty()) Text(stringResource(R.string.activities_empty))
                activities.firstOrNull { it.id == selectedId }?.let { activity ->
                    MygosCard {
                        Text(activity.title, style = MaterialTheme.typography.titleLarge)
                        Text(activity.meetingPoint)
                        MygosPrimaryButton("Ver detalle", { onDetail(activity.id) })
                    }
                }
            }
        }
    }
}

// El detalle conserva indicaciones e inscripción/retiro y observa los inscritos reales.
@Composable
fun ActivityDetail(activity: ActivityEntity, user: AuthUser?, viewModel: ActivitiesViewModel,
    onDismiss: () -> Unit, onMap: () -> Unit, onAccess: () -> Unit) {
    val context = LocalContext.current
    var mapsError by remember { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable(activity.id, user?.id) { mutableStateOf(false) }
    val members = viewModel.members.filter { it.activityId == activity.id }
    val participants = activity.participantCount(members)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            MygosScreen(Modifier.testTag("activity_detail")) {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
                Text(activity.title, style = MaterialTheme.typography.headlineMedium)
                Text(activity.commune, color = MaterialTheme.colorScheme.primary)
                Text(activity.schedule)
                Text(activity.meetingPoint)
                if (activity.description.isNotBlank()) {
                    Text("Descripción", style = MaterialTheme.typography.titleMedium)
                    Text(activity.description, modifier = Modifier.testTag("activity_description"))
                }
                val hasCoordinates = activity.latitude != null && activity.longitude != null
                if (hasCoordinates && !BuildConfig.MAPS_CONFIGURED) MygosNotice("El mapa integrado no está configurado. Puedes consultar la dirección y abrir las indicaciones externas.")
                if (!hasCoordinates) MygosNotice("Ubicación indicada por dirección o referencia. Esta actividad no tiene coordenadas para mapa o indicaciones.")
                Text("Participantes: $participants/${activity.capacity}")
                if (participants >= activity.capacity) MygosNotice("Actividad completa")
                viewModel.operationMessage?.let { MygosNotice(it, isError = true) }
                if (viewModel.error) MygosNotice(stringResource(R.string.activities_error), isError = true)
                if (mapsError) MygosNotice(stringResource(R.string.activities_maps_error), isError = true)
                if (user == null) {
                    MygosPrimaryButton("Iniciar sesión para participar", onAccess)
                } else if (user.id == activity.organizerId) {
                    Text(stringResource(R.string.activities_organizer))
                    OutlinedButton(onClick = { confirmingDelete = true }, enabled = !viewModel.saving,
                        modifier = Modifier.fillMaxWidth().testTag("delete_activity")) { Text("Eliminar actividad") }
                } else {
                    val joined = members.any { it.userId == user.id }
                    MygosPrimaryButton(stringResource(if (joined) R.string.activities_leave else R.string.activities_join),
                        { viewModel.toggleMembership(activity.id, user.id, joined) },
                        !viewModel.saving && (joined || participants < activity.capacity))
                }
                OutlinedButton(onClick = onMap, enabled = hasCoordinates && BuildConfig.MAPS_CONFIGURED, modifier = Modifier.fillMaxWidth()) { Text("Ver mapa") }
                OutlinedButton(onClick = {
                    val uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                        .appendQueryParameter("api", "1")
                        .appendQueryParameter("destination", "${activity.latitude},${activity.longitude}").build()
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        mapsError = false
                    } catch (_: ActivityNotFoundException) { mapsError = true }
                }, enabled = hasCoordinates, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.activities_directions)) }
            }
        }
    }
    if (confirmingDelete && user?.id == activity.organizerId) {
        AlertDialog(onDismissRequest = { if (!viewModel.saving) confirmingDelete = false },
            title = { Text("Eliminar actividad") },
            text = { Text("Se eliminará la actividad y sus inscripciones. Esta acción es definitiva.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(activity.id, requireNotNull(user).id) {
                        confirmingDelete = false
                        onDismiss()
                    }
                }, enabled = !viewModel.saving) { Text("Eliminar definitivamente") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }, enabled = !viewModel.saving) { Text("Cancelar") }
            })
    }
}

// Estado del borrador recuperable; las reglas se comparten con el ViewModel de guardado.
@Composable
private fun ActivityForm(
    saving: Boolean, failed: Boolean, onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, LatLng?, Int) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var capacity by rememberSaveable { mutableStateOf(10) }
    var commune by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var dateInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var timeInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val date = ActivityFormValidator.formatDate(dateInput.text)
    val time = ActivityFormValidator.formatTime(timeInput.text)
    var dateValidated by rememberSaveable { mutableStateOf(false) }
    var timeValidated by rememberSaveable { mutableStateOf(false) }
    var dateFocused by remember { mutableStateOf(false) }
    var timeFocused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    var description by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var datePicking by rememberSaveable { mutableStateOf(false) }
    var timePicking by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val location = latitude?.let { lat -> longitude?.let { LatLng(lat, it) } }

    // Actualiza la validación temporal si el formulario permanece abierto.
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val dateError = ActivityFormValidator.dateError(date, currentTime)
    val timeError = ActivityFormValidator.timeError(time)
    val scheduleError = ActivityFormValidator.scheduleError(date, time, currentTime)
    val showDateError = attempted || dateValidated
    val showTimeError = attempted || timeValidated
    val showScheduleError = when {
        dateError != null -> showDateError
        timeError != null -> showTimeError
        else -> attempted || (dateValidated && timeValidated)
    }
    val issues = buildList {
        if (title.isBlank()) add("Ingresa el nombre de la actividad.")
        if (commune.isBlank()) add("Ingresa la comuna.")
        if (place.isBlank()) add("Ingresa la dirección o referencia del encuentro.")
        scheduleError?.let { add(it) }
        ActivityFormValidator.descriptionError(description)?.let { add(it) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            MygosScreen(Modifier.testTag("activity_form")) {
                Text(stringResource(R.string.activities_create), style = MaterialTheme.typography.headlineMedium)
                Text("Capacidad máxima: $capacity (incluye al organizador)")
                // Selector discreto: diez opciones válidas y valor inicial de diez cupos.
                Slider(value = capacity.toFloat(), onValueChange = { capacity = it.toInt().coerceIn(1, 10) },
                    valueRange = 1f..10f, steps = 8, enabled = !saving,
                    modifier = Modifier.testTag("create_capacity"))
                OutlinedTextField(title, { title = it.take(100) }, label = { Text(stringResource(R.string.activities_title)) },
                    modifier = Modifier.fillMaxWidth().testTag("create_title"), singleLine = true, enabled = !saving,
                    isError = attempted && title.isBlank())
                OutlinedTextField(commune, { commune = it.take(100) }, label = { Text(stringResource(R.string.activities_commune)) },
                    modifier = Modifier.fillMaxWidth().testTag("create_commune"), singleLine = true, enabled = !saving,
                    isError = attempted && commune.isBlank())
                // Dígitos y selección reales; los separadores se dibujan sin alterar el cursor.
                OutlinedTextField(dateInput, {
                    if (it.text != dateInput.text) dateValidated = false
                    dateInput = numericInput(it, 8)
                }, visualTransformation = DateInputTransformation,
                    label = { Text("Fecha (DD-MM-AAAA)") }, placeholder = { Text("30-10-2026") },
                    modifier = Modifier.fillMaxWidth().testTag("create_date").onFocusChanged {
                        if (dateFocused && !it.isFocused) dateValidated = true
                        if (!dateFocused && it.isFocused) dateValidated = false
                        dateFocused = it.isFocused
                    }, singleLine = true, enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        dateValidated = true
                        focus.moveFocus(FocusDirection.Down)
                    }),
                    isError = showDateError && dateError != null,
                    supportingText = { if (showDateError && dateError != null) Text(dateError) },
                    trailingIcon = { IconButton(onClick = { datePicking = true }, enabled = !saving) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Elegir fecha")
                    } })
                OutlinedTextField(timeInput, {
                    if (it.text != timeInput.text) timeValidated = false
                    timeInput = numericInput(it, 4)
                }, visualTransformation = TimeInputTransformation,
                    label = { Text("Hora (HH:mm)") }, placeholder = { Text("19:20") },
                    modifier = Modifier.fillMaxWidth().testTag("create_time").onFocusChanged {
                        if (timeFocused && !it.isFocused) timeValidated = true
                        if (!timeFocused && it.isFocused) timeValidated = false
                        timeFocused = it.isFocused
                    }, singleLine = true, enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { timeValidated = true; focus.clearFocus() }),
                    isError = showTimeError && (timeError != null || (dateError == null && scheduleError != null)),
                    supportingText = {
                        if (showTimeError) {
                            (timeError ?: if (dateError == null) scheduleError else null)?.let { Text(it) }
                        }
                    },
                    trailingIcon = { IconButton(onClick = { timePicking = true }, enabled = !saving) {
                        ClockIcon()
                    } })
                OutlinedTextField(description, { description = it.take(2000) },
                    label = { Text("Descripción (opcional)") }, minLines = 3, maxLines = 8, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("create_description"),
                    supportingText = { Text("${description.length} / 2000 caracteres") })
                OutlinedTextField(place, { place = it.take(200) }, label = { Text(stringResource(R.string.activities_place)) },
                    modifier = Modifier.fillMaxWidth().testTag("create_place"), enabled = !saving,
                    isError = attempted && place.isBlank())
                // La ubicación textual siempre es válida; las coordenadas son opcionales y reales.
                MygosNotice(if (BuildConfig.MAPS_CONFIGURED)
                    "Puedes guardar con comuna y dirección, o elegir un punto exacto en el mapa."
                    else "Google Maps no está configurado. Puedes guardar con comuna y dirección o referencia, sin coordenadas.")
                if (location != null) {
                    Text(stringResource(R.string.activities_coordinates,
                        String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)))
                    TextButton(onClick = { latitude = null; longitude = null }, enabled = !saving) {
                        Text("Usar solo dirección textual")
                    }
                }
                OutlinedButton(onClick = { picking = true }, enabled = BuildConfig.MAPS_CONFIGURED && !saving,
                    modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.activities_pick)) }
                if (failed) MygosNotice(stringResource(R.string.activities_error), isError = true)
                if (issues.isNotEmpty()) {
                    MygosNotice("Para guardar:\n" + issues.joinToString("\n") {
                        if (it == scheduleError && !showScheduleError)
                            "Completa la fecha y la hora para guardar." else it
                    })
                }
                MygosPrimaryButton(stringResource(if (saving) R.string.activities_saving else R.string.activities_save), {
                    attempted = true
                    // Comprueba nuevamente el reloj al pulsar, sin depender de una recomposición previa.
                    if (ActivityFormValidator.scheduleError(date, time) == null && issues.isEmpty()) {
                        onSave(title.trim(), commune.trim(), place.trim(), "$date, $time", description.trim(), location, capacity)
                    } else currentTime = System.currentTimeMillis()
                }, !saving && issues.isEmpty())
                TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activities_cancel))
                }
            }
        }
    }
    // Los selectores nativos se reabren si la actividad se recrea mientras estaban visibles.
    if (datePicking) DisposableEffect(context) {
        val initial = Calendar.getInstance()
        if (ActivityFormValidator.dateError(date) == null) {
            initial.set(date.takeLast(4).toInt(), date.substring(3, 5).toInt() - 1, date.take(2).toInt())
        }
        val picker = DatePickerDialog(context, { _, year, month, day ->
            val digits = String.format(Locale.US, "%02d%02d%04d", day, month + 1, year)
            dateInput = TextFieldValue(digits, TextRange(digits.length))
            dateValidated = true
            datePicking = false
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
        picker.datePicker.minDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        picker.setOnDismissListener { datePicking = false }
        picker.show()
        onDispose { picker.setOnDismissListener(null); picker.dismiss() }
    }
    if (timePicking) DisposableEffect(context) {
        val initial = Calendar.getInstance()
        val valid = ActivityFormValidator.timeError(time) == null
        val picker = TimePickerDialog(context, { _, hour, minute ->
            val digits = String.format(Locale.US, "%02d%02d", hour, minute)
            timeInput = TextFieldValue(digits, TextRange(digits.length))
            timeValidated = true
            timePicking = false
        }, if (valid) time.take(2).toInt() else initial.get(Calendar.HOUR_OF_DAY),
            if (valid) time.takeLast(2).toInt() else initial.get(Calendar.MINUTE), true)
        picker.setOnDismissListener { timePicking = false }
        picker.show()
        onDispose { picker.setOnDismissListener(null); picker.dismiss() }
    }
    if (picking) LocationPicker(location, onDismiss = { picking = false }) {
        latitude = it.latitude
        longitude = it.longitude
        picking = false
    }
}

@Composable
private fun ClockIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp).semantics { contentDescription = "Elegir hora" }) {
        drawCircle(color, radius = size.minDimension * 0.4f, style = Stroke(width = 2.dp.toPx()))
        drawLine(color, center, Offset(center.x, size.height * 0.25f), strokeWidth = 2.dp.toPx())
        drawLine(color, center, Offset(size.width * 0.68f, size.height * 0.58f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
private fun LocationPicker(initial: LatLng?, onDismiss: () -> Unit, onConfirm: (LatLng) -> Unit) {
    var latitude by rememberSaveable { mutableStateOf(initial?.latitude) }
    var longitude by rememberSaveable { mutableStateOf(initial?.longitude) }
    val selected = latitude?.let { lat -> longitude?.let { LatLng(lat, it) } }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initial ?: LatLng(-33.4489, -70.6693), if (initial == null) 11f else 16f)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.activities_pick_hint))
                GoogleMap(modifier = Modifier.fillMaxWidth().weight(1f), cameraPositionState = camera,
                    onMapClick = { latitude = it.latitude; longitude = it.longitude }) {
                    selected?.let {
                        Marker(state = rememberUpdatedMarkerState(it), title = stringResource(R.string.activities_point))
                    }
                }
                MygosPrimaryButton(stringResource(R.string.activities_confirm_point), { selected?.let(onConfirm) }, selected != null)
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.activities_cancel)) }
            }
        }
    }
}
