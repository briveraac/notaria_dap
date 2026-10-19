// Recibe las acciones de la pantalla y guarda la consulta a través del repositorio.
package com.duoc.plomoredsocial.ui.contact

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.data.contact.ContactRepository
import com.duoc.plomoredsocial.domain.contact.ContactReason
import com.duoc.plomoredsocial.domain.contact.ContactValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Pantalla → ViewModel → Repository → DAO → SQLite.
// El resultado vuelve al estado y Compose muestra la carga, el éxito o el error.
class ContactViewModel(private val repository: ContactRepository) : ViewModel() {
    var uiState by mutableStateOf(ContactUiState())
        private set

    fun updateName(value: String) = update(uiState.copy(nameOrAlias = value))
    fun updateEmail(value: String) = update(uiState.copy(email = value))
    fun updateReason(value: ContactReason) = update(uiState.copy(reason = value))
    fun updateMessage(value: String) = update(uiState.copy(message = value))

    // Mientras se guarda no se puede cambiar la consulta que está en curso.
    private fun update(state: ContactUiState) {
        if (uiState.saveStatus == ContactSaveStatus.LOADING) return
        uiState = state.copy(
            errors = if (state.validationAttempted) {
                ContactValidator.validate(state.toRequest())
            } else state.errors,
            reviewVisible = false,
            saveStatus = ContactSaveStatus.IDLE,
            savedContactId = null
        )
    }

    // Antes de guardar siempre se muestra el resumen de datos válidos.
    fun continueToReview() {
        if (uiState.saveStatus == ContactSaveStatus.LOADING ||
            uiState.saveStatus == ContactSaveStatus.SUCCESS) return
        val errors = ContactValidator.validate(uiState.toRequest())
        uiState = uiState.copy(
            errors = errors,
            validationAttempted = true,
            reviewVisible = errors.isValid,
            saveStatus = ContactSaveStatus.IDLE,
            savedContactId = null
        )
    }

    fun dismissReview() {
        uiState = uiState.copy(reviewVisible = false)
    }

    // La llamada suspend de Room trabaja fuera del hilo de la interfaz.
    // El éxito se muestra solo al recibir el ID de la fila ya insertada.
    fun confirmReview() {
        if (!uiState.reviewVisible || uiState.saveStatus == ContactSaveStatus.LOADING) return
        val request = uiState.toRequest()
        val errors = ContactValidator.validate(request)
        uiState = uiState.copy(
            errors = errors,
            validationAttempted = true,
            reviewVisible = false,
            saveStatus = if (errors.isValid) ContactSaveStatus.LOADING else ContactSaveStatus.IDLE
        )
        if (!errors.isValid) return
        viewModelScope.launch {
            try {
                val id = repository.save(request)
                uiState = uiState.copy(saveStatus = ContactSaveStatus.SUCCESS, savedContactId = id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                // No borramos lo escrito: el usuario puede corregirlo o volver a intentar.
                uiState = uiState.copy(saveStatus = ContactSaveStatus.ERROR, savedContactId = null)
            }
        }
    }

    // La actividad entrega el repositorio; el ViewModel no guarda referencias a ella.
    class Factory(private val repository: ContactRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ContactViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ContactViewModel(repository) as T
        }
    }
}
