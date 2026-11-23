# Autenticación de usuarios con Xano

## Alcance

La app principal usa Xano como fuente de identidad mediante los formularios
normales **Crear cuenta** e **Iniciar sesión**. `MainActivity` entrega
`XanoAuthRepository` a ambos ViewModels. Room v6 y los repositorios locales
permanecen para datos y pruebas heredados, pero ya no autentican a la app
principal. Los flujos de actividades compartidas, Maps, GPS y notificaciones
no cambian en esta integración.

## Contrato comprobado

La base existente de Retrofit es
`https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/`. Sin enviar credenciales ni
crear cuentas, las rutas respondieron: `GET /auth/me` sin token HTTP 401,
`POST /auth/login` con JSON vacío HTTP 400 y `POST /auth/signup` con JSON vacío
HTTP 400. El usuario informó pruebas válidas en Xano para los tres endpoints:
signup/login entregan `authToken` y `user_id`; me entrega el registro de
`usuarios notariax` sin contraseña. No se modificaron scripts de Xano.

## Flujo y seguridad

- Registro normaliza el correo, valida nombre/correo/contraseña/confirmación y
  bloquea envíos repetidos mientras Xano responde. Envía únicamente `name`,
  `email` y `password`; nunca envía `administrador`.
- Login usa `POST /auth/login`, valida `authToken` y `user_id` y consulta
  `GET /auth/me` con Bearer. El perfil remoto debe tener el mismo ID; la UI
  recibe nombre y correo, no el token.
- Un registro HTTP 2xx seguido de fallo al crear la sesión se muestra como
  **cuenta creada, iniciar sesión**, para evitar un segundo signup accidental.
- El token se cifra con AES-GCM y una clave del Android Keystore antes de
  guardarse en `noBackupFilesDir` con `AtomicFile`. Al abrir se consulta
  `/auth/me`; un token rechazado se elimina. Cerrar sesión borra el token antes
  de mostrar la UI sin usuario. La contraseña no se persiste.
- La respuesta de Xano no se copia a la tabla Room `users`. El panel de
  administración local no se concede por un campo que venga del cliente.
- Se retiraron los campos y botones de auth del panel diagnóstico de
  Explorar. Uno de esos campos guardaba la contraseña con `rememberSaveable`.

## Verificaciones de esta sesión

- `:app:assembleDebug :app:testDebugUnitTest --no-daemon`: BUILD SUCCESSFUL;
  42 pruebas unitarias, 0 fallos. Las pruebas de `XanoAuthRepository` usan
  MockWebServer; no crean usuarios reales.
- `:app:compileDebugAndroidTestKotlin --no-daemon`: BUILD SUCCESSFUL. Las
  pruebas instrumentadas adaptadas usan repositorios falsos para auth. No se
  ejecutaron en dispositivo porque el Pixel 7 tenía un release firmado;
  la firma debug es distinta.
- `:app:assembleRelease --no-daemon`: BUILD SUCCESSFUL; `apksigner verify`
  confirmó una firma v2 válida. APK local:
  `app/build/outputs/apk/release/app-release.apk` (SHA-256
  `057DAFE73A5EB7D0C737EF448DF743FB62499E0103F3B219F405BFFBA2CEE880`).
  `adb install -r` lo actualizó inicialmente en Pixel 7 sin desinstalar.
- En el APK firmado se observaron **Inicio**, **Explorar** con una actividad
  obtenida de Xano, **Perfil** invitado y los formularios normales **Iniciar
  sesión** y **Crear cuenta**. No se enviaron credenciales.
- Durante la revisión del emulador, Android retiró el paquete por medio de
  Package Installer (`ACTION_PACKAGE_FULLY_REMOVED` en logcat). No hubo un
  registro de crash de Mygos que explique la salida. Se reinstaló el mismo
  APK y las pantallas anteriores volvieron a abrir; la instalación posterior
  no restaura los datos que Android hubiera eliminado. La causa de la acción
  de desinstalación queda sin determinar.

## Límites y siguientes pasos

- Falta observar un registro y login válidos desde los formularios normales
  contra Xano, con una cuenta de prueba autorizada. No se guarda una
  contraseña de prueba en el repositorio ni se crea una cuenta remota desde
  una prueba automática.
- Falta ejecutar las pruebas instrumentadas en un AVD independiente con la
  firma debug; el Pixel 7 tiene el release firmado.
- No existe endpoint de restablecimiento de contraseña entre los tres
  endpoints confirmados. La recuperación implementada es del **perfil de
  usuario** al restaurar el token con `/auth/me`.
- Las cuentas locales anteriores no se migran ni se eliminan automáticamente.
  Sus datos Room se conservan, pero no sirven para iniciar sesión en Xano.
