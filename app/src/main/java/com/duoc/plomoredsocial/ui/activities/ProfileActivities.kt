package com.duoc.plomoredsocial.ui.activities

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.ActivitiesViewModel

// Room filtra por el ID real de sesión; nunca por correo, nombre ni un ID de demostración fijo.
@Composable
fun ColumnScope.ProfileActivities(user: AuthUser, model: ActivitiesViewModel = viewModel()) {
    val createdFlow = remember(user.id, model) { model.observeCreated(user.id) }
    val participatingFlow = remember(user.id, model) { model.observeParticipating(user.id) }
    val created by createdFlow.collectAsState(initial = emptyList())
    val participating by participatingFlow.collectAsState(initial = emptyList())
    var selectedId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var mapId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    val visible = created + participating
    if (model.error) MygosNotice("No se pudieron cargar o actualizar las actividades.", isError = true)

    ProfileActivitySection("Mis actividades creadas", "Aún no has creado actividades.",
        created, model.members, "profile_created") { selectedId = it }
    ProfileActivitySection("Actividades en las que participo", "Aún no participas en actividades.",
        participating, model.members, "profile_participating") { selectedId = it }
    visible.firstOrNull { it.id == selectedId }?.let { activity ->
        ActivityDetail(activity, user, model, onDismiss = { selectedId = null },
            onMap = { selectedId = null; mapId = activity.id }, onAccess = { selectedId = null })
    }
    if (mapId != null) ActivityMapDialog(visible, mapId, onDismiss = { mapId = null },
        onDetail = { selectedId = it; mapId = null })
}

@Composable
private fun ProfileActivitySection(title: String, empty: String, activities: List<ActivityEntity>,
    members: List<ActivityMember>, tag: String, onSelect: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag(tag))
    if (activities.isEmpty()) MygosNotice(empty)
    activities.forEach { activity ->
        key(activity.id) {
            MygosCard {
                Text(activity.title, style = MaterialTheme.typography.titleMedium)
                Text(activity.schedule)
                Text(activity.commune, color = MaterialTheme.colorScheme.primary)
                MygosMetric("Participantes: ${activity.participantCount(members)}/${activity.capacity}")
                MygosPrimaryButton("Ver detalle", { onSelect(activity.id) },
                    modifier = Modifier.testTag("${tag}_${activity.id}"))
            }
        }
    }
}
