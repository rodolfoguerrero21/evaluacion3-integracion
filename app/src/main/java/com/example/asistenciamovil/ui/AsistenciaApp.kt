package com.example.asistenciamovil.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.asistenciamovil.BuildConfig
import com.example.asistenciamovil.R
import com.example.asistenciamovil.data.AttendanceRecord
import com.example.asistenciamovil.data.StatusOption
import com.example.asistenciamovil.data.UserSession
import com.example.asistenciamovil.data.WorkerSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun AsistenciaApp(appViewModel: AppViewModel = viewModel()) {
    val state by appViewModel.uiState.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val session = state.session
        when {
            session == null -> LoginScreen(
                state = state.login,
                onEmailChange = appViewModel::updateEmail,
                onPasswordChange = appViewModel::updatePassword,
                onLogin = appViewModel::login,
            )

            session.esAdministrador -> AdminScreen(
                session = session,
                admin = state.admin,
                attendance = state.attendance,
                onLogout = appViewModel::logout,
                onStatusChange = appViewModel::updateStatus,
                onRutFilterChange = appViewModel::updateRutFilter,
                onLoadWorkers = appViewModel::loadWorkers,
                onSelectWorker = appViewModel::selectWorker,
                onDateChange = appViewModel::updateDate,
                onConsult = appViewModel::consultAttendance,
            )

            else -> WorkerScreen(
                session = session,
                attendance = state.attendance,
                onLogout = appViewModel::logout,
                onDateChange = appViewModel::updateDate,
                onConsult = appViewModel::consultAttendance,
            )
        }
    }
}

@Composable
private fun LoginScreen(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        item {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth().testTag("login_email"),
                label = { Text(stringResource(R.string.email_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                enabled = !state.isLoading,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth().testTag("login_password"),
                label = { Text(stringResource(R.string.password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onLogin() }),
                singleLine = true,
                enabled = !state.isLoading,
            )
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorText(it)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("login_submit"),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.login_action))
                }
            }
            if (BuildConfig.DEBUG && BuildConfig.API_BASE_URL.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.configured_server, BuildConfig.API_BASE_URL),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerScreen(
    session: UserSession,
    attendance: AttendanceState,
    onLogout: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onConsult: () -> Unit,
) {
    Scaffold(topBar = { AppTopBar(stringResource(R.string.worker_title), onLogout) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { UserHeader(session) }
            item { DateSelector(attendance.date, onDateChange) }
            item { ConsultButton(attendance.isLoading, onConsult) }
            item { AttendanceContent(attendance) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminScreen(
    session: UserSession,
    admin: AdminState,
    attendance: AttendanceState,
    onLogout: () -> Unit,
    onStatusChange: (String) -> Unit,
    onRutFilterChange: (String) -> Unit,
    onLoadWorkers: () -> Unit,
    onSelectWorker: (WorkerSummary) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onConsult: () -> Unit,
) {
    Scaffold(topBar = { AppTopBar(stringResource(R.string.admin_title), onLogout) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { UserHeader(session) }
            item {
                StatusSelector(
                    selected = admin.status,
                    options = admin.statusOptions,
                    onSelected = onStatusChange,
                )
            }
            item {
                OutlinedTextField(
                    value = admin.rutFilter,
                    onValueChange = onRutFilterChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_rut_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onLoadWorkers() }),
                )
            }
            item {
                OutlinedButton(
                    onClick = onLoadWorkers,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !admin.isLoadingWorkers,
                ) {
                    if (admin.isLoadingWorkers) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.filter_users))
                    }
                }
            }
            admin.workersError?.let { error -> item { ErrorText(error) } }
            if (!admin.isLoadingWorkers && admin.workers.isEmpty() && admin.workersError == null) {
                item { EmptyMessage(stringResource(R.string.no_users)) }
            }
            items(admin.workers, key = { it.id }) { worker ->
                WorkerCard(
                    worker = worker,
                    selected = admin.selectedWorker?.id == worker.id,
                    onClick = { onSelectWorker(worker) },
                )
            }
            admin.selectedWorker?.let { worker ->
                item {
                    Text(
                        text = stringResource(R.string.selected_worker),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.user_detail_format, worker.nombre, worker.rut.orEmpty()))
                }
                item { DateSelector(attendance.date, onDateChange) }
                item { ConsultButton(attendance.isLoading, onConsult) }
                item { AttendanceContent(attendance) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String, onLogout: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        actions = { TextButton(onClick = onLogout) { Text(stringResource(R.string.logout)) } },
    )
}

@Composable
private fun UserHeader(session: UserSession) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(session.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.user_detail_format, session.rut.orEmpty(), session.rol))
        }
    }
}

@Composable
private fun StatusSelector(
    selected: String,
    options: List<StatusOption>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val allOption = StatusOption("TODOS", stringResource(R.string.all_statuses))
    val available = listOf(allOption) + options
    val selectedName = available.firstOrNull { it.valor == selected }?.nombre ?: selected
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.account_status), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedName)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                available.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.nombre) },
                        onClick = {
                            expanded = false
                            onSelected(option.valor)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerCard(worker: WorkerSummary, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(worker.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(worker.rut ?: stringResource(R.string.time_not_registered))
            Text(stringResource(R.string.user_detail_format, worker.rol, worker.estado))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.date_label), style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(date.format(DATE_FORMATTER))
        }
    }
    if (showDialog) {
        val initialMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDialog = false
                    },
                ) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun ConsultButton(isLoading: Boolean, onConsult: () -> Unit) {
    Button(
        onClick = onConsult,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(R.string.consult))
        }
    }
}

@Composable
private fun AttendanceContent(state: AttendanceState) {
    state.error?.let { ErrorText(it) }
    state.result?.let { result ->
        if (result.registros.isEmpty()) {
            EmptyMessage(stringResource(R.string.no_records))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                result.registros.forEach { AttendanceCard(it) }
            }
        }
    }
}

@Composable
private fun AttendanceCard(record: AttendanceRecord) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(record.estadoNombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DetailRow(stringResource(R.string.entry), record.horaEntrada ?: stringResource(R.string.time_not_registered))
            DetailRow(stringResource(R.string.exit), record.horaSalida ?: stringResource(R.string.time_not_registered))
            if (record.observacion.isNotBlank()) {
                DetailRow(stringResource(R.string.observation), record.observacion)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun ErrorText(error: UiText) {
    Text(
        text = error.asString(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun EmptyMessage(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(18.dp))
    }
}

@Composable
private fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(id)
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
