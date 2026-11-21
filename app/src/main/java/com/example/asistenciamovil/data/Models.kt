package com.example.asistenciamovil.data

data class UserSession(
    val id: Long,
    val nombre: String,
    val rut: String?,
    val rol: String,
    val estado: String,
    val esAdministrador: Boolean,
)

data class AttendanceRecord(
    val id: Long,
    val horaEntrada: String?,
    val horaSalida: String?,
    val estado: String,
    val estadoNombre: String,
    val observacion: String,
)

data class AttendanceResult(
    val trabajadorNombre: String,
    val trabajadorRut: String,
    val fecha: String,
    val registros: List<AttendanceRecord>,
)

data class WorkerSummary(
    val id: Long,
    val nombre: String,
    val rut: String?,
    val rol: String,
    val estado: String,
)

data class StatusOption(val valor: String, val nombre: String)

data class WorkersResult(
    val usuarios: List<WorkerSummary>,
    val estadosDisponibles: List<StatusOption>,
)

enum class ApiFailureType {
    CONFIGURATION,
    NETWORK,
    TIMEOUT,
    UNAUTHORIZED,
    FORBIDDEN,
    SERVER,
    API,
    PARSING,
}

class ApiException(
    val type: ApiFailureType,
    override val message: String,
    val statusCode: Int? = null,
    val errorCode: String? = null,
) : Exception(message)
