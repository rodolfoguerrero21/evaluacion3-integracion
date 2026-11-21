package com.example.asistenciamovil.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class AsistenciaApi(baseUrl: String) {
    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
    private val cookieJar = SessionCookieJar()
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    private var csrfToken: String? = null

    suspend fun login(email: String, password: String): UserSession {
        val token = fetchCsrfToken()
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint("api/auth/login/"))
            .header("X-CSRFToken", token)
            .post(body)
            .build()
        val json = executeJson(request)
        csrfToken = json.getString("csrf_token")
        return parseUser(json.getJSONObject("usuario"))
    }

    suspend fun logout() {
        val token = csrfToken ?: fetchCsrfToken()
        val request = Request.Builder()
            .url(endpoint("api/auth/logout/"))
            .header("X-CSRFToken", token)
            .post(ByteArray(0).toRequestBody(null))
            .build()
        executeJson(request)
        clearSession()
    }

    suspend fun getAttendance(rut: String, fecha: String): AttendanceResult {
        val url = endpoint("api/asistencias/").newBuilder()
            .addQueryParameter("rut", rut)
            .addQueryParameter("fecha", fecha)
            .build()
        val json = executeJson(Request.Builder().url(url).get().build())
        val trabajador = json.getJSONObject("trabajador")
        val recordsJson = json.getJSONArray("registros")
        return AttendanceResult(
            trabajadorNombre = trabajador.getString("nombre"),
            trabajadorRut = trabajador.getString("rut"),
            fecha = json.getString("fecha"),
            registros = recordsJson.mapObjects { record ->
                AttendanceRecord(
                    id = record.getLong("id"),
                    horaEntrada = record.nullableString("hora_entrada"),
                    horaSalida = record.nullableString("hora_salida"),
                    estado = record.getString("estado"),
                    estadoNombre = record.getString("estado_nombre"),
                    observacion = record.optString("observacion"),
                )
            },
        )
    }

    suspend fun getWorkers(estado: String, rut: String?): WorkersResult {
        val urlBuilder = endpoint("api/usuarios/").newBuilder().addQueryParameter("estado", estado)
        if (!rut.isNullOrBlank()) {
            urlBuilder.addQueryParameter("rut", rut)
        }
        val json = executeJson(Request.Builder().url(urlBuilder.build()).get().build())
        return WorkersResult(
            usuarios = json.getJSONArray("usuarios").mapObjects { user ->
                WorkerSummary(
                    id = user.getLong("id"),
                    nombre = user.getString("nombre"),
                    rut = user.nullableString("rut"),
                    rol = user.getString("rol"),
                    estado = user.getString("estado"),
                )
            },
            estadosDisponibles = json.getJSONArray("estados_disponibles").mapObjects { status ->
                StatusOption(status.getString("valor"), status.getString("nombre"))
            },
        )
    }

    fun clearSession() {
        csrfToken = null
        cookieJar.clear()
    }

    private suspend fun fetchCsrfToken(): String {
        val request = Request.Builder().url(endpoint("api/auth/csrf/")).get().build()
        return executeJson(request).getString("csrf_token").also { csrfToken = it }
    }

    private fun endpoint(path: String) = if (normalizedBaseUrl.isBlank()) {
        throw ApiException(
            ApiFailureType.CONFIGURATION,
            "La dirección de la API no está configurada.",
        )
    } else {
        "$normalizedBaseUrl/${path.trimStart('/')}".toHttpUrl()
    }

    private suspend fun executeJson(request: Request): JSONObject = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                parseResponse(response)
            }
        } catch (error: ApiException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw ApiException(ApiFailureType.TIMEOUT, "El servidor tardó demasiado en responder.")
        } catch (error: IOException) {
            throw ApiException(ApiFailureType.NETWORK, "No fue posible conectar con el servidor.")
        } catch (error: JSONException) {
            throw ApiException(ApiFailureType.PARSING, "La respuesta del servidor no tiene el formato esperado.")
        }
    }

    private fun parseResponse(response: Response): JSONObject {
        val rawBody = response.body.string()
        val json = try {
            if (rawBody.isBlank()) JSONObject() else JSONObject(rawBody)
        } catch (_: JSONException) {
            if (!response.isSuccessful) {
                throw ApiException(failureType(response.code), "El servidor respondió con un error.", response.code)
            }
            throw ApiException(ApiFailureType.PARSING, "La respuesta del servidor no tiene el formato esperado.")
        }
        if (!response.isSuccessful) {
            val errorJson = json.optJSONObject("error")
            val message = errorJson?.optString("mensaje")
                ?.takeIf { it.isNotBlank() }
                ?: "El servidor respondió con un error."
            val errorCode = errorJson?.optString("codigo")?.takeIf { it.isNotBlank() }
            throw ApiException(failureType(response.code), message, response.code, errorCode)
        }
        return json
    }

    private fun failureType(statusCode: Int) = when (statusCode) {
        401 -> ApiFailureType.UNAUTHORIZED
        403 -> ApiFailureType.FORBIDDEN
        in 500..599 -> ApiFailureType.SERVER
        else -> ApiFailureType.API
    }

    private fun parseUser(json: JSONObject) = UserSession(
        id = json.getLong("id"),
        nombre = json.getString("nombre"),
        rut = json.nullableString("rut"),
        rol = json.getString("rol"),
        estado = json.getString("estado"),
        esAdministrador = json.getBoolean("es_administrador"),
    )

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
