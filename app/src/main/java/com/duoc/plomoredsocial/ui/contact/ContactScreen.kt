// Formulario de Contáctanos y ventana para revisar los datos.
package com.duoc.plomoredsocial.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.viewmodel.ContactViewModel
import com.duoc.plomoredsocial.viewmodel.ContactUiState
import com.duoc.plomoredsocial.viewmodel.ContactSaveStatus
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.ContactError
import com.duoc.plomoredsocial.model.ContactReason
import com.duoc.plomoredsocial.model.ContactValidator
import com.duoc.plomoredsocial.ui.theme.PlomoRedSocialTheme
import com.duoc.plomoredsocial.ui.components.MygosScreen
import com.duoc.plomoredsocial.ui.components.MygosCard
import com.duoc.plomoredsocial.ui.components.MygosNotice
import com.duoc.plomoredsocial.ui.components.MygosPrimaryButton
import com.duoc.plomoredsocial.ui.components.MygosHeader
import com.duoc.plomoredsocial.ui.components.mygosButtonColors

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
        modifier = modifier,
        showTitle = false
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
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        errorContainerColor = MaterialTheme.colorScheme.surface
    )
    MygosScreen(modifier) {
        // La barra superior ya muestra el título; la vista previa puede mostrarlo aquí.
        if (showTitle) Text(
            stringResource(R.string.contact_title),
            style = MaterialTheme.typography.headlineMedium
        )
        MygosHeader("Estamos para escucharte", stringResource(R.string.contact_intro))
        MygosCard {
            Text(
                stringResource(R.string.contact_privacy),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.contact_demo_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Los campos con error se marcan en rojo.
        // El error y el contador van en líneas separadas para que no se pisen.
        OutlinedTextField(
            shape = MaterialTheme.shapes.small,
            colors = fieldColors,
            value = state.nameOrAlias,
            onValueChange = onNameChange,
            enabled = state.saveStatus != ContactSaveStatus.LOADING,
            label = { Text(stringResource(R.string.contact_name)) },
            singleLine = true,
            isError = state.errors.name != null,
            supportingText = {
                Column {
                    FieldSupport(state.errors.name)
                    Text(
                        stringResource(
                            R.string.contact_counter,
                            state.nameOrAlias.length,
                            ContactValidator.NAME_MAX
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            shape = MaterialTheme.shapes.small,
            colors = fieldColors,
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
        // El indicador de selección y el color señalan el motivo elegido.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContactReason.entries.forEach { reason ->
                FilterChip(
                    selected = state.reason == reason,
                    onClick = { onReasonChange(reason) },
                    enabled = state.saveStatus != ContactSaveStatus.LOADING,
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text(stringResource(reason.labelResource())) },
                    leadingIcon = if (state.reason == reason) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
        FieldSupport(state.errors.reason)
        OutlinedTextField(
            shape = MaterialTheme.shapes.small,
            colors = fieldColors,
            value = state.message,
            onValueChange = onMessageChange,
            enabled = state.saveStatus != ContactSaveStatus.LOADING,
            label = { Text(stringResource(R.string.contact_message)) },
            minLines = 4,
            isError = state.errors.message != null,
            supportingText = {
                Column {
                    FieldSupport(state.errors.message)
                    Text(
                        stringResource(
                            R.string.contact_counter,
                            state.message.length,
                            ContactValidator.MESSAGE_MAX
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Continuar revisa los campos antes de abrir el resumen.
        MygosPrimaryButton(
            text = stringResource(R.string.contact_continue),
            onClick = onContinue,
            enabled = state.saveStatus != ContactSaveStatus.LOADING && state.saveStatus != ContactSaveStatus.SUCCESS
        )
        // El éxito aparece después de que Room devuelve el ID insertado.
        when (state.saveStatus) {
            ContactSaveStatus.LOADING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.contact_saving))
                }
            }

            ContactSaveStatus.SUCCESS -> MygosNotice(
                stringResource(R.string.contact_saved),
                icon = Icons.Filled.CheckCircle,
                success = true
            )

            ContactSaveStatus.ERROR -> MygosNotice(
                stringResource(R.string.contact_save_error),
                isError = true,
                icon = Icons.Filled.Warning
            )

            ContactSaveStatus.IDLE -> Unit
        }
    }
    // Aquí se revisan los datos antes de confirmar.
    // Volver a editar conserva los datos. Confirmar los guarda solo en este dispositivo.
    if (state.reviewVisible) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.contact_review_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.contact_review_field,
                            stringResource(R.string.contact_name),
                            state.nameOrAlias.trim()
                        )
                    )
                    Text(
                        stringResource(
                            R.string.contact_review_field,
                            stringResource(R.string.contact_email),
                            state.email.trim()
                        )
                    )
                    state.reason?.let { reason ->
                        Text(
                            stringResource(
                                R.string.contact_review_field,
                                stringResource(R.string.contact_reason),
                                stringResource(reason.labelResource())
                            )
                        )
                    }
                    Text(
                        stringResource(
                            R.string.contact_review_field,
                            stringResource(R.string.contact_message),
                            state.message.trim()
                        )
                    )
                    Text(stringResource(R.string.contact_confirm_notice))
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = mygosButtonColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.contact_confirm))
                }
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
            stringResource(
                when (error) {
                    ContactError.REQUIRED -> R.string.contact_error_required
                    ContactError.NAME_LENGTH -> R.string.contact_error_name
                    ContactError.EMAIL_FORMAT -> R.string.contact_error_email
                    ContactError.MESSAGE_LENGTH -> R.string.contact_error_message
                }
            ),
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
