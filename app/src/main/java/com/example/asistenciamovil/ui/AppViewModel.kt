package com.example.asistenciamovil.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asistenciamovil.BuildConfig
import com.example.asistenciamovil.R
import com.example.asistenciamovil.data.ApiException
import com.example.asistenciamovil.data.ApiFailureType
import com.example.asistenciamovil.data.AsistenciaApi
import com.example.asistenciamovil.data.AttendanceResult
import com.example.asistenciamovil.data.StatusOption
import com.example.asistenciamovil.data.UserSession
import com.example.asistenciamovil.data.WorkerSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(@param:StringRes val id: Int) : UiText
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

data class AttendanceState(
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val result: AttendanceResult? = null,
    val error: UiText? = null,
)

data class AdminState(
    val status: String = "ACTIVO",
    val statusOptions: List<StatusOption> = emptyList(),
    val rutFilter: String = "",
    val workers: List<WorkerSummary> = emptyList(),
    val selectedWorker: WorkerSummary? = null,
    val isLoadingWorkers: Boolean = false,
    val workersError: UiText? = null,
)

data class AppUiState(
    val session: UserSession? = null,
    val login: LoginState = LoginState(),
    val attendance: AttendanceState = AttendanceState(),
    val admin: AdminState = AdminState(),
)

class AppViewModel : ViewModel() {
    private val api = AsistenciaApi(BuildConfig.API_BASE_URL)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(login = it.login.copy(email = value, error = null)) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(login = it.login.copy(password = value, error = null)) }
    }

    fun login() {
        val loginState = _uiState.value.login
        if (loginState.isLoading) return
        if (loginState.email.isBlank() || loginState.password.isBlank()) {
            _uiState.update { it.copy(login = it.login.copy(error = UiText.Resource(R.string.required_credentials))) }
            return
        }

        _uiState.update { it.copy(login = it.login.copy(isLoading = true, error = null)) }
        viewModelScope.launch {
            try {
                val session = api.login(loginState.email.trim(), loginState.password)
                _uiState.update {
                    it.copy(
                        session = session,
                        login = LoginState(email = loginState.email.trim()),
                        attendance = AttendanceState(),
                        admin = AdminState(),
                    )
                }
                if (session.esAdministrador) loadWorkers() else consultAttendance()
            } catch (error: ApiException) {
                _uiState.update {
                    it.copy(login = it.login.copy(isLoading = false, password = "", error = error.toUiText()))
                }
            }
        }
    }

    fun logout() {
        if (_uiState.value.session == null) return
        viewModelScope.launch {
            try {
                api.logout()
            } catch (_: ApiException) {
                api.clearSession()
            } finally {
                _uiState.value = AppUiState()
            }
        }
    }

    fun updateDate(date: LocalDate) {
        _uiState.update { it.copy(attendance = it.attendance.copy(date = date, result = null, error = null)) }
    }

    fun updateStatus(status: String) {
        _uiState.update {
            it.copy(
                admin = it.admin.copy(status = status, selectedWorker = null, workersError = null),
                attendance = it.attendance.copy(result = null, error = null),
            )
        }
        loadWorkers()
    }

    fun updateRutFilter(rut: String) {
        _uiState.update { it.copy(admin = it.admin.copy(rutFilter = rut, workersError = null)) }
    }

    fun selectWorker(worker: WorkerSummary) {
        _uiState.update {
            it.copy(
                admin = it.admin.copy(selectedWorker = worker),
                attendance = it.attendance.copy(result = null, error = null),
            )
        }
    }

    fun loadWorkers() {
        val current = _uiState.value
        if (current.admin.isLoadingWorkers || current.session?.esAdministrador != true) return
        _uiState.update { it.copy(admin = it.admin.copy(isLoadingWorkers = true, workersError = null)) }
        viewModelScope.launch {
            try {
                val result = api.getWorkers(current.admin.status, current.admin.rutFilter.trim().ifBlank { null })
                _uiState.update {
                    it.copy(
                        admin = it.admin.copy(
                            workers = result.usuarios,
                            statusOptions = result.estadosDisponibles,
                            selectedWorker = result.usuarios.singleOrNull(),
                            isLoadingWorkers = false,
                        ),
                        attendance = it.attendance.copy(result = null, error = null),
                    )
                }
            } catch (error: ApiException) {
                handleProtectedError(error) { state, text ->
                    state.copy(admin = state.admin.copy(isLoadingWorkers = false, workersError = text))
                }
            }
        }
    }

    fun consultAttendance() {
        val current = _uiState.value
        if (current.attendance.isLoading) return
        val rut = if (current.session?.esAdministrador == true) {
            current.admin.selectedWorker?.rut
        } else {
            current.session?.rut
        }
        if (rut.isNullOrBlank()) {
            _uiState.update {
                it.copy(attendance = it.attendance.copy(error = UiText.Dynamic("El usuario no tiene un RUT asignado.")))
            }
            return
        }

        _uiState.update { it.copy(attendance = it.attendance.copy(isLoading = true, error = null, result = null)) }
        viewModelScope.launch {
            try {
                val result = api.getAttendance(rut, current.attendance.date.toString())
                _uiState.update {
                    it.copy(attendance = it.attendance.copy(isLoading = false, result = result))
                }
            } catch (error: ApiException) {
                handleProtectedError(error) { state, text ->
                    state.copy(attendance = state.attendance.copy(isLoading = false, error = text))
                }
            }
        }
    }

    private fun handleProtectedError(
        error: ApiException,
        update: (AppUiState, UiText) -> AppUiState,
    ) {
        if (error.type == ApiFailureType.UNAUTHORIZED || error.errorCode == "CUENTA_NO_ACTIVA") {
            api.clearSession()
            _uiState.value = AppUiState(login = LoginState(error = UiText.Resource(R.string.expired_session)))
        } else {
            _uiState.update { update(it, error.toUiText()) }
        }
    }

    private fun ApiException.toUiText(): UiText = when (type) {
        ApiFailureType.CONFIGURATION -> UiText.Resource(R.string.api_not_configured)
        ApiFailureType.NETWORK -> UiText.Resource(R.string.network_error)
        ApiFailureType.TIMEOUT -> UiText.Resource(R.string.timeout_error)
        ApiFailureType.SERVER -> UiText.Resource(R.string.server_error)
        ApiFailureType.PARSING -> UiText.Resource(R.string.server_error)
        else -> UiText.Dynamic(message)
    }
}
