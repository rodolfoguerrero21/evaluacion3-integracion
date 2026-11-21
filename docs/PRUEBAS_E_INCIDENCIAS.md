# Pruebas e incidencias

Fecha de ejecución: 6 de noviembre de 2026.

## Entorno verificado

- Servidor Django 6.1.3 ejecutado en Windows en `127.0.0.1:8000`.
- SQLite temporal en `.tmp/integration.sqlite3`; la base `db.sqlite3` recuperada no se usó para la integración.
- Emulador `Pixel_7`, conectado como `emulator-5556`.
- Conexión comprobada mediante `adb reverse tcp:8000 tcp:8000` y URL Android `http://127.0.0.1:8000`.
- Datos ficticios creados solo en la base temporal.

## Resultados automatizados

| Comando | Resultado real |
|---|---|
| `.\.venv\Scripts\python.exe manage.py test` | 34 pruebas aprobadas; base de prueba aislada |
| `.\.venv\Scripts\python.exe manage.py check` | Sin incidencias |
| `.\.venv\Scripts\python.exe manage.py makemigrations --check --dry-run` | Sin cambios de modelo pendientes |
| `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` | BUILD SUCCESSFUL |

## Casos funcionales ejecutados

| Caso | Requisito | Resultado observado |
|---|---|---|
| Login de trabajador activo | GU-04/AP-03 | Aprobado; abrió solo su consulta |
| Login de administrador por rol real | GU-04/AP-03 | Aprobado; abrió consulta administrativa |
| Contraseña incorrecta/usuario inexistente | GU-04 | Aprobado; mensaje de credenciales, HTTP 401 |
| Cuenta inactiva | GU-04 | Aprobado; mensaje de cuenta suspendida o inactiva, HTTP 403 |
| API sin sesión | AP-02/AP-03 | Aprobado; HTTP 401 |
| Trabajador consulta su día con entrada y salida | GU-05/AP-02 | Aprobado; se mostraron 09:42, 17:38 y estado Atraso desde SQLite temporal |
| Trabajador intenta otro RUT | AP-02 | Aprobado en prueba automatizada; HTTP 403 |
| Día sin registros | GU-05 | Aprobado; mensaje vacío y arreglo `[]` |
| Trabajador abre listado administrativo | GU-05 | Rechazado correctamente; HTTP 403 |
| Filtros administrativos reales | GU-05 | Aprobado para Activo, Suspendido e Inactivo en API; Activo/Inactivo también inspeccionados en UI |
| Administrador consulta otro trabajador | GU-05/AP-02 | Aprobado de extremo a extremo |
| RUT/fecha/estado inválidos y RUT inexistente | AP-02 | Aprobado; HTTP 400/404 según contrato |
| Logout y botón Atrás | GU-04/GU-06 | Aprobado; no reapareció pantalla protegida |
| Sesión vencida | GU-04/AP-03 | Aprobado en prueba automatizada; HTTP 401 y limpieza prevista en ViewModel |
| Cambio de filtro tras consultar | GU-05/GU-06 | Aprobado tras corrección; no queda visible el resultado del trabajador anterior |
| Selector de fecha en español | GU-06 | Aprobado; título, mes y botones en español |

## Incidencias encontradas

1. `10.0.2.2:8000` produjo espera agotada en el AVD aunque el servidor alcanzó a recibir una solicitud. Se sustituyó por `adb reverse`, que quedó verificado. Esto no demuestra conectividad con Ubuntu.
2. Un AVD ya existente estaba bloqueado por otra ejecución. Se usó un segundo AVD disponible sin eliminar datos.
3. Dos emuladores simultáneos saturaron el equipo y provocaron una ANR transitoria. Al cerrar el emulador sobrante, el inicio fue normal.
4. La pantalla administrativa conservaba inicialmente el resultado anterior al cambiar de estado. Se corrigió limpiando resultado/error al modificar o recargar el filtro y se volvió a comprobar.

## Pendientes honestos

- No se comprobó la IP, puerto, inicio automático ni red de la VM Ubuntu.
- No se probó un teléfono físico.
- No se forzó una respuesta HTTP 500 real; sí se comprobaron errores 400/401/403/404 y espera agotada de red.
- Las capturas de `.tmp/screens` son evidencia local de desarrollo, no se presentan como capturas de la VM productiva.
