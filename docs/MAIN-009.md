# MAIN-009 — API REST externa Xano

## Estado verificable (20 de noviembre de 2026)

Se trabaja en `main`. Al comenzar esta continuación había cambios sin commit
de la integración previa y se conservaron. No se creó otra rama, push ni PR.

El grupo indicado usa la base
`https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/`. El OpenAPI proporcionado y
el Swagger HTML del grupo respondieron HTTP 404. `GET /actividades` respondió
HTTP 200 con `[]`: confirma la ruta y el contenedor JSON, pero no los campos
de un registro. Las cinco rutas CRUD de actividades y la cifra de 22 endpoints
provienen de la información del proyecto aportada por el usuario. Las otras
17 rutas no se identificaron mediante un contrato de este grupo.

| Método | Ruta | Uso en Mygos | Evidencia |
| --- | --- | --- | --- |
| GET | `/actividades` | Sección Actividades en línea de Explorar | HTTP 200 con `[]` en consulta real; MockWebServer aprobado |
| GET | `/actividades/{actividades_id}` | Consulta manual por ID remoto en Explorar | MockWebServer aprobado; sin llamada a Xano por falta de ID real |
| POST | `/actividades` | No habilitado | Esquema de entrada desconocido y escritura reportada pública |
| PATCH | `/actividades/{actividades_id}` | No habilitado | Escritura reportada pública |
| DELETE | `/actividades/{actividades_id}` | No habilitado | Escritura destructiva reportada pública |

Hay dos combinaciones método/ruta implementadas en el cliente y **una** llamada
real comprobada contra Xano. Faltan cinco combinaciones consumidas en la app
para llegar a siete. MockWebServer no equivale a una llamada real a Xano.

Room continúa en versión 6 y conserva usuarios, actividades, cupos,
inscripciones y administración local. El JSON remoto se mantiene separado y
no se inserta en Room. Los IDs locales no se envían como identidades Xano.
No se configuró autenticación remota ni se conceden permisos ADMIN por rol local.
La pantalla conserva la identidad visual y muestra carga, vacío, error y reintento.

La comprobación ejecutada fue
`.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --rerun-tasks --no-daemon`:
BUILD SUCCESSFUL, 26 pruebas unitarias aprobadas, 0 fallos; Kotlin de las
pruebas instrumentadas compiló. Las pruebas HTTP simuladas cubren GET de lista
y detalle, JSON genérico, 401, 403, 404, 500, cuerpo vacío y timeout.
ADB sigue fallando con `Cannot mkdir '\.android': Permission denied`; no se
ejecutaron pruebas instrumentadas ni la pantalla en el emulador.

## Pendientes para los criterios de aceptación

1. Exportar el OpenAPI JSON de este grupo sin token ni secreto, o compartir la
   lista completa de rutas, esquemas de request/response y permisos de Xano.
2. Identificar cinco rutas adicionales útiles y demostrar su consumo real.
3. Proteger las escrituras en Xano con identidad y autorización del servidor;
   probar POST solo con registros ficticios en un entorno de prueba controlado.
4. Verificar el flujo visual en un dispositivo o emulador cuando ADB funcione.
5. Un microservicio Spring Boot, si la pauta lo exige, sigue siendo un requisito
   separado que esta integración de Xano no cumple.

## Texto para la tarjeta Trello existente

**MAIN-009 — Implementar y verificar APIs REST externas con Xano y Retrofit**

- Retrofit 3.0.0, OkHttp 5.5.1 y conversor Gson configurados.
- Cliente, DTO JSON sin campos supuestos, repositorio y ViewModel remotos implementados.
- GET de actividades comprobado contra Xano: HTTP 200, lista vacía.
- GET de detalle disponible en Explorar y verificado con MockWebServer; sin ID real para prueba contra Xano.
- Sección Actividades en línea integrada visualmente en Explorar; Room permanece independiente.
- 26 pruebas unitarias aprobadas; compilación de instrumentadas aprobada.
- OpenAPI/Swagger de este grupo: HTTP 404. Faltan cinco endpoints consumidos para el mínimo de siete.
- POST/PATCH/DELETE pendientes por contrato incompleto y escrituras reportadas públicas.
- ADB bloqueado por permiso al crear `\.android`; prueba de emulador pendiente.

**Estado sugerido de la tarjeta:** En curso. No marcar finalizada hasta cumplir
las rutas, seguridad, pruebas reales y revisión visual pendientes.
