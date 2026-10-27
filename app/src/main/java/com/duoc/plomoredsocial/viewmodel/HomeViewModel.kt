package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.model.HomeActivity
import com.duoc.plomoredsocial.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Información que Inicio necesita: actividades, indicador de carga y aviso de error.
data class HomeUiState(
    val activities: List<HomeActivity> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false
)

// Recibe las actividades y prepara lo que mostrará Inicio, separado del diseño de la pantalla.
class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        // Actualiza el listado cuando llegan datos y termina el indicador de carga.
        viewModelScope.launch {
            try {
                repository.observeActivities().collect { activities ->
                    uiState = HomeUiState(activities = activities, loading = false)
                }
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
    class Factory(private val repository: HomeRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
    }
}
