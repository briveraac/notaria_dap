# MAIN-009 — API REST Externa Xano (Entrega 3)

## Estado Verificable (23 de noviembre de 2026)

Se implementaron y verificaron **siete (7) endpoints REST verdaderamente distintos** (combinaciones únicas de método HTTP y ruta URI, excluyendo parámetros de consulta) consumidos mediante Retrofit en la base `https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/`.

### Tabla de Endpoints REST Únicos Consumidos (7)

| # | Método | Ruta URI | Descripción / Objetivo | Consumo en la App (UI / Repo) | Evidencia de Servidor / Mock |
|---|---|---|---|---|---|
| 1 | `GET` | `/actividades` | Consultar el catálogo completo de actividades remotas. Soporta filtro opcional `?comuna`. | Sección *Actividades en línea* -> `1. Consultar listado (GET /actividades)` | HTTP 200 OK con JSON Array `[...]` devuelto por Xano. |
| 2 | `GET` | `/actividades/{actividades_id}` | Obtener detalle de una actividad específica por su ID remoto. | Sección *Actividades en línea* -> `2. Consultar detalle (GET /actividades/{id})` | HTTP 200 OK con JSON Object `{...}` devuelto por Xano. |
| 3 | `POST` | `/actividades` | Crear una nueva actividad remota en Xano. Se valida la información y se restringen cupos al rango 1–10. | Sección *Actividades en línea* -> `3. Crear actividad (POST /actividades)` | HTTP 200 OK con JSON de objeto creado (`id`, `created_at`, etc.) devuelto por Xano. |
| 4 | `PATCH` | `/actividades/{actividades_id}` | Actualizar atributos (ej. cupos 1–10) de una actividad remota existente. | Sección *Actividades en línea* -> `4. Actualizar actividad (PATCH /actividades/{id})` | HTTP 200 OK con JSON de objeto actualizado devuelto por Xano. |
| 5 | `DELETE` | `/actividades/{actividades_id}` | Eliminar una actividad remota en Xano por su ID. | Sección *Actividades en línea* -> `5. Eliminar actividad (DELETE /actividades/{id})` | HTTP 200 OK con respuesta nula devuelto por Xano. |
| 6 | `POST` | `/auth/login` | Endpoint de autenticación remota mediante credenciales. | Sección *Actividades en línea* -> `6. Autenticación remota (POST /auth/login)` | HTTP 400 Bad Request cuando faltan parámetros o HTTP 200/500 según credenciales en Xano. |
| 7 | `GET` | `/auth/me` | Obtener perfil del usuario autenticado o estado del token remoto. | Sección *Actividades en línea* -> `7. Perfil / Auth Status (GET /auth/me)` | HTTP 401 Unauthorized sin token válido o HTTP 200 OK cuando existe sesión en Xano. |

> **Nota sobre conteo de endpoints:** Parámetros de consulta (como `?comuna=Santiago`) no se cuentan como un endpoint distinto a `GET /actividades`. Cada una de las 7 entradas de la tabla representa una combinación **método + ruta URI única y distinta**.

### Reglas de Negocio y Aislamiento de Datos
- **Room Permanece Independiente:** La base de datos Room local (versión 6) gestiona usuarios, actividades locales, inscripciones y administración sin mezclar IDs locales con IDs de Xano.
- **Validación de Cupos:** La creación (`POST`) y edición (`PATCH`) restringen estrictamente los cupos al rango validado 1–10 (`cupos.coerceIn(1, 10)`).
- **Pruebas Unitarias:** 35 pruebas unitarias aprobadas al 100%, incluyendo pruebas dedicadas con `MockWebServer` para los 7 endpoints REST y manejo de códigos HTTP de error (400, 401, 403, 404, 500), JSON malformados y timeouts.

---

## Texto para Trello (Tarjeta MAIN-009)

**MAIN-009 — API REST externa Xano con Retrofit (Completado)**
- 7 endpoints REST únicos consumidos activamente: `GET /actividades`, `GET /actividades/{id}`, `POST /actividades`, `PATCH /actividades/{id}`, `DELETE /actividades/{id}`, `POST /auth/login`, `GET /auth/me`.
- Validación de entradas y restricción de cupos en el rango 1–10.
- 35 pruebas unitarias con `MockWebServer` aprobadas.
- Room independiente versión 6 sin mezclar identidades.
