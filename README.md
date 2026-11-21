# notaria_dap
Trabajo Desarrollo app movil.

## Actividades y Google Maps

En **Actividades** se puede filtrar por comuna, crear un panorama con nombre,
fecha/hora y referencia, elegir el punto exacto tocando el mapa, inscribirse o
salir y abrir las indicaciones de Google Maps. Para crear o inscribirse hay que
iniciar sesión. El mapa comienza en Santiago y permite desplazarse a otro sector.

Las actividades e inscripciones se guardan en Room en este dispositivo; todavía
no hay un servidor que las publique o sincronice entre teléfonos. La comuna se
ingresa manualmente y el filtro busca ese texto; no calcula cercanía por GPS.
El esquema Room actual es la versión 6.

Para habilitar el mapa integrado:

1. Crear un proyecto de Google Cloud con facturación habilitada y activar
   **Maps SDK for Android**.
2. Crear una clave restringida a aplicaciones Android, al paquete
   `com.duoc.plomoredsocial` y al SHA-1 del certificado de firma (se obtiene con
   `.\gradlew.bat :app:signingReport`). Restringir también la API al Maps SDK for Android.
3. Agregar `MAPS_API_KEY=tu_clave` a `local.properties` (excluido de Git), o definir
   la variable de entorno `MAPS_API_KEY`, que tiene prioridad. Volver a compilar.

Sin clave, la pantalla muestra un aviso y deshabilita la elección del punto;
no intenta iniciar un mapa sin configurar. La dirección textual sigue disponible.
La clave de una app Android se incluye en el APK: las restricciones de Google
Cloud son necesarias aunque no esté en el repositorio.

