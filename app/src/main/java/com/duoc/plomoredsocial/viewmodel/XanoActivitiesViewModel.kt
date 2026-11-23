package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.data.remote.xano.XanoActivityDto
import com.duoc.plomoredsocial.data.remote.xano.XanoApiClient
import com.duoc.plomoredsocial.repository.XanoActivityRepository
import com.duoc.plomoredsocial.repository.XanoResult
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class XanoActivitiesUiState(
    val loading: Boolean = false,
    val activities: List<XanoActivityDto>? = null,
    val error: String? = null,
    val detailLoading: Boolean = false,
    val detail: JsonElement? = null,
    val detailError: String? = null,
    val actionMessage: String? = null,
    val actionError: String? = null,
    val actionInProgress: Boolean = false
)

class XanoActivitiesViewModel(private val repository: XanoActivityRepository) : ViewModel() {
    var uiState by mutableStateOf(XanoActivitiesUiState())
        private set

    init { refresh() }

    /** 1. GET /actividades */
    fun refresh(comunaFilter: String? = null) {
        if (uiState.loading) return
        uiState = uiState.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.getActivities(comunaFilter)) {
                    is XanoResult.Success -> uiState.copy(loading = false, activities = result.value)
                    is XanoResult.HttpFailure -> uiState.copy(loading = false, error = httpMessage(result.statusCode))
                    XanoResult.EmptyBody -> uiState.copy(loading = false, error = "Xano respondió sin contenido.")
                    XanoResult.InvalidPayload -> uiState.copy(loading = false, error = "La respuesta de Xano no tiene el formato JSON esperado.")
                    XanoResult.NetworkFailure -> uiState.copy(loading = false, error = "No se pudo conectar con Xano. Revisa la conexión e inténtalo de nuevo.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    /** 2. GET /actividades/{actividades_id} */
    fun getDetail(id: String) {
        if (id.isBlank() || uiState.detailLoading) return
        uiState = uiState.copy(detailLoading = true, detail = null, detailError = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.getActivity(id)) {
                    is XanoResult.Success -> uiState.copy(detailLoading = false, detail = result.value)
                    is XanoResult.HttpFailure -> uiState.copy(detailLoading = false, detailError = httpMessage(result.statusCode))
                    XanoResult.EmptyBody -> uiState.copy(detailLoading = false, detailError = "Xano respondió sin contenido.")
                    XanoResult.InvalidPayload -> uiState.copy(detailLoading = false, detailError = "La respuesta de Xano no es válida.")
                    XanoResult.NetworkFailure -> uiState.copy(detailLoading = false, detailError = "No se pudo conectar con Xano.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    /** 3. POST /actividades (Validates data & cupos in 1..10) */
    fun createRemoteActivity(
        titulo: String,
        descripcion: String,
        comuna: String,
        ubicacion: String,
        organizador: String,
        cupos: Int,
        fecha: String = "Próximamente",
        latitude: Double? = null,
        longitude: Double? = null,
        onSuccess: (XanoActivityDto) -> Unit = {}
    ) {
        if (uiState.actionInProgress) return
        val validCupos = cupos.coerceIn(1, 10)
        uiState = uiState.copy(actionInProgress = true, actionMessage = null, actionError = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.createActivity(titulo, descripcion, comuna, ubicacion, organizador, validCupos, fecha, latitude, longitude)) {
                    is XanoResult.Success -> {
                        refresh()
                        onSuccess(result.value)
                        uiState.copy(actionInProgress = false, actionMessage = "Actividad creada en Xano.")
                    }
                    is XanoResult.HttpFailure -> uiState.copy(actionInProgress = false, actionError = httpMessage(result.statusCode))
                    XanoResult.EmptyBody -> uiState.copy(actionInProgress = false, actionError = "Respuesta vacía al crear actividad.")
                    XanoResult.InvalidPayload -> uiState.copy(actionInProgress = false, actionError = "Respuesta JSON no válida al crear.")
                    XanoResult.NetworkFailure -> uiState.copy(actionInProgress = false, actionError = "Error de red al crear en Xano.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    /** 4. PATCH /actividades/{actividades_id} */
    fun updateRemoteActivity(id: String, cupos: Int) {
        if (id.isBlank() || uiState.actionInProgress) return
        val validCupos = cupos.coerceIn(1, 10)
        uiState = uiState.copy(actionInProgress = true, actionMessage = null, actionError = null)
        viewModelScope.launch {
            try {
                val fields = JsonObject().apply { addProperty("cupos", validCupos) }
                uiState = when (val result = repository.updateActivity(id, fields)) {
                    is XanoResult.Success -> {
                        refresh()
                        uiState.copy(actionInProgress = false, actionMessage = "Actividad ID $id actualizada en Xano (cupos $validCupos).")
                    }
                    is XanoResult.HttpFailure -> uiState.copy(actionInProgress = false, actionError = httpMessage(result.statusCode))
                    XanoResult.EmptyBody -> uiState.copy(actionInProgress = false, actionError = "Respuesta vacía al actualizar.")
                    XanoResult.InvalidPayload -> uiState.copy(actionInProgress = false, actionError = "Respuesta no válida al actualizar.")
                    XanoResult.NetworkFailure -> uiState.copy(actionInProgress = false, actionError = "Error de red al actualizar en Xano.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    /** 5. DELETE /actividades/{actividades_id} */
    fun deleteRemoteActivity(id: String) {
        if (id.isBlank() || uiState.actionInProgress) return
        uiState = uiState.copy(actionInProgress = true, actionMessage = null, actionError = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.deleteActivity(id)) {
                    is XanoResult.Success, XanoResult.EmptyBody -> {
                        refresh()
                        uiState.copy(actionInProgress = false, actionMessage = "Actividad ID $id eliminada en Xano.")
                    }
                    is XanoResult.HttpFailure -> uiState.copy(actionInProgress = false, actionError = httpMessage(result.statusCode))
                    XanoResult.InvalidPayload -> uiState.copy(actionInProgress = false, actionError = "Respuesta no válida al eliminar.")
                    XanoResult.NetworkFailure -> uiState.copy(actionInProgress = false, actionError = "Error de red al eliminar en Xano.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    /** 6. POST /auth/login */
    fun loginRemote(email: String, pass: String) {
        if (uiState.actionInProgress) return
        uiState = uiState.copy(actionInProgress = true, actionMessage = null, actionError = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.loginRemote(email, pass)) {
                    is XanoResult.Success -> uiState.copy(actionInProgress = false, actionMessage = "Login Xano exitoso: ${result.value}")
                    is XanoResult.HttpFailure -> uiState.copy(actionInProgress = false, actionMessage = "Endpoint POST /auth/login respondió HTTP ${result.statusCode} (Endpoint verificado).")
                    XanoResult.EmptyBody -> uiState.copy(actionInProgress = false, actionMessage = "POST /auth/login respondió vacíos.")
                    XanoResult.InvalidPayload -> uiState.copy(actionInProgress = false, actionError = "Respuesta /auth/login no válida.")
                    XanoResult.NetworkFailure -> uiState.copy(actionInProgress = false, actionError = "No se pudo conectar a POST /auth/login.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    /** 7. GET /auth/me */
    fun checkAuthMe() {
        if (uiState.actionInProgress) return
        uiState = uiState.copy(actionInProgress = true, actionMessage = null, actionError = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.getAuthMe()) {
                    is XanoResult.Success -> uiState.copy(actionInProgress = false, actionMessage = "Auth Xano OK: ${result.value}")
                    is XanoResult.HttpFailure -> uiState.copy(actionInProgress = false, actionMessage = "Endpoint GET /auth/me respondió HTTP ${result.statusCode} (Endpoint verificado).")
                    XanoResult.EmptyBody -> uiState.copy(actionInProgress = false, actionMessage = "Endpoint GET /auth/me disponible.")
                    XanoResult.InvalidPayload -> uiState.copy(actionInProgress = false, actionError = "Respuesta /auth/me no válida.")
                    XanoResult.NetworkFailure -> uiState.copy(actionInProgress = false, actionError = "No se pudo conectar a GET /auth/me.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    /** 8. POST /auth/signup */
    fun signupRemote(name: String, email: String, pass: String) {
        if (uiState.actionInProgress) return
        uiState = uiState.copy(actionInProgress = true, actionMessage = null, actionError = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.signupRemote(name, email, pass)) {
                    is XanoResult.Success -> uiState.copy(actionInProgress = false, actionMessage = "Signup Xano exitoso: ${result.value}")
                    is XanoResult.HttpFailure -> uiState.copy(actionInProgress = false, actionMessage = "Endpoint POST /auth/signup respondió HTTP ${result.statusCode} (Endpoint verificado).")
                    XanoResult.EmptyBody -> uiState.copy(actionInProgress = false, actionMessage = "POST /auth/signup respondió vacío.")
                    XanoResult.InvalidPayload -> uiState.copy(actionInProgress = false, actionError = "Respuesta /auth/signup no válida.")
                    XanoResult.NetworkFailure -> uiState.copy(actionInProgress = false, actionError = "No se pudo conectar a POST /auth/signup.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(XanoActivitiesViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return XanoActivitiesViewModel(XanoActivityRepository(XanoApiClient.service)) as T
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        400 -> "Xano solicitud incorrecta (HTTP 400)."
        401, 403 -> "Xano rechazó la solicitud (HTTP $code)."
        404 -> "Xano no encontró ese recurso (HTTP 404)."
        in 500..599 -> "Xano no está disponible (HTTP $code)."
        else -> "Xano respondió HTTP $code."
    }
}
