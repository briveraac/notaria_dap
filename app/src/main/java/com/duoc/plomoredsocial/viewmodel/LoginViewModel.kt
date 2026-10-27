package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.model.LoginError
import com.duoc.plomoredsocial.model.LoginValidator
import com.duoc.plomoredsocial.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: LoginError? = null,
    val passwordError: LoginError? = null,
    val error: LoginError? = null,
    val loading: Boolean = false,
    val validationAttempted: Boolean = false,
    val user: AuthUser? = null
)

// La sesión vive en memoria durante el proceso; nunca persistimos la contraseña.
class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    var uiState by mutableStateOf(LoginUiState())
        private set

    fun updateEmail(value: String) {
        if (uiState.loading) return
        uiState = uiState.copy(email = value, error = null,
            emailError = if (uiState.validationAttempted) LoginValidator.emailError(value) else null)
    }

    fun updatePassword(value: String) {
        if (uiState.loading) return
        uiState = uiState.copy(password = value, error = null,
            passwordError = if (uiState.validationAttempted && value.isEmpty()) LoginError.PASSWORD_REQUIRED else null)
    }

    fun login() {
        if (uiState.loading || uiState.user != null) return
        val emailError = LoginValidator.emailError(uiState.email)
        val passwordError = if (uiState.password.isEmpty()) LoginError.PASSWORD_REQUIRED else null
        uiState = uiState.copy(emailError = emailError, passwordError = passwordError,
            error = null, validationAttempted = true)
        if (emailError != null || passwordError != null) return
        val email = uiState.email.trim()
        val password = uiState.password // Los espacios pueden formar parte de una contraseña.
        uiState = uiState.copy(loading = true)
        viewModelScope.launch {
            try {
                val user = repository.authenticate(email, password)
                uiState = uiState.copy(loading = false, user = user,
                    password = "", error = if (user == null) LoginError.INVALID_CREDENTIALS else null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                uiState = uiState.copy(loading = false, password = "", error = LoginError.UNAVAILABLE)
            }
        }
    }

    fun clearForm() {
        if (!uiState.loading) uiState = LoginUiState(user = uiState.user)
    }

    fun logout() { uiState = LoginUiState() }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LoginViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
    }
}
