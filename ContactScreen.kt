// Formulario de Contáctanos y ventana para revisar los datos.
package com.duoc.plomoredsocial.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.domain.contact.ContactError
import com.duoc.plomoredsocial.domain.contact.ContactReason
import com.duoc.plomoredsocial.domain.contact.ContactValidator
import com.duoc.plomoredsocial.ui.theme.PlomoRedSocialTheme

// Pasa a la pantalla los datos del ViewModel y las acciones de los botones.
@Composable
fun ContactScreen(viewModel: ContactViewModel, modifier: Modifier = Modifier) {
    ContactScreenContent(
        state = viewModel.uiState,
        onNameChange = viewModel::updateName,
        onEmailChange = viewModel::updateEmail,
        onReasonChange = viewModel::updateReason,
        onMessageChange = viewModel::updateMessage,
        onContinue = viewModel::continueToReview,
        onDismiss = viewModel::dismissReview,
        onConfirm = viewModel::confirmReview,
        modifier = modifier
    )
}

// Recibe los datos y las acciones por separado para poder usar también la vista previa.
// El formulario permite desplazarse cuando no cabe completo.
@Composable
fun ContactScreenContent(
    state: ContactUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onReasonChange: (ContactReason) -> Unit,
    onMessageChange: (String) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.contact_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.contact_privacy))
        Text(stringResource(R.string.contact_demo_notice))
        // Los campos con error se marcan en rojo.
        // El error y el contador van en líneas separadas para que no se pisen.
        OutlinedTextField(
            value = state.nameOrAlias,
            onValueChange = onNameChange,
            enabled = state.saveStatus != ContactSaveStatus.LOADING,
            label = { Text(stringResource(R.string.contact_name)) },
            singleLine = true,
            isError = state.errors.name != null,
            supportingText = {
                Column {
                    FieldSupport(state.errors.name)
                    Text(stringResource(R.string.contact_counter, state.nameOrAlias.length, ContactValidator.NAME_MAX))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            enabled = state.saveStatus != ContactSaveStatus.LOADING,
            label = { Text(stringResource(R.string.contact_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = state.errors.email != null,
            supportingText = { FieldSupport(state.errors.email) },
            modifier = Modifier.fillMaxWidth()
        )
        Text(stringResource(R.string.contact_reason), style = MaterialTheme.typography.titleMedium)
        // El texto del botón indica qué motivo está seleccionado.
        ContactReason.entries.forEach { reason ->
            TextButton(
                onClick = { onReasonChange(reason) },
                enabled = state.saveStatus != ContactSaveStatus.LOADING,
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(
                    if (state.reason == reason) R.string.contact_reason_selected else R.string.contact_reason_option,
                    stringResource(reason.labelResource())
                ))
            }
        }
        FieldSupport(state.errors.reason)
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            enabled = state.saveStatus != ContactSaveStatus.LOADING,
            label = { Text(stringResource(R.string.contact_message)) },
            minLines = 4,
            isError = state.errors.message != null,
            supportingText = {
                Column {
                    FieldSupport(state.errors.message)
                    Text(stringResource(R.string.contact_counter, state.message.length, ContactValidator.MESSAGE_MAX))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Continuar revisa los campos antes de abrir el resumen.
        Button(
            onClick = onContinue,
            enabled = state.saveStatus != ContactSaveStatus.LOADING && state.saveStatus != ContactSaveStatus.SUCCESS,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.contact_continue))
        }
        // El éxito aparece después de que Room devuelve el ID insertado.
        when (state.saveStatus) {
            ContactSaveStatus.LOADING -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.contact_saving))
            }
            ContactSaveStatus.SUCCESS -> Text(stringResource(R.string.contact_saved))
            ContactSaveStatus.ERROR -> Text(
                stringResource(R.string.contact_save_error),
                color = MaterialTheme.colorScheme.error
            )
            ContactSaveStatus.IDLE -> Unit
        }
    }
    // Aquí se revisan los datos antes de confirmar.
    // Volver a editar conserva los datos. Confirmar los guarda solo en este dispositivo.
    if (state.reviewVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.contact_review_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.contact_review_field, stringResource(R.string.contact_name), state.nameOrAlias.trim()))
                    Text(stringResource(R.string.contact_review_field, stringResource(R.string.contact_email), state.email.trim()))
                    state.reason?.let { reason ->
                        Text(stringResource(R.string.contact_review_field, stringResource(R.string.contact_reason), stringResource(reason.labelResource())))
                    }
                    Text(stringResource(R.string.contact_review_field, stringResource(R.string.contact_message), state.message.trim()))
                    Text(stringResource(R.string.contact_confirm_notice))
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(stringResource(R.string.contact_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.contact_edit)) }
            }
        )
    }
}

// Muestra el mensaje que corresponde al error, usando el color rojo del tema.
@Composable
private fun FieldSupport(error: ContactError?) {
    if (error != null) {
        Text(
            stringResource(when (error) {
                ContactError.REQUIRED -> R.string.contact_error_required
                ContactError.NAME_LENGTH -> R.string.contact_error_name
                ContactError.EMAIL_FORMAT -> R.string.contact_error_email
                ContactError.MESSAGE_LENGTH -> R.string.contact_error_message
            }),
            color = MaterialTheme.colorScheme.error
        )
    }
}

// Busca el texto de cada motivo en los recursos.
private fun ContactReason.labelResource(): Int = when (this) {
    ContactReason.QUERY -> R.string.contact_reason_query
    ContactReason.SUGGESTION -> R.string.contact_reason_suggestion
    ContactReason.PROBLEM -> R.string.contact_reason_problem
    ContactReason.OTHER -> R.string.contact_reason_other
}

// Datos ficticios para ver la pantalla desde Android Studio.
@Preview(showBackground = true)
@Composable
private fun ContactScreenPreview() {
    PlomoRedSocialTheme {
        ContactScreenContent(
            state = ContactUiState(
                nameOrAlias = "Alias de prueba",
                email = "prueba@example.com",
                reason = ContactReason.QUERY,
                message = "Este es un mensaje ficticio de prueba."
            ),
            onNameChange = {},
            onEmailChange = {},
            onReasonChange = {},
            onMessageChange = {},
            onContinue = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}
