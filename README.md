# Mygos — Aplicación Móvil Android (Kotlin + Jetpack Compose)

Aplicación móvil Android nativa desarrollada en Kotlin para la exploración, creación y gestión comunitaria de actividades y eventos locales. Implementa una arquitectura reactiva MVVM, almacenamiento local en Room, integración de servicios web REST con Xano y Retrofit, mapas interactivos mediante Google Maps SDK y funcionalidades nativas de Android (GPS y Notificaciones).

---

## 📋 1. Funcionalidades Principales

- **Autenticación y Sesión:** Autenticación local persistente (PBKDF2 con salt), registro de usuarios y control de sesiones sin exponer contraseñas en memoria.
- **Catálogo Unificado (Xano como Fuente Central):** Las actividades comunitarias compartidas tienen a Xano como fuente principal de verdad (`GET /actividades`, `POST /actividades`, `PATCH /actividades`, `DELETE /actividades`).
- **Creación y Gestión:** Publicación de panoramas desde el formulario principal de la app directamente en Xano, con validación estricta de cupos en el rango 1–10.
- **Servicios Nativos:** GPS real mediante `FusedLocationProviderClient` y Notificaciones del sistema mediante `NotificationManager`.

---

## 🏗️ 2. Arquitectura y Tecnologías

- **Lenguaje:** Kotlin 2.0.21 / Java JDK 25 (JBR Android Studio)
- **UI Framework:** Jetpack Compose + Material 3 (Typography Plus Jakarta Sans)
- **Arquitectura:** MVVM (Model-View-ViewModel) con Unidirectional Data Flow (UDF)
- **Base de Datos Local:** Room Database versión 6 (para sesiones de usuario y caché local)
- **Red REST:** Retrofit 3.0.0 + OkHttp 5.5.1 + Gson Converter (Xano API)
- **Pruebas:** JUnit 4 + MockWebServer 3 (35 pruebas unitarias aprobadas)
- **Google Maps SDK:** Maps Compose (`com.google.maps.android:maps-compose`)

---

## 🎯 3. Requisitos de la Entrega 3 y Estado Real

| Requisito | Estado Real | Evidencia y Verificación |
|---|---|---|
| **1. Mínimo 7 Endpoints REST** | ✅ Cumplido | 7 endpoints REST con combinaciones distintas de método HTTP y ruta URI consumidos vía Retrofit. |
| **2. Mínimo 2 Pruebas Unitarias** | ✅ Cumplido | **35 pruebas unitarias ejecutadas y aprobadas al 100%**. |
| **3. APK Release Firmado** | ✅ Cumplido | APK generado (`app-release.apk`) y verificado con `apksigner` (Esquema v2, RSA 3072 bits). |
| **4. Repositorio Git Actualizado** | ✅ Cumplido | Cambios consolidados en la rama `main` y sincronizados en `origin/main`. |
| **5. Documentación en README** | ✅ Cumplido | README técnico detallado y actualizado reflejando la arquitectura central en Xano. |
| **6. Ícono Personalizado** | ✅ Cumplido | Configurado como ícono de aplicación adaptativo (`ic_launcher` en `res/mipmap-*`). |
| **7. Dos Recursos Nativos** | ✅ Cumplido | GPS real (`FusedLocationProviderClient`) y Notificaciones del sistema (`NotificationManager`). |

---

## 🌐 4. Tabla de Siete (7) Endpoints REST Diferentes (Xano)

| # | Método | Ruta URI | Descripción / Objetivo | Consumo en Mygos | Estado / Respuesta Evidenciada |
|---|---|---|---|---|---|
| 1 | `GET` | `/actividades` | Catálogo central de actividades remotas. | `XanoApiService.getActivities()` | HTTP 200 OK (Array JSON `[...]`) |
| 2 | `GET` | `/actividades/{actividades_id}` | Detalle de actividad por ID remoto. | `XanoApiService.getActivity(id)` | HTTP 200 OK (Objeto JSON `{...}`) |
| 3 | `POST` | `/actividades` | Creación de nueva actividad en Xano (cupos validados 1–10). | `XanoApiService.createActivity(body)` | HTTP 200 OK (Objeto creado con `id`) |
| 4 | `PATCH` | `/actividades/{actividades_id}` | Actualización parcial de atributos (ej. cupos 1–10). | `XanoApiService.updateActivity(id, body)` | HTTP 200 OK (Objeto actualizado) |
| 5 | `DELETE` | `/actividades/{actividades_id}` | Eliminación remota de actividad por ID. | `XanoApiService.deleteActivity(id)` | HTTP 200 OK (Respuesta nula) |
| 6 | `POST` | `/auth/login` | Autenticación remota por credenciales. | `XanoApiService.login(body)` | HTTP 400 Bad Request / 200 OK |
| 7 | `GET` | `/auth/me` | Consulta de perfil y token remoto. | `XanoApiService.getAuthMe(token)` | HTTP 401 Unauthorized / 200 OK |

---

## 🔒 5. Sincronización Bidireccional y Xano como Fuente Central

- **Integración Bidireccional Real:** La pantalla **Explorar** consulta directamente a Xano (`GET /actividades`) para poblar el catálogo principal de actividades comunitarias en tarjetas estructuradas Material 3.
- **Creación Unificada:** El botón *Crear actividad* de la interfaz principal invoca `POST /actividades` en Xano, valida los cupos en el rango 1–10 y notifica localmente mediante `ActivityCreatedNotifier`.
- **Actualización y Borrado:** Los cambios (`PATCH`) y eliminaciones (`DELETE`) se reflejan en tiempo real contra el servidor.

---

## 📦 6. Compilación, Firma y Publicación

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

# Ejecutar 35 pruebas unitarias
.\gradlew.bat :app:testDebugUnitTest

# Generar APK release firmado
.\gradlew.bat :app:assembleRelease --no-daemon

# Verificar firma con apksigner
& 'C:\Users\Juseppe\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat' verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```
