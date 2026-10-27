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
La actualización de la base de datos de versión 1 a 2 conserva los contactos.

Para habilitar el mapa integrado:

1. Crear un proyecto de Google Cloud con facturación habilitada y activar
   **Maps SDK for Android**.
2. Crear una clave restringida a aplicaciones Android, al paquete
   `com.duoc.plomoredsocial` y al SHA-1 del certificado de firma (se obtiene con
   `.\gradlew.bat :app:signingReport`). Restringir también la API al Maps SDK for Android.
3. Agregar `MAPS_API_KEY=tu_clave` a `local.properties` (excluido de Git), o definir
   la variable de entorno `MAPS_API_KEY`, que tiene prioridad. Volver a compilar.

Sin clave, la pantalla muestra un aviso y deshabilita la elección del punto;
no intenta iniciar un mapa sin configurar. No se solicitan permisos de ubicación.
La clave de una app Android se incluye en el APK: las restricciones de Google
Cloud son necesarias aunque no esté en el repositorio.

Referencias: [configurar Maps SDK](https://developers.google.com/maps/documentation/android-sdk/get-api-key)
y [Maps Compose](https://github.com/googlemaps/android-maps-compose).

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
Crear cuenta continúa siendo una pantalla pendiente.

## Verificación

Requiere el SDK Android 37 y el JDK 25 configurados por el proyecto.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas de login cubren validación, lectura de la cuenta temporal,
credenciales incorrectas, inicio, recreación de la actividad, restauración desde
disco y cierre de sesión persistente.
