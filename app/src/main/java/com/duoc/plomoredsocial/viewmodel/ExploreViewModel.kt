package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.duoc.plomoredsocial.model.ActivityEntity

// No duplica la fuente de datos: filtra las actividades que llegan del ViewModel de Room.
class ExploreViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    var query by mutableStateOf(savedState.get<String>("query") ?: "")
        private set
    var commune by mutableStateOf(savedState.get<String>("commune") ?: "")
        private set

    fun updateQuery(value: String) { query = value; savedState["query"] = value }
    fun updateCommune(value: String) { commune = value; savedState["commune"] = value }
    fun clear() { updateQuery(""); updateCommune("") }
    fun filter(activities: List<ActivityEntity>) = filterActivities(activities, query, commune)
}

// Normaliza espacios y combina ambos criterios sin distinguir mayúsculas.
internal fun filterActivities(activities: List<ActivityEntity>, query: String, commune: String): List<ActivityEntity> {
    fun normalize(value: String) = value.trim().replace(Regex("\\s+"), " ")
    val text = normalize(query)
    val area = normalize(commune)
    return activities.filter { activity ->
        (text.isEmpty() || listOf(activity.title, activity.commune, activity.meetingPoint)
            .any { normalize(it).contains(text, ignoreCase = true) }) &&
            (area.isEmpty() || normalize(activity.commune).contains(area, ignoreCase = true))
    }
}
