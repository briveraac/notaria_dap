package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: AdminRepository) : ViewModel() {
    var snapshot by mutableStateOf<AdminSnapshot?>(null)
        private set
    var denied by mutableStateOf(false)
        private set
    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try { repository.observePanel().collect { snapshot = it; loading = false } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { snapshot = null; denied = true; loading = false }
            catch (_: Exception) { snapshot = null; message = "No se pudo cargar el panel."; loading = false }
        }
    }

    fun editUser(id: String, name: String, email: String, done: () -> Unit) = perform(done) {
        repository.updateUser(id, name, email)
    }
    fun resetPassword(id: String, password: String, done: () -> Unit) = perform(done) {
        repository.resetPassword(id, password)
    }
    fun deleteUser(id: String, done: () -> Unit) = perform(done) { repository.deleteUser(id) }
    fun deleteActivity(id: String, done: () -> Unit) = perform(done) { repository.deleteActivity(id) }
    fun clearMessage() { message = null }

    private fun perform(done: () -> Unit, action: suspend () -> Unit) {
        if (saving || denied) return
        saving = true
        message = null
        viewModelScope.launch {
            try { action(); done() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { snapshot = null; denied = true }
            catch (_: DuplicateEmailException) { message = "El correo ya está registrado." }
            catch (error: AdminOperationException) { message = error.message }
            catch (_: Exception) { message = "No se pudo completar la operación." }
            finally { saving = false }
        }
    }

    class Factory(private val repository: AdminRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AdminViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return AdminViewModel(repository) as T
        }
    }
}
