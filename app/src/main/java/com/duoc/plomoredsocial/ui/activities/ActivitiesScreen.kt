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

@Composable
fun ActivitiesScreen(user: AuthUser?, viewModel: ActivitiesViewModel = viewModel()) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var commune by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val activities = viewModel.activities.filter {
        commune.isBlank() || it.commune.contains(commune.trim(), ignoreCase = true)
    }
    val context = LocalContext.current
    var mapsError by remember { mutableStateOf(false) }

    MygosScreen {
        Text(stringResource(R.string.nav_activities), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.activities_intro))
        MygosNotice(stringResource(R.string.activities_local))
        if (viewModel.error) MygosNotice(stringResource(R.string.activities_error), isError = true)
        if (user == null) Text(stringResource(R.string.activities_login))
        MygosPrimaryButton(stringResource(R.string.activities_create), { creating = true }, user != null)
        OutlinedTextField(value = commune, onValueChange = { commune = it },
            label = { Text(stringResource(R.string.activities_filter)) },
            modifier = Modifier.fillMaxWidth().testTag("activities_commune"), singleLine = true)

        if (BuildConfig.MAPS_CONFIGURED) {
            val camera = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(-33.4489, -70.6693), 11f)
            }
            LaunchedEffect(selectedId, activities) {
                activities.firstOrNull { it.id == selectedId }?.let {
                    camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
                }
            }
            GoogleMap(modifier = Modifier.fillMaxWidth().height(280.dp), cameraPositionState = camera,
                uiSettings = MapUiSettings(scrollGesturesEnabled = false, zoomControlsEnabled = true)) {
                activities.forEach { activity ->
                    key(activity.id) {
                        Marker(state = rememberUpdatedMarkerState(LatLng(activity.latitude, activity.longitude)),
                            title = activity.title, snippet = activity.meetingPoint,
                            onClick = { selectedId = activity.id; false })
                    }
                }
            }
        } else MygosNotice(stringResource(R.string.activities_map_unconfigured))

        if (activities.isEmpty()) Text(stringResource(R.string.activities_empty))
        if (mapsError) MygosNotice(stringResource(R.string.activities_maps_error), isError = true)
        // La selección del marcador pone su actividad primero sin ocultar las otras.
        activities.sortedByDescending { it.id == selectedId }.forEach { activity ->
            MygosCard {
                Text(activity.title, style = MaterialTheme.typography.titleLarge)
                Text(activity.commune, color = MaterialTheme.colorScheme.primary)
                Text(activity.schedule)
                Text(activity.meetingPoint)
                val members = viewModel.members.filter { it.activityId == activity.id }
                Text(stringResource(R.string.activities_attendees, members.size))
                OutlinedButton(onClick = {
                    val uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                        .appendQueryParameter("api", "1")
                        .appendQueryParameter("destination", "${activity.latitude},${activity.longitude}").build()
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        mapsError = false
                    } catch (_: ActivityNotFoundException) { mapsError = true }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.activities_directions)) }
                if (user?.id == activity.organizerId) {
                    Text(stringResource(R.string.activities_organizer))
                } else {
                    val joined = members.any { it.userId == user?.id }
                    MygosPrimaryButton(stringResource(if (joined) R.string.activities_leave else R.string.activities_join),
                        { user?.let { viewModel.toggleMembership(activity.id, it.id, joined) } },
                        enabled = user != null && !viewModel.saving)
                }
            }
        }
    }
    if (creating && user != null) {
        ActivityForm(saving = viewModel.saving, failed = viewModel.error,
            onDismiss = { if (!viewModel.saving) creating = false },
            onSave = { title, communeName, place, schedule, location ->
                viewModel.create(ActivityEntity(UUID.randomUUID().toString(), title, communeName,
                    place, schedule, location.latitude, location.longitude, user.id)) { creating = false }
            })
    }
}

@Composable
private fun ActivityForm(
    saving: Boolean, failed: Boolean, onDismiss: () -> Unit,
    onSave: (String, String, String, String, LatLng) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var commune by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var schedule by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val location = latitude?.let { lat -> longitude?.let { LatLng(lat, it) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            MygosScreen {
                Text(stringResource(R.string.activities_create), style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(title, { title = it.take(100) }, label = { Text(stringResource(R.string.activities_title)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
                OutlinedTextField(commune, { commune = it.take(100) }, label = { Text(stringResource(R.string.activities_commune)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
                OutlinedTextField(schedule, { schedule = it.take(100) }, label = { Text(stringResource(R.string.activities_schedule)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
                OutlinedTextField(place, { place = it.take(200) }, label = { Text(stringResource(R.string.activities_place)) },
                    modifier = Modifier.fillMaxWidth(), enabled = !saving)
                Text(stringResource(R.string.activities_pick_hint))
                if (location != null) Text(stringResource(R.string.activities_coordinates,
                    String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)))
                if (!BuildConfig.MAPS_CONFIGURED) MygosNotice(stringResource(R.string.activities_map_unconfigured))
                OutlinedButton(onClick = { picking = true }, enabled = BuildConfig.MAPS_CONFIGURED && !saving,
                    modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.activities_pick)) }
                if (failed) MygosNotice(stringResource(R.string.activities_error), isError = true)
                MygosPrimaryButton(stringResource(if (saving) R.string.activities_saving else R.string.activities_save), {
                    location?.let { onSave(title.trim(), commune.trim(), place.trim(), schedule.trim(), it) }
                }, !saving && title.isNotBlank() && commune.isNotBlank() && place.isNotBlank() && schedule.isNotBlank() && location != null)
                TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.activities_cancel))
                }
            }
        }
    }
    if (picking) LocationPicker(location, onDismiss = { picking = false }) {
        latitude = it.latitude
        longitude = it.longitude
        picking = false
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
