package com.duoc.plomoredsocial.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.duoc.plomoredsocial.model.LoginValidator

// Solo valida el formulario de demostración; no crea usuarios ni modifica la sesión.
class RegisterViewModel : ViewModel() {
    var name by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var attempted by mutableStateOf(false)
        private set
    var validated by mutableStateOf(false)
        private set
    val nameError: String? get() = when {
        !attempted -> null
        name.trim().isEmpty() -> "Ingresa tu nombre o alias."
        name.trim().length > 80 -> "Usa hasta 80 caracteres."
        else -> null
    }
    val emailError: String? get() = if (attempted && LoginValidator.emailError(email) != null)
        "Ingresa un correo válido, por ejemplo nombre@correo.cl." else null

    fun updateName(value: String) { name = value; validated = false }
    fun updateEmail(value: String) { email = value; validated = false }
    fun validate() {
        attempted = true
        validated = nameError == null && emailError == null
    }
}
