package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.model.RegisterValidator
import com.duoc.plomoredsocial.repository.AccountCreatedSessionUnavailableException
import com.duoc.plomoredsocial.repository.AuthRepository
import com.duoc.plomoredsocial.repository.AuthNetworkException
import com.duoc.plomoredsocial.repository.DuplicateEmailException
import com.duoc.plomoredsocial.repository.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// El registro remoto inicia sesión cuando Xano entrega un token válido.
class RegisterViewModel(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore? = null
) : ViewModel() {
    var name by mutableStateOf(""); private set
    var email by mutableStateOf(""); private set
    var password by mutableStateOf(""); private set
    var confirmation by mutableStateOf(""); private set
    var attempted by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var registeredEmail by mutableStateOf<String?>(null); private set
    var registeredUser by mutableStateOf<AuthUser?>(null); private set
    var duplicateEmail by mutableStateOf(false); private set

    val nameError get() = if (attempted) RegisterValidator.nameError(name) else null
    val emailError get() = if (duplicateEmail) "Este correo ya tiene una cuenta."
        else if (attempted) RegisterValidator.emailError(email) else null
    val passwordError get() = if (attempted) RegisterValidator.passwordError(password) else null
    val confirmationError get() = if (attempted) RegisterValidator.confirmationError(password, confirmation) else null
    private val editable get() = !loading && registeredEmail == null

    fun updateName(value: String) { if (editable) { name = value; error = null } }
    fun updateEmail(value: String) { if (editable) { email = value; duplicateEmail = false; error = null } }
    fun updatePassword(value: String) { if (editable) { password = value; error = null } }
    fun updateConfirmation(value: String) { if (editable) { confirmation = value; error = null } }

    fun register() {
        if (!editable) return
        attempted = true; error = null; duplicateEmail = false
        if (nameError != null || emailError != null || passwordError != null || confirmationError != null) return
        val submittedName = name.trim()
        val submittedEmail = RegisterValidator.normalizeEmail(email)
        val submittedPassword = password
        loading = true
        viewModelScope.launch {
            try {
                val session = repository.registerSession(submittedName, submittedEmail, submittedPassword)
                registeredEmail = session.user.email
                clearSensitive()
                attempted = false
                if (session.token != null) {
                    try {
                        requireNotNull(sessionStore).saveToken(session.token)
                        registeredUser = session.user
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = "Tu cuenta se creó en Xano, pero no pudimos guardar la sesión. Inicia sesión."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: DuplicateEmailException) {
                duplicateEmail = true
            } catch (_: AccountCreatedSessionUnavailableException) {
                registeredEmail = submittedEmail
                clearSensitive()
                error = "Tu cuenta se creó en Xano, pero no pudimos confirmar la sesión. Inicia sesión."
            } catch (_: AuthNetworkException) {
                error = "No pudimos conectar con Xano. Revisa tu conexión e intenta nuevamente."
            } catch (_: Exception) {
                error = "No pudimos crear la cuenta en Xano. Revisa los datos e intenta nuevamente."
            } finally { loading = false }
        }
    }

    // Nunca se guardan contraseñas en SavedStateHandle, Bundle ni Room.
    fun clearSensitive() { password = ""; confirmation = "" }
    fun consumeRegisteredUser(): AuthUser? = registeredUser.also { registeredUser = null }
    fun clearForm() {
        if (loading) return
        name = ""; email = ""; clearSensitive()
        attempted = false; duplicateEmail = false; error = null; registeredEmail = null; registeredUser = null
    }

    class Factory(
        private val repository: AuthRepository,
        private val sessionStore: SessionStore? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RegisterViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repository, sessionStore) as T
        }
    }
}
