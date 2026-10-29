package com.duoc.plomoredsocial.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.ActivityDao
import com.duoc.plomoredsocial.model.ActivityMember
import com.duoc.plomoredsocial.model.MygosDatabase
import com.duoc.plomoredsocial.repository.RoomActivityRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch

class ActivitiesViewModel(application: Application, private val dao: ActivityDao) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, MygosDatabase.getInstance(application).activityDao())
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
    var operationMessage by mutableStateOf<String?>(null)
        private set

    fun observeCreated(userId: String) = dao.observeCreated(userId).catch { error = true; emit(emptyList()) }
    fun observeParticipating(userId: String) = dao.observeParticipating(userId).catch { error = true; emit(emptyList()) }

    init {
        viewModelScope.launch {
            try { repository.observeActivities().collect { activities = it } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = true }
        }
        viewModelScope.launch {
            try { repository.observeMembers().collect { members = it } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = true }
        }
    }

    fun create(activity: ActivityEntity, onSuccess: () -> Unit) = persist {
        // Revalida al guardar: la hora puede haber pasado mientras el formulario estaba abierto.
        require(com.duoc.plomoredsocial.model.ActivityFormValidator.isValid(activity)) {
            "Revisa los datos de la actividad."
        }
        dao.createForAccount(activity)
        onSuccess()
    }

    fun toggleMembership(activityId: String, userId: String, joined: Boolean) = persist {
        if (joined) dao.leave(activityId, userId)
        else if (!dao.joinForAccount(ActivityMember(activityId, userId))) {
            operationMessage = "La actividad está completa o ya no está disponible."
        }
    }

    fun delete(activityId: String, userId: String, onSuccess: () -> Unit) = persist {
        if (dao.deleteAsOrganizer(activityId, userId)) onSuccess()
        else operationMessage = "Solo el organizador puede eliminar esta actividad."
    }

    private fun persist(action: suspend () -> Unit) {
        if (saving) return
        saving = true
        error = false
        operationMessage = null
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = true }
            finally { saving = false }
        }
    }
}
