# Registro de impacto de cambios

## Situación inicial comprobada

- El proyecto Android era la plantilla Compose inicial y compilaba.
- Django ya tenía usuarios, perfiles, roles, estados y registros de asistencia usados por la web.
- Existía un endpoint JSON de empleados sin autenticación que exponía correo; no cumplía AP-03.
- El modelo permite una sola fila de asistencia por usuario y fecha, con entrada y salida en esa fila.

## Cambios implementados

- API de sesión/CSRF, consulta de asistencia y listado administrativo con permisos en Django.
- Normalización de RUT compartida entre formularios web y API.
- Ruta anterior `/api/empleados/` conservada como alias, ahora protegida y sin correo.
- Base de datos configurable mediante `DJANGO_DB_PATH` para pruebas aisladas.
- Aplicación Compose con login, navegación por rol, consultas, estados de carga/error/vacío, cierre y vencimiento de sesión.
- URL del servidor configurable; HTTP solo habilitado para la variante Android `debug`.
- Pruebas automatizadas de API y documentación del contrato y la integración.

## Impacto técnico y operacional

- No hubo migraciones ni cambios de esquema; los datos y funciones web se reutilizan.
- La sesión móvil aparece en la tabla de sesiones normal de Django y respeta sus reglas de expiración.
- Cambiar el estado de un perfil a suspendido/inactivo invalida su acceso en la siguiente petición protegida.
- La operación diaria sigue en la web; la app solo autentica y consulta, según el alcance solicitado.
- La API reduce datos personales: entrega solo identificación funcional, rol, estado y asistencia necesaria.

## Riesgos y mitigaciones

| Riesgo | Mitigación aplicada | Pendiente operacional |
|---|---|---|
| Dirección distinta entre Windows, VM y teléfono | Base URL configurable y guía por escenario | Medir IP/puerto real de Ubuntu antes de desplegar |
| HTTP local inseguro | Excepción limitada a `debug` | Configurar HTTPS para producción |
| Sesión robada o vencida | Cookies en memoria, CSRF, limpieza al recibir 401/403 de cuenta | Definir política de duración del despliegue |
| Rol interpretado de forma incorrecta | Se reutiliza `es_administrador` del sistema | Mantener una única regla de rol |
| Datos reales alterados por pruebas | `DJANGO_DB_PATH` y SQLite temporal | No usar la base Ubuntu en pruebas |
| RUT formalmente inválido | Se conserva la validación heredada de formato | Evaluar cálculo del dígito verificador como cambio futuro separado |

## Comprobación y trazabilidad

- Backend: 34 pruebas Django aprobadas en base de prueba aislada; incluyen las 20 pruebas web existentes y 14 pruebas nuevas de API.
- Android: compilación debug, pruebas unitarias disponibles y lint aprobados.
- Integración: recorrido Android → API Django → SQLite temporal verificado para trabajador y administrador.
- La matriz detallada está en `docs/MATRIZ_TRAZABILIDAD.md` y los resultados en `docs/PRUEBAS_E_INCIDENCIAS.md`.

## No realizado

- No se desplegó ni modificó la VM Ubuntu.
- No se tocaron datos de la instalación funcional ni se reinició su base.
- No se publicó código ni se subieron credenciales.
- No se amplió la app móvil para registrar, editar o borrar marcaciones.
