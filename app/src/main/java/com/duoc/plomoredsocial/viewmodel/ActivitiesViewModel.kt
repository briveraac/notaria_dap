package com.duoc.plomoredsocial.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.ActivityMember
import com.duoc.plomoredsocial.model.MygosDatabase
import com.duoc.plomoredsocial.repository.RoomActivityRepository
import kotlinx.coroutines.launch

class ActivitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MygosDatabase.getInstance(application).activityDao()
    // Comparte con Inicio la lectura de actividades e inscritos. El guardado sigue igual.
    private val repository = RoomActivityRepository(dao)
    var activities by mutableStateOf(emptyList<ActivityEntity>())
        private set
    var members by mutableStateOf(emptyList<ActivityMember>())
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            try { repository.observeActivities().collect { activities = it } }
            catch (_: Exception) { error = true }
        }
        viewModelScope.launch {
            try { repository.observeMembers().collect { members = it } }
            catch (_: Exception) { error = true }
        }
    }

    fun create(activity: ActivityEntity, onSuccess: () -> Unit) = persist {
        // Revalida al guardar: la hora puede haber pasado mientras el formulario estaba abierto.
        require(com.duoc.plomoredsocial.model.ActivityFormValidator.isValid(activity)) {
            "Revisa los datos de la actividad."
        }
        dao.create(activity)
        onSuccess()
    }

    fun toggleMembership(activityId: String, userId: String, joined: Boolean) = persist {
        if (joined) dao.leave(activityId, userId) else dao.join(ActivityMember(activityId, userId))
    }

    private fun persist(action: suspend () -> Unit) {
        if (saving) return
        saving = true
        error = false
        viewModelScope.launch {
            try { action() } catch (_: Exception) { error = true }
            finally { saving = false }
        }
    }
}
