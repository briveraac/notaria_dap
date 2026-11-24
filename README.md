# Mygos — Aplicación Móvil Android (Kotlin + Jetpack Compose)

Aplicación móvil Android nativa desarrollada en Kotlin para la exploración, creación y gestión comunitaria de actividades y eventos locales. Implementa una arquitectura reactiva MVVM, almacenamiento local en Room, integración de servicios web REST con Xano y Retrofit, mapas interactivos mediante Google Maps SDK y funcionalidades nativas de Android (GPS y Notificaciones).

---

--1. Funcionalidades Principales

- **Autenticación y Sesión:** Autenticación remota en Xano con almacenamiento seguro del token cifrado mediante AES-GCM en el KeyStore de Android.
- **Catálogo Unificado (Xano como Fuente Central):** Las actividades comunitarias compartidas tienen a Xano como fuente principal de verdad (`GET /actividades`, `POST /actividades`, `PATCH /actividades`, `DELETE /actividades`).
- **Creación y Gestión:** Publicación de panoramas desde el formulario principal de la app directamente en Xano, con validación estricta de cupos en el rango 1–10.
- **Servicios Nativos:** GPS mediante los proveedores de ubicación Android y Notificaciones del sistema mediante `NotificationManager`.

---

--2. Arquitectura y Tecnologías

- **Lenguaje:** Kotlin / Java JDK (JBR Android Studio)
- **UI Framework:** Jetpack Compose + Material 3 (Typography Plus Jakarta Sans)
- **Arquitectura:** MVVM (Model-View-ViewModel) con Unidirectional Data Flow (UDF)
- **Base de Datos Local:** Room Database versión 6 (para sesiones y caché local)
- **Red REST:** Retrofit + OkHttp + Gson Converter (Xano API)
- **Google Maps SDK:** Maps Compose

---

--3. Requisitos de la Entrega y Estado Real

| Requisito | Estado Real | Evidencia y Verificación |
|---|---|---|
| **1. Mínimo 7 Endpoints REST** | ✅ Cumplido | 8 endpoints REST implementados y probados con Retrofit. |
| **2. Pruebas Unitarias** | ✅ Cumplido | Pruebas unitarias ejecutadas y aprobadas. |
| **3. APK Release Firmado** | ✅ Cumplido | APK generado (`app-release.apk`) y verificado con `apksigner` (Esquema v2). |
| **4. Repositorio Git Actualizado** | ✅ Cumplido | Cambios consolidados en la rama `main` y sincronizados en `origin/main`. |
| **5. Documentación en README** | ✅ Cumplido | README técnico detallado. |
| **6. Ícono Personalizado** | ✅ Cumplido | Ícono adaptativo configurado. |
| **7. Dos Recursos Nativos** | ✅ Cumplido | GPS real y Notificaciones del sistema. |

---

--4. Endpoints REST de Xano

1. `GET /actividades`
2. `GET /actividades/{actividades_id}`
3. `POST /actividades`
4. `PATCH /actividades/{actividades_id}`
5. `DELETE /actividades/{actividades_id}`
6. `POST /auth/signup`
7. `POST /auth/login`
8. `GET /auth/me`

---

--5. Compilación y Firma

```powershell
# Generar APK release firmado
.\gradlew.bat :app:assembleRelease --no-daemon

# Verificar firma con apksigner
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

---

--6. Limitaciones y Pendientes

- **Autorización Server-Side:** Las operaciones `PATCH` y `DELETE` se mantienen deshabilitadas en la interfaz pública hasta que el backend en Xano valide la propiedad y los permisos del propietario por token.
