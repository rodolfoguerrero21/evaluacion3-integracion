# Matriz de trazabilidad móvil

| Requisito | Implementación principal | Verificación |
|---|---|---|
| GU-04 Autenticación | `AsistenciaApi.login`, `AppViewModel.login`, pantalla `LoginScreen`; sesión Django y CSRF en `asistencia/api.py` | `test_ap03_login_trabajador_y_administrador`, credenciales erróneas/inactivas, flujo manual de login/logout |
| GU-05 Perfil trabajador | Estado de sesión y `WorkerScreen`; API `/api/asistencias/` restringida al RUT propio | Tests de consulta propia, múltiples campos, día vacío y rechazo de otro RUT; flujo manual con fecha |
| GU-05 Perfil administrador | `AdminScreen`; `/api/usuarios/` con estados reales y consulta de terceros | Tests de filtros/permisos; flujo manual Activo/Inactivo, selección y asistencia |
| GU-06 Interfaz | Compose Material 3, selector de fecha, carga, error, vacío, resultados, teclado y cierre | Inspección visual en AVD y compilación/lint |
| AP-02 Asistencia REST | `GET /api/asistencias/?rut=&fecha=`; normalización, fecha ISO y control por solicitante | Tests 401/403/404/400/200 y recorrido real a SQLite temporal |
| AP-03 Autenticación y rol | `GET csrf`, `POST login/logout`, `GET session`; `es_administrador` reutilizado | Tests de sesión, CSRF, trabajador, administrador, cuenta no activa y sesión vencida |

## Componentes Android

- `app/src/main/java/com/example/asistenciamovil/data/AsistenciaApi.kt`: HTTP, JSON, CSRF y clasificación de errores.
- `app/src/main/java/com/example/asistenciamovil/data/SessionCookieJar.kt`: cookies solo en memoria.
- `app/src/main/java/com/example/asistenciamovil/ui/AppViewModel.kt`: estado y reglas de interacción.
- `app/src/main/java/com/example/asistenciamovil/ui/AsistenciaApp.kt`: pantallas Compose y navegación por estado autenticado.
- `app/src/main/res/values/strings.xml`: textos en español.

## Componentes Django

- `asistencia/api.py`: recursos y permisos.
- `asistencia/tests_api.py`: pruebas automatizadas AP-02/AP-03/GU-04/GU-05.
- `usuarios/rut.py`: normalización compartida.
- `asistencia/urls.py`: rutas REST.
- `docs/api_movil.md`: contrato completo.
