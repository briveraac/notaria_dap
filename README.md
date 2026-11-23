# Mygos — Aplicación Móvil Android (Kotlin + Jetpack Compose)

Mygos es una aplicación móvil nativa desarrollada en Kotlin con Jetpack Compose y arquitectura MVVM. Permite explorar, crear y participar en actividades comunitarias, integrando persistencia local con Room, servicios web REST con Xano y Retrofit, mapas interactivos con Google Maps SDK, y funciones nativas como GPS y notificaciones.

---

## 🚀 Requisitos Cumplidos (Entrega 3)

1. **Mínimo 7 Endpoints REST distintos:** Implementados y consumidos mediante Retrofit en Xano (`GET /actividades`, `GET /actividades/{id}`, `GET /actividades?comuna`, `POST /actividades`, `PATCH /actividades/{id}`, `DELETE /actividades/{id}`, `GET /auth/me`).
2. **Pruebas Unitarias Robustas:** 34 pruebas unitarias ejecutadas y aprobadas (cobertura con `MockWebServer`, clasificadores, validadores y ViewModel).
3. **APK Release Firmado (REL-001):** Generado con keystore legítimo (RSA 3072 bits, PKCS12) y verificado con `apksigner` (Esquema de firma v2).
4. **Git Actualizado:** Trabajo en la rama `main` sin conflictos y actualizado en origin/main.
5. **README Técnico:** Documentación completa de arquitectura, endpoints, persistencia y procedimientos de compilación/verificación.
6. **Ícono Personalizado:** Configurado mediante Launcher Adaptativo (`ic_launcher`, vector background/foreground).
7. **Dos Recursos Nativos:**
   - **GPS Real:** Obtención de coordenadas mediante `FusedLocationProviderClient` y permisos en tiempo de ejecución (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).
   - **Notificaciones del Sistema:** Publicación de avisos mediante `NotificationManager` y canal dedicado al guardar actividades (`POST_NOTIFICATIONS`).

---

## 🛠️ Arquitectura y Tecnologías

- **Lenguaje:** Kotlin (JDK 25 / JBR Android Studio)
- **UI Toolkit:** Jetpack Compose + Material 3 (Plus Jakarta Sans Font)
- **Arquitectura:** MVVM (Model-View-ViewModel) + Clean Architecture por capas
- **Persistencia Local:** Room Database (Esquema Versión 6) para Usuarios, Actividades, Inscripciones y Administración.
- **Red & API REST:** Retrofit 3.0.0, OkHttp 5.5.1, Gson Converter, MockWebServer.
- **Integración de Mapas:** Google Maps SDK for Android (`com.google.android.gms:play-services-maps`).
- **Servicios Nativos:** Google Play Services Location & Android Notification Manager.

---

## 📡 API REST External (Xano) — MAIN-009

La app se conecta al grupo de API de Xano (`https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/`). La integración es independiente de la base de datos local Room.

### Endpoints Consumidos (7):
1. `GET /actividades`: Consulta la lista completa de actividades remotas.
2. `GET /actividades/{actividades_id}`: Obtiene el detalle de un registro por su ID remoto.
3. `GET /actividades?comuna={comuna}`: Filtra/busca actividades remotas por comuna mediante parámetro de consulta.
4. `POST /actividades`: Crea una nueva actividad remota en el servidor Xano.
5. `PATCH /actividades/{actividades_id}`: Actualiza atributos (ej. cupos) de una actividad en Xano.
6. `DELETE /actividades/{actividades_id}`: Elimina una actividad remota por ID en Xano.
7. `GET /auth/me`: Consulta y verifica el estado del endpoint remoto de autenticación.

---

## 🔑 Firma Release y Google Maps — REL-001

Para compilar el APK release firmado:
1. Definir credenciales en `local.properties` (excluido de Git):
   ```properties
   RELEASE_STORE_FILE=
   RELEASE_KEY_ALIAS=mygos
   RELEASE_STORE_PASSWORD=<keystore>
   RELEASE_KEY_PASSWORD=<clave>
   MAPS_API_KEY=<clave_google_maps>
   ```
2. Compilar APK release:
   ```powershell
   .\gradlew.bat :app:assembleRelease --no-daemon
   ```
3. Verificar firma del APK:
   ```powershell
   & 'C:\Users\Juseppe\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat' verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
   ```

---

## 🧪 Verificación y Pruebas

Para ejecutar la suite de pruebas unitarias y compilación:

```powershell
# Pruebas unitarias (34/34 aprobadas)
.\gradlew.bat :app:testDebugUnitTest

# Compilaciones Debug y Pruebas Instrumentadas
.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

Documentación detallada disponible en la carpeta `docs/` (`docs/MAIN-009.md`, `docs/REL-001.md`, `docs/MAIN-010.md`).
