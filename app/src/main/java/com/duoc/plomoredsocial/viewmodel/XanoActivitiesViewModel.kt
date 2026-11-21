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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class XanoActivitiesUiState(
    val loading: Boolean = false,
    val activities: List<XanoActivityDto>? = null,
    val error: String? = null,
    val detailLoading: Boolean = false,
    val detail: JsonElement? = null,
    val detailError: String? = null
)

class XanoActivitiesViewModel(private val repository: XanoActivityRepository) : ViewModel() {
    var uiState by mutableStateOf(XanoActivitiesUiState())
        private set

    init { refresh() }

    fun refresh() {
        if (uiState.loading) return
        uiState = uiState.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                uiState = when (val result = repository.getActivities()) {
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

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(XanoActivitiesViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return XanoActivitiesViewModel(XanoActivityRepository(XanoApiClient.service)) as T
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401, 403 -> "Xano rechazó la solicitud (HTTP $code)."
        404 -> "Xano no encontró ese recurso (HTTP 404)."
        in 500..599 -> "Xano no está disponible (HTTP $code)."
        else -> "Xano respondió HTTP $code."
    }
}
