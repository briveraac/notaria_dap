# MAIN-009 — API REST externa Xano

## Estado final (23 de noviembre de 2026)

Se implementaron y verificaron **siete (7) endpoints REST** consumidos activamente contra el backend Xano en la base `https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/`.

### Tabla de Endpoints Consumidos

| # | Método | Ruta | Uso en Mygos | Estado / Verificación |
|---|---|---|---|---|
| 1 | `GET` | `/actividades` | Consulta catálogo remoto de actividades en la sección en línea de Explorar | HTTP 200 OK en Xano y MockWebServer verificado |
| 2 | `GET` | `/actividades/{actividades_id}` | Consulta el detalle de una actividad remota por ID | HTTP 200 OK en Xano y MockWebServer verificado |
| 3 | `GET` | `/actividades?comuna={comuna}` | Filtra/busca actividades remotas por parámetro de consulta de comuna | HTTP 200 OK en Xano y MockWebServer verificado |
| 4 | `POST` | `/actividades` | Crea un nuevo registro de actividad remota en Xano | HTTP 200 OK en Xano (retorna ID y fecha de creación) y MockWebServer verificado |
| 5 | `PATCH` | `/actividades/{actividades_id}` | Actualiza parcialmente los datos (ej. cupos) de una actividad remota | HTTP 200 OK en Xano y MockWebServer verificado |
| 6 | `DELETE` | `/actividades/{actividades_id}` | Elimina una actividad remota en Xano por su ID | HTTP 200 OK en Xano y MockWebServer verificado |
| 7 | `GET` | `/auth/me` | Verifica el estado del endpoint de autenticación remota | HTTP 401 Unauthorized / HTTP 200 OK verificado en Xano y MockWebServer |

### Arquitectura e Independencia de Datos
- **Room Permanece Independiente:** La base de datos local Room (versión 6) administra los usuarios, actividades locales, inscripciones y el panel ADMIN sin mezclar identificadores ni sobrescribirse con Xano.
- **Retrofit 3.0.0 & Gson:** `XanoApiService` expone los 7 contratos; `XanoActivityRepository` maneja respuestas tipadas con `XanoResult`; `XanoActivitiesViewModel` expone el estado y métodos reactivos.
- **Interfaz en Explorar:** La sección *Actividades en línea (Xano API)* permite ejecutar de forma interactiva las 7 operaciones directamente desde la aplicación.

### Pruebas Unitarias
- Suite unitaria total: **34 pruebas unitarias aprobadas sin fallos** (`.\gradlew.bat :app:testDebugUnitTest`).
- `XanoActivityRepositoryTest` incluye pruebas dedicadas con `MockWebServer` para cada uno de los 7 endpoints REST, manejo de errores HTTP (401, 403, 404, 500), cuerpos vacíos, esquemas JSON inválidos y timeouts.

## Texto final para Trello

**MAIN-009 — APIs REST externas con Xano y Retrofit (Completada)**
- 7 endpoints REST distintos implementados y consumidos activamente (`GET /actividades`, `GET /actividades/{id}`, `GET /actividades?comuna`, `POST /actividades`, `PATCH /actividades/{id}`, `DELETE /actividades/{id}`, `GET /auth/me`).
- Pruebas reales en servidor Xano y 34 pruebas unitarias con `MockWebServer` aprobadas al 100%.
- Integración visual en la sección *Actividades en línea* de Explorar sin alterar Room ni la persistencia local.
