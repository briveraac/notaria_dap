Mygos — Aplicación Móvil Android (Kotlin + Jetpack Compose)

Aplicación móvil Android nativa desarrollada en Kotlin para la exploración, creación y gestión comunitaria de actividades y eventos locales. Implementa una arquitectura reactiva MVVM, almacenamiento local en Room, integración de servicios web REST con Xano y Retrofit, mapas interactivos mediante Google Maps SDK y funcionalidades nativas de Android (GPS y Notificaciones).

---

Funcionalidades Principales

- **Autenticación y Sesión:** Autenticación local persistente (PBKDF2 con salt), registro de usuarios y control de sesiones sin exponer contraseñas en memoria.
- **Exploración de Actividades:** Catálogo interactivo de actividades locales con filtros por comuna, texto y mapa en tiempo real.
- **Creación y Gestión:** Publicación de panoramas con punto de encuentro, horario, comuna y control de cupos.
- **Participación:** Inscripción y cancelación de cupos con actualización reactiva en la base de datos local.
- **Panel Administrativo:** Pantalla ADMIN para gestionar usuarios, cambiar roles y supervisar actividades e inscripciones.
- **Integración con Google Maps:** Selección visual de ubicaciones en mapa interactivo y apertura de indicaciones.
- **Servicios en Línea (Xano REST):** Sección dedicada para interactuar con la API REST externa mediante Retrofit.

---

Arquitectura y Tecnologías

- **Lenguaje:** Kotlin 2.0.21 / Java JDK 25 (JBR Android Studio)
- **UI Framework:** Jetpack Compose + Material 3 (Typography Plus Jakarta Sans)
- **Arquitectura:** MVVM (Model-View-ViewModel) con Unidirectional Data Flow (UDF)
- **Base de Datos Local:** Room Database versión 6 (`MygosDatabase`, `UserDao`, `ActivityDao`, `ContactDao`)
- **Red REST:** Retrofit 3.0.0 + OkHttp 5.5.1 + Gson Converter
- **Pruebas y Simulación HTTP:** JUnit 4 + MockWebServer 3
- **Google Maps SDK:** Maps Compose (`com.google.maps.android:maps-compose:6.2.1`)
- **Servicios Nativos:** Google Play Services Location (`FusedLocationProviderClient`) y Android `NotificationManager`

---

Requisitos de la Entrega 3 y Estado Real

| Requisito | Estado Real | Evidencia y Verificación |
|---|---|---|
| **1. Mínimo 7 Endpoints REST** | ✅ Cumplido | 7 endpoints REST con combinaciones distintas de método HTTP y ruta URI consumidos vía Retrofit. |
| **2. Mínimo 2 Pruebas Unitarias** | ✅ Cumplido | **35 pruebas unitarias ejecutadas y aprobadas al 100%**. |
| **3. APK Release Firmado** | ✅ Cumplido | APK generado (`app-release.apk`) y verificado con `apksigner` (Esquema v2, RSA 3072 bits). |
| **4. Repositorio Git Actualizado** | ✅ Cumplido | Cambios consolidados en la rama `main` y sincronizados en `origin/main`. |
| **5. Documentación en README** | ✅ Cumplido | README técnico detallado y actualizado reflejando la implementación real. |
| **6. Ícono Personalizado** | ✅ Cumplido | Configurado como ícono de aplicación adaptativo (`ic_launcher` en `res/mipmap-*`). |
| **7. Dos Recursos Nativos** | ✅ Cumplido | GPS real (`FusedLocationProviderClient`) y Notificaciones del sistema (`NotificationManager`). |

---

Tabla de Siete (7) Endpoints REST Diferentes (Xano)

Los endpoints REST consumidos de la API Xano (`https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/`) corresponden a combinaciones **únicas** de método HTTP y ruta URI (sin contar parámetros de consulta):

| # | Método | Ruta URI | Descripción / Objetivo | Consumo en Mygos | Estado / Respuesta Evidenciada |
|---|---|---|---|---|---|
| 1 | `GET` | `/actividades` | Obteción del catálogo general de actividades remotas. | `XanoApiService.getActivities()` | HTTP 200 OK (Array JSON `[...]`) |
| 2 | `GET` | `/actividades/{actividades_id}` | Obteción del detalle de una actividad por su ID remoto. | `XanoApiService.getActivity(id)` | HTTP 200 OK (Objeto JSON `{...}`) |
| 3 | `POST` | `/actividades` | Creación de nueva actividad remota (cupos validados 1–10). | `XanoApiService.createActivity(body)` | HTTP 200 OK (Objeto creado con `id`) |
| 4 | `PATCH` | `/actividades/{actividades_id}` | Actualización parcial de atributos (ej. cupos 1–10). | `XanoApiService.updateActivity(id, body)` | HTTP 200 OK (Objeto actualizado) |
| 5 | `DELETE` | `/actividades/{actividades_id}` | Eliminación remota de una actividad por su ID. | `XanoApiService.deleteActivity(id)` | HTTP 200 OK (Respuesta vacía/nula) |
| 6 | `POST` | `/auth/login` | Endpoint de autenticación remota mediante credenciales. | `XanoApiService.login(body)` | HTTP 400 Bad Request / 200 OK |
| 7 | `GET` | `/auth/me` | Consulta del perfil / estado de sesión del usuario remoto. | `XanoApiService.getAuthMe(token)` | HTTP 401 Unauthorized / 200 OK |

---

Integración de Xano y Separación con Room

