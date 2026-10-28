package com.duoc.plomoredsocial.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onLogin: () -> Unit) {
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); viewModel.validate() }
    MygosScreen {
        Text("Crea tu espacio en Mygos", style = MaterialTheme.typography.headlineMedium)
        // El alcance temporal es visible antes de introducir datos.
        MygosNotice("Registro de demostración. Puedes validar el formulario, pero no se crea una cuenta, no se envían ni se guardan tus datos. No ingreses contraseñas.")
        MygosCard {
            OutlinedTextField(
                value = viewModel.name, onValueChange = viewModel::updateName,
                label = { Text("Nombre o alias") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_name"),
                isError = viewModel.nameError != null,
                supportingText = { viewModel.nameError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                })
            )
            OutlinedTextField(
                value = viewModel.email, onValueChange = viewModel::updateEmail,
                label = { Text("Correo electrónico") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_email"),
                isError = viewModel.emailError != null,
                supportingText = { viewModel.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
            MygosPrimaryButton("Validar demostración", submit)
            if (viewModel.validated) MygosNotice("Formulario válido. La demostración terminó: no se creó una cuenta ni se inició sesión.")
        }
        // El acceso usa el login existente y sus cuentas de prueba.
        OutlinedButton(onClick = onLogin,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Iniciar sesión")
        }
    }
}
