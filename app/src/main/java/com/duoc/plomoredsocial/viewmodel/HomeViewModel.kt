package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.model.ActivityEntity
import com.duoc.plomoredsocial.model.participantCount
import com.duoc.plomoredsocial.repository.ActivityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// Información que Inicio necesita: actividades, indicador de carga y aviso de error.
data class HomeUiState(
    val activities: List<ActivityEntity> = emptyList(),
    val participantCounts: Map<String, Int> = emptyMap(),
    val loading: Boolean = true,
    val failed: Boolean = false
)

// Recibe las actividades y prepara lo que mostrará Inicio, separado del diseño de la pantalla.
class HomeViewModel(private val repository: ActivityRepository) : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        // Actualiza el listado cuando llegan datos y termina el indicador de carga.
        viewModelScope.launch {
            try {
                // Si cambian las actividades o los inscritos, Inicio se actualiza.
                // Incluye al organizador una sola vez, sin crear inscripciones artificiales.
                combine(repository.observeActivities(), repository.observeMembers()) { activities, members ->
                    HomeUiState(activities = activities, loading = false,
                        participantCounts = activities.associate { it.id to it.participantCount(members) })
                }.collect { uiState = it }
            } catch (cancelled: CancellationException) {
                // Si se detiene la carga, respeta esa interrupción sin mostrar un error.
                throw cancelled
            } catch (exception: Exception) {
                // Si falla la lectura, permite que Inicio muestre un aviso.
                uiState = HomeUiState(loading = false, failed = true)
            }
        }
    }

    // Crea el encargado de Inicio con la fuente de actividades que entrega la aplicación.
    class Factory(private val repository: ActivityRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
    }
}
