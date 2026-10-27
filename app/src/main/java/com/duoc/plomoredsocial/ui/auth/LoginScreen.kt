package com.duoc.plomoredsocial.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.LoginError
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); onLogin() }
    MygosScreen {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Filled.Lock, null, Modifier.padding(18.dp).size(30.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.login_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        MygosCard {
            OutlinedTextField(
                value = state.email, onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth().testTag("login_email"),
                label = { Text(stringResource(R.string.login_email)) },
                leadingIcon = { Icon(Icons.Filled.Email, null) },
                singleLine = true, enabled = !state.loading,
                shape = MaterialTheme.shapes.small,
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(loginErrorText(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
            )
            OutlinedTextField(
                value = state.password, onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth().testTag("login_password"),
                label = { Text(stringResource(R.string.login_password)) },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                singleLine = true, enabled = !state.loading,
                shape = MaterialTheme.shapes.small,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = state.passwordError != null,
                supportingText = { state.passwordError?.let { Text(loginErrorText(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = passwordVisible, onCheckedChange = { passwordVisible = it }, enabled = !state.loading)
                Text(stringResource(if (passwordVisible) R.string.login_hide_password else R.string.login_show_password),
                    style = MaterialTheme.typography.bodyMedium)
            }
            state.error?.let { MygosNotice(loginErrorText(it), isError = true) }
            Button(onClick = submit, enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("login_submit"),
                shape = MaterialTheme.shapes.small) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(stringResource(if (state.loading) R.string.login_loading else R.string.login_submit))
            }
        }
        Text(stringResource(R.string.login_no_account), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRegister, enabled = !state.loading) {
            Text(stringResource(R.string.nav_register))
        }
    }
}

@Composable
private fun loginErrorText(error: LoginError): String = stringResource(when (error) {
    LoginError.EMAIL_REQUIRED -> R.string.login_email_required
    LoginError.EMAIL_FORMAT -> R.string.login_email_format
    LoginError.PASSWORD_REQUIRED -> R.string.login_password_required
    LoginError.INVALID_CREDENTIALS -> R.string.login_invalid
    LoginError.UNAVAILABLE -> R.string.login_unavailable
})
