package com.duoc.plomoredsocial.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onLogin: () -> Unit) {
    val focus = LocalFocusManager.current
    var visible by remember { mutableStateOf(false) }
    val submit = { focus.clearFocus(); viewModel.register() }
    val editable = !viewModel.loading && viewModel.registeredEmail == null
    // Al salir o recrear la vista se descartan los secretos, conservando nombre/correo en el ViewModel.
    DisposableEffect(viewModel) { onDispose { viewModel.clearSensitive() } }
    MygosScreen {
        MygosBrand()
        MygosHeader("Crea tu espacio en Mygos", "Tus próximos encuentros empiezan con una cuenta.")
        MygosNotice("La cuenta se guarda solo en este dispositivo. No hay sincronización, verificación de correo ni recuperación remota de contraseña.")
        if (viewModel.registeredEmail != null) {
            MygosCard {
                Text("Cuenta creada", style = MaterialTheme.typography.titleLarge)
                Text("Tu cuenta se guardó correctamente. Ya puedes iniciar sesión.")
                MygosPrimaryButton("Iniciar sesión", onLogin)
            }
        } else {
            MygosCard {
                OutlinedTextField(value = viewModel.name, onValueChange = viewModel::updateName,
                    label = { Text("Nombre o alias") }, singleLine = true, enabled = editable,
                    modifier = Modifier.fillMaxWidth().testTag("register_name"),
                    isError = viewModel.nameError != null,
                    supportingText = { viewModel.nameError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
                OutlinedTextField(value = viewModel.email, onValueChange = viewModel::updateEmail,
                    label = { Text("Correo electrónico") }, singleLine = true, enabled = editable,
                    modifier = Modifier.fillMaxWidth().testTag("register_email"),
                    isError = viewModel.emailError != null,
                    supportingText = { viewModel.emailError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                        autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
                OutlinedTextField(value = viewModel.password, onValueChange = viewModel::updatePassword,
                    label = { Text("Contraseña") }, singleLine = true, enabled = editable,
                    modifier = Modifier.fillMaxWidth().testTag("register_password"),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = viewModel.passwordError != null,
                    supportingText = { Text(viewModel.passwordError ?: "Usa entre 12 y 128 caracteres. Puedes usar una frase.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
                OutlinedTextField(value = viewModel.confirmation, onValueChange = viewModel::updateConfirmation,
                    label = { Text("Confirmar contraseña") }, singleLine = true, enabled = editable,
                    modifier = Modifier.fillMaxWidth().testTag("register_confirmation"),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = viewModel.confirmationError != null,
                    supportingText = { viewModel.confirmationError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = visible, onCheckedChange = { visible = it }, enabled = editable)
                    Text("Mostrar contraseñas")
                }
                viewModel.error?.let { MygosNotice(it, isError = true) }
                MygosPrimaryButton(if (viewModel.loading) "Creando cuenta…" else "Crear cuenta", submit, editable,
                    Modifier.testTag("register_submit"))
                if (viewModel.loading) CircularProgressIndicator(Modifier.size(24.dp))
            }
            TextButton(onClick = onLogin, enabled = !viewModel.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Iniciar sesión") }
        }
    }
}
