package com.duoc.plomoredsocial.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.repository.*
import com.duoc.plomoredsocial.ui.components.*
import com.duoc.plomoredsocial.viewmodel.AdminViewModel

@Composable
fun AdminRoute(user: AuthUser?) {
    // También bloquea una ruta abierta directamente o recuperada desde navegación.
    if (user?.isAdmin != true) {
        MygosScreen { MygosNotice("Acceso restringido a administradores.", isError = true) }
        return
    }
    val context = LocalContext.current.applicationContext
    val factory = remember(context, user.id) {
        AdminViewModel.Factory(AdminRepository(MygosDatabase.getInstance(context), LocalSessionStore(context.noBackupFilesDir)))
    }
    val model: AdminViewModel = viewModel(key = "admin-${user.id}", factory = factory)
    AdminScreen(user, model)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdminScreen(user: AuthUser?, model: AdminViewModel) {
    if (user?.isAdmin != true || model.denied) {
        MygosScreen { MygosNotice("Acceso restringido a administradores.", isError = true) }
        return
    }
    var tab by rememberSaveable(user.id) { mutableStateOf(0) }
    var selectedId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var action by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    val state = model.snapshot
    val close = { selectedId = null; action = null; model.clearMessage() }
    val select: (String, String) -> Unit = { id, operation ->
        model.clearMessage(); selectedId = id; action = operation
    }
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("admin_panel"),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Panel de administración", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text("Administración local de Mygos")
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Usuarios") }, modifier = Modifier.testTag("admin_users_tab"))
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Actividades") }, modifier = Modifier.testTag("admin_activities_tab"))
                }
            }
        }
        if (model.loading) item { CircularProgressIndicator() }
        if (action == null && model.message != null) item { MygosNotice(requireNotNull(model.message), isError = true) }
        if (state != null && tab == 0) {
            if (state.users.isEmpty()) item { MygosNotice("No hay usuarios guardados.") }
            items(state.users, key = { it.id }) { account ->
                MygosCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(account.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(account.role, Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Text("ID: ${account.id}")
                    Text(account.email)
                    Text("Contraseña protegida · Sin visualización", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { select(account.id, "edit") }, modifier = Modifier.fillMaxWidth().testTag("admin_edit_${account.id}")) {
                        Text(if (account.toAuthUser().isAdmin) "Gestionar cuenta" else "Editar")
                    }
                    TextButton(onClick = { select(account.id, "password") }, modifier = Modifier.fillMaxWidth().testTag("admin_password_${account.id}")) {
                        Text("Restablecer contraseña")
                    }
                    if (account.toAuthUser().isAdmin) Text("Último administrador protegido", color = MaterialTheme.colorScheme.primary)
                    else OutlinedButton(onClick = { select(account.id, "user-delete") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_delete_user_${account.id}")) { Text("Eliminar usuario") }
                }
            }
        }
        if (state != null && tab == 1) {
            if (state.activities.isEmpty()) item { MygosNotice("No hay actividades guardadas.") }
            items(state.activities, key = { it.id }) { activity ->
                MygosCard {
                    Text(activity.title, style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${activity.id}")
                    val organizer = state.users.firstOrNull { it.id == activity.organizerId }
                    Text("Organizador: ${organizer?.name ?: "Cuenta histórica"} · ${activity.organizerId}")
                    Text(activity.schedule)
                    Text(activity.commune)
                    Text("Participantes: ${activity.participantCount(state.members)}/${activity.capacity}")
                    OutlinedButton(onClick = { select(activity.id, "activity-delete") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_delete_activity_${activity.id}")) { Text("Eliminar actividad") }
                }
            }
        }
    }
    val account = state?.users?.firstOrNull { it.id == selectedId }
    if (account != null && (action == "edit" || action == "password")) {
        AccountEditor(account, action == "password", model, close)
    }
    if (account != null && action == "user-delete") {
        val count = state.activities.count { it.organizerId == account.id }
        AlertDialog(onDismissRequest = { if (!model.saving) close() },
            title = { Text("Eliminar usuario") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (count > 0) "Esta cuenta organiza $count actividad(es). Debes resolverlas primero en Actividades."
                    else "Se eliminará la cuenta ${account.name} y todas sus inscripciones. Esta acción es definitiva.")
                model.message?.let { MygosNotice(it, isError = true) }
            } },
            confirmButton = { TextButton(onClick = { model.deleteUser(account.id, close) },
                enabled = count == 0 && !model.saving) { Text("Eliminar definitivamente") } },
            dismissButton = { TextButton(onClick = close, enabled = !model.saving) { Text("Cancelar") } })
    }
    val activity = state?.activities?.firstOrNull { it.id == selectedId }
    if (activity != null && action == "activity-delete") {
        AlertDialog(onDismissRequest = { if (!model.saving) close() },
            title = { Text("Eliminar actividad") },
            text = { Column {
                Text("Se eliminará «${activity.title}» y sus inscripciones. Esta acción es definitiva.")
                model.message?.let { MygosNotice(it, isError = true) }
            } },
            confirmButton = { TextButton(onClick = { model.deleteActivity(activity.id, close) }, enabled = !model.saving) {
                Text("Eliminar definitivamente")
            } },
            dismissButton = { TextButton(onClick = close, enabled = !model.saving) { Text("Cancelar") } })
    }
}

@Composable
private fun AccountEditor(account: UserAccount, passwordMode: Boolean, model: AdminViewModel, close: () -> Unit) {
    var name by rememberSaveable(account.id) { mutableStateOf(account.name) }
    var email by rememberSaveable(account.id) { mutableStateOf(account.email) }
    // Las contraseñas nuevas viven solo en memoria; no sobreviven al cierre ni a la recreación.
    var password by remember(account.id) { mutableStateOf("") }
    var confirmation by remember(account.id) { mutableStateOf("") }
    val nameError = RegisterValidator.nameError(name)
    val emailError = RegisterValidator.emailError(email)
    val passwordError = RegisterValidator.passwordError(password)
    val confirmationError = RegisterValidator.confirmationError(password, confirmation)
    val valid = if (passwordMode) passwordError == null && confirmationError == null else nameError == null && emailError == null
    Dialog(onDismissRequest = { if (!model.saving) close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            MygosScreen(Modifier.testTag("admin_editor")) {
                Text(if (passwordMode) "Restablecer contraseña" else "Editar usuario", style = MaterialTheme.typography.headlineMedium)
                Text("ID: ${account.id}")
                if (!passwordMode) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre o alias") }, enabled = !model.saving,
                        modifier = Modifier.fillMaxWidth().testTag("admin_name"), singleLine = true,
                        isError = nameError != null, supportingText = { nameError?.let { Text(it) } })
                    OutlinedTextField(email, { email = it }, label = { Text("Correo electrónico") }, enabled = !model.saving,
                        modifier = Modifier.fillMaxWidth().testTag("admin_email"), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailError != null, supportingText = { emailError?.let { Text(it) } })
                } else {
                    MygosNotice("Define una contraseña nueva de 12 a 128 caracteres. La contraseña anterior nunca se muestra.")
                    OutlinedTextField(password, { password = it }, label = { Text("Nueva contraseña") }, enabled = !model.saving,
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_password"), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = { if (password.isNotEmpty()) passwordError?.let { Text(it) } })
                    OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirmar contraseña") }, enabled = !model.saving,
                        modifier = Modifier.fillMaxWidth().testTag("admin_confirm_password"), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = { if (confirmation.isNotEmpty()) confirmationError?.let { Text(it) } })
                }
                model.message?.let { MygosNotice(it, isError = true) }
                MygosPrimaryButton(if (model.saving) "Guardando…" else "Guardar cambios", {
                    if (passwordMode) model.resetPassword(account.id, password) { password = ""; confirmation = ""; close() }
                    else model.editUser(account.id, name, email, close)
                }, enabled = valid && !model.saving, modifier = Modifier.testTag("admin_save"))
                TextButton(onClick = close, enabled = !model.saving) { Text("Cancelar") }
            }
        }
    }
}
