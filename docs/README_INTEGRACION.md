# Ejecución de la integración móvil

## Componentes

- Android: Kotlin, Jetpack Compose Material 3, ViewModel/StateFlow y OkHttp.
- Servidor: Django 6.1.3 con los modelos y sesiones existentes.
- Autenticación: cookie de sesión Django más protección CSRF.
- Base URL: se inyecta en `BuildConfig.API_BASE_URL` desde `local.properties`; ninguna pantalla fija una dirección.

## Preparar Django en Windows

Desde la carpeta del proyecto Django:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe manage.py migrate
$env:DJANGO_DEBUG = "True"
$env:DJANGO_ALLOWED_HOSTS = "127.0.0.1,localhost"
.\.venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000
```

La base normal sigue siendo `db.sqlite3`. Para pruebas aisladas se puede definir `DJANGO_DB_PATH` con otra ruta antes de ejecutar migraciones o pruebas; esto evita modificar datos reales.

## Configurar Android

Agregar en el `local.properties` local, que está ignorado por Git:

```properties
ASISTENCIA_API_BASE_URL=http\://127.0.0.1\:8000
```

Para el escenario realmente verificado —Django en Windows y emulador— se utilizó redirección ADB:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb reverse tcp:8000 tcp:8000
.\gradlew.bat installDebug
```

Con `adb reverse`, `127.0.0.1:8000` dentro del emulador se redirige al puerto 8000 de Windows. La regla dura mientras el dispositivo/emulador y ADB mantienen la conexión.

El HTTP sin cifrar está permitido únicamente en el manifiesto `debug`; el manifiesto de producción no desactiva globalmente la seguridad TLS.

## Otros escenarios no verificados

- **Emulador y Django en Windows:** `10.0.2.2` suele representar al anfitrión en el emulador estándar, pero en este AVD concreto produjo espera agotada. La configuración comprobada fue `adb reverse`.
- **Django en Ubuntu virtualizado:** se necesita la IP realmente alcanzable de la VM o un reenvío de puertos del hipervisor. Django debe escuchar en una interfaz accesible y `DJANGO_ALLOWED_HOSTS` debe incluir el host usado. `127.0.0.1` de Ubuntu no es el teléfono ni Windows.
- **Teléfono físico:** el teléfono y el servidor deben tener una ruta de red común; normalmente se usa la IP LAN del equipo/VM y se revisan firewall, puerto y hosts permitidos. También puede usarse `adb reverse` por USB para desarrollo.
- **Producción:** se debe usar HTTPS válido y mantener activadas las cookies seguras. No se debe reutilizar la excepción HTTP de depuración.

## Compilar

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Demostración

1. Crear en Django datos de demostración aislados: un trabajador activo, un administrador según la regla real y marcaciones del trabajador.
2. Iniciar el servidor y aplicar `adb reverse`.
3. Ingresar como trabajador, elegir una fecha con marcación y otra sin registros, y cerrar sesión.
4. Confirmar que Atrás no devuelve a una pantalla protegida.
5. Ingresar como administrador, cambiar el estado, seleccionar un trabajador y consultar por fecha.
6. Probar contraseña incorrecta y una cuenta suspendida/inactiva.

No se guardan contraseñas. Las cookies de sesión viven solo en memoria y se eliminan al cerrar sesión o detectar sesión vencida.