Referencias: [configurar Maps SDK](https://developers.google.com/maps/documentation/android-sdk/get-api-key)
y [Maps Compose](https://github.com/googlemaps/android-maps-compose).

## GPS y notificaciones Android (MAIN-010)

En **Explorar → Crear actividad → Elegir punto en el mapa**, el botón **Usar mi
ubicación** consulta la posición actual del dispositivo. Android solicita
`ACCESS_COARSE_LOCATION` y `ACCESS_FINE_LOCATION` solo al tocarlo. Con permiso,
el mapa se centra en la posición y muestra un marcador; **Usar este punto**
confirma las coordenadas. Si se deniega el permiso, la ubicación está desactivada
o no llega una posición, aparece un aviso y se puede reintentar, tocar el mapa
o continuar solo con la dirección textual. El GPS no filtra actividades cercanas
ni sincroniza ubicaciones entre teléfonos.

Al guardar correctamente una actividad local en Room, Mygos envía una
notificación mediante `NotificationManager` y el canal **Actividades creadas**.
En Android 13 o superior solicita `POST_NOTIFICATIONS` después del guardado.
Si se deniega, la actividad queda creada igualmente. Cada actividad usa su ID
para reemplazar un eventual aviso repetido; no hay servicio en segundo plano.

Para probar ambos recursos en Pixel_7:

1. Iniciar el emulador y fijar una ubicación ficticia desde **Extended Controls
   → Location**, o con `adb emu geo fix -70.6693 -33.4489` (longitud primero).
2. Iniciar sesión con una cuenta de demostración, abrir **Explorar → Crear
   actividad → Elegir punto en el mapa** y pulsar **Usar mi ubicación**. Probar
   primero **No permitir** y luego conceder **Ubicación precisa**; comprobar el
   aviso en el primer caso y el marcador centrado en el segundo.
3. Confirmar el punto, completar el formulario con datos ficticios futuros y
   guardar. Comprobar la actividad en el catálogo local y el aviso en la
   bandeja de notificaciones. Repetir con `POST_NOTIFICATIONS` denegado: el
   guardado debe continuar sin aviso.

El estado verificado, los comandos de prueba y las limitaciones están en
`docs/MAIN-010.md`.

## Login temporal

Desde **Inicio → Iniciar sesión**, usar:

- Correo: `demo@mygos.cl`
- Contraseña: `Mygos2026!`

Las cuentas ficticias están en `app/src/main/assets/temporary_auth/users.json`.
El archivo contiene identificador, nombre, correo, salt, iteraciones y hash PBKDF2;
no contiene la contraseña en texto plano. Es una fuente local de demostración,
no autenticación de producción. No agregar credenciales reales al repositorio.

`TemporaryAuthRepository` implementa `AuthRepository`. Para migrar, crear otra
implementación del contrato y cambiar la dependencia en `MainActivity`.
El formulario y el ViewModel no necesitan conocer la fuente de datos.

Al entrar se vuelve a Inicio y se muestra el nombre. En Perfil se muestran los
datos de la cuenta y **Cerrar sesión**. La sesión se conserva al rotar, navegar,
cerrar la app o reiniciar el dispositivo. Se guarda solo el ID del usuario en un
archivo privado dentro de `noBackupFilesDir`, mediante `LocalSessionStore`.
Al abrir se consulta nuevamente la cuenta; si ya no existe, se descarta la sesión.
No se guarda la contraseña. Cerrar sesión borra el acceso persistido antes de
mostrar la interfaz sin usuario. Desinstalar la app o borrar sus datos también
elimina la sesión; el archivo está excluido de las copias de seguridad.
La pantalla **Crear cuenta** está disponible desde Login.

## Verificación

Requiere el SDK Android 37 y el JDK 25 configurados por el proyecto.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:connectedDebugAndroidTest
```

Para MAIN-010, `assembleDebug` terminó correctamente: 29 pruebas unitarias y
67 pruebas instrumentadas en Pixel_7 aprobaron, sin fallos. Estas últimas
incluyen Room, creación, navegación y una notificación tras el guardado.

Las pruebas de login cubren validación, lectura de la cuenta temporal,
credenciales incorrectas, inicio, recreación de la actividad, restauración desde
disco y cierre de sesión persistente.

## Actividades en línea (Xano)

Explorar incluye una sección separada para consultar actividades de Xano. La URL
base es `https://x8ki-letl-twmt.n7.xano.io/api:6kDB2GLk/` y también está
definida en `XanoApiClient`; la pantalla usa Retrofit para `GET
/actividades` y permite consultar `GET /actividades/{actividades_id}` por el ID
remoto. Los objetos JSON se conservan sin mapear campos no confirmados y se
ocultan claves con nombres comunes de credenciales o contacto al mostrarlos.

Room sigue siendo la fuente independiente de usuarios, actividades, inscripciones
y administración local. Las respuestas de Xano no se insertan ni sincronizan a
Room; sus IDs tampoco se mezclan con los IDs locales. En esta etapa no se
sincronizan cupos ni inscritos entre dispositivos.

El contrato OpenAPI compartido no estuvo accesible (HTTP 404) y una consulta
real de `GET /actividades` respondió HTTP 200 con `[]`. Por ello aún no se conoce
el esquema de campos de las actividades ni se implementan escrituras. Según lo
informado, Xano reporta 22 endpoints en el grupo y las escrituras aparecen como
públicas; no se pudo revisar su Swagger para confirmar esos ajustes. Solo se
confirmaron los cinco métodos/rutas de actividades indicados para el trabajo.
Las operaciones `POST`, `PATCH` y `DELETE` no se habilitan desde la app mientras
no se confirmen autorización y validaciones server-side. No se incluye token ni
clave. Esta integración tampoco sustituye un requisito separado de Spring Boot.

Las pruebas de `XanoActivityRepository` usan MockWebServer y datos ficticios para
validar GET, respuesta vacía, objeto JSON sin esquema asumido, errores HTTP,
forma JSON inválida y timeout. No ejecutan POST contra Xano. Para habilitar más
rutas, exportar el OpenAPI del grupo sin claves y verificar primero su política
de autenticación; no compartir una Developer API Key.

Para revisar la integración, abre **Explorar**, desplázate a **Actividades en
línea** y usa **Actualizar actividades en línea**. La consulta de detalle acepta
un ID remoto escrito manualmente; no utiliza los UUID de Room. La pantalla
muestra estados de carga, vacío, error y reintento. El JSON se presenta sin
suponer nombres de campos, porque el servidor respondió con una lista vacía.

Compila y ejecuta las pruebas de lectura sin generar un APK release:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --rerun-tasks --no-daemon
```

El estado verificable y el texto para Trello están en `docs/MAIN-009.md`.