- **Aislamiento de Persistencia:** Room (versión 6) administra los usuarios, actividades locales, inscripciones y panel ADMIN de manera totalmente independiente. Las respuestas de Xano no se escriben en Room ni sustituyen los datos locales.
- **IDs Separados:** Los identificadores locales de Room (UUID / IDs autonuméricos) no se confunden ni se envían como IDs remotos de Xano.
- **Validación de Cupos:** La creación (`POST`) y edición (`PATCH`) de actividades remotas valida y restringe estrictamente los cupos al rango **1 a 10** (`cupos.coerceIn(1, 10)`).

---

Dos Recursos Nativos de Android

1. **GPS y Ubicación Real (`FusedLocationProviderClient`)**:
   - Solicita permisos `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` en tiempo de ejecución al pulsar *Usar mi ubicación*.
   - Obtiene las coordenadas actuales del dispositivo/emulador y centra el marcador en el mapa.
   - Si el permiso se deniega o la ubicación está desactivada, muestra una alerta y permite la selección manual en el mapa o ingreso textual.
2. **Notificaciones del Sistema (`NotificationManager`)**:
   - Crea un canal de notificación dedicado (*Actividades creadas*).
   - Solicita `POST_NOTIFICATIONS` en Android 13+.
   - Publica un aviso en la bandeja de notificaciones al guardar exitosamente una actividad local en Room.
   - Si el permiso de notificación se deniega, la actividad se guarda correctamente sin interrumpir el flujo.

---

Pruebas Unitarias e Instrumentadas

### Pruebas Unitarias (JUnit 4 + MockWebServer)
- **Aprobadas:** 35 pruebas unitarias.
- **Fallidas:** 0.
- **Omitidas:** 0.
- **Cobertura:** Proban validadores de formularios (`ActivityFormValidator`, `LoginValidator`, `RegisterValidator`, `ContactValidator`), derivación de contraseñas (`PasswordDeriver`), filtrado de búsquedas, y pruebas de repositorio HTTP con `MockWebServer` para los 7 endpoints de Xano (éxito, 400, 401, 403, 404, 500, cuerpo vacío, JSON malformado y timeout).

### Pruebas Instrumentadas (AndroidTest)
- **Suite:** 67 pruebas instrumentadas en AVD Pixel 7 (Room, navegación, UI, notificaciones e integración).
- **Estado de compilación:** `:app:compileDebugAndroidTestKotlin` pasa exitosamente.

---

## 🗺️ 8. Configuración de Google Maps SDK

Agregar la clave de API en `local.properties` (archivo excluido de Git):
   ```properties
   MAPS_API_KEY=AIzaSy...clave
   ```
El manifiesto de la aplicación (`AndroidManifest.xml`) incluye el metadato:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="${MAPS_API_KEY}" />
   ```
Paquete de la aplicación: `com.duoc.plomoredsocial`.


---

 Compilación Debug y Pruebas

Para ejecutar la compilación debug y la suite unitaria:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

# Compilar variante debug y ejecutar 35 pruebas unitarias
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug

# Compilar pruebas instrumentadas
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

---

Generación del APK Release Firmado

La variante release requiere las propiedades de firma en `local.properties`:

```properties
RELEASE_STORE_FILE=
RELEASE_STORE_PASSWORD=<keystore>
RELEASE_KEY_ALIAS=mygos
RELEASE_KEY_PASSWORD=<contraseña_alias>
```

Para generar el APK release firmado:

```powershell
.\gradlew.bat :app:assembleRelease --no-daemon
```

El APK resultante se ubica en:
`app/build/outputs/apk/release/app-release.apk`



**Resultado esperado:** `Verifies`, `Verified using v2 scheme: true`, algoritmo RSA 3072 bits.

---

Instalación y Prueba del APK

Con un emulador Pixel 7 o dispositivo conectado vía ADB:

```powershell
& 'C:\Users\Juseppe\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r 'app/build/outputs/apk/release/app-release.apk'
```

---

 Estructura Técnica del Proyecto

```
notaria_dap/
├── app/
│   ├── build.gradle.kts (Configuración Gradle, dependencias, signingConfigs)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml (Permisos, metadatos Maps, canal notificaciones)
│   │   │   ├── java/com/duoc/plomoredsocial/
│   │   │   │   ├── data/remote/xano/ (XanoApiService, XanoApiClient, XanoActivityDto)
│   │   │   │   ├── model/ (Room DAOs, Entities, Validators)
│   │   │   │   ├── notifications/ (ActivityCreatedNotifier)
│   │   │   │   ├── repository/ (RoomAuthRepository, XanoActivityRepository)
│   │   │   │   ├── ui/ (ActivitiesScreen, LoginScreen, ExploreScreen, AdminScreen)
│   │   │   │   └── viewmodel/ (ActivitiesViewModel, XanoActivitiesViewModel, LoginViewModel)
│   │   │   └── res/ (mipmap-* launcher icons, drawables, fonts)
│   │   └── test/ (35 pruebas unitarias con MockWebServer)
├── docs/ (Documentación técnica MAIN-009, MAIN-010, REL-001)
├── README.md (Documento principal)
└── build.gradle.kts (Raíz del proyecto)
```

---

 Limitaciones y Pendientes Reales

- **Rotación de Credenciales:** Tras la entrega final se deberán rotar las claves de API locales en Google Cloud y actualizar contraseñas del keystore release.
- **Sincronización Bidireccional:** Room y Xano se mantienen deliberadamente separados en esta entrega; no existe sincronización en segundo plano de inscritos entre teléfonos.
