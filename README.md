# notaria_dap
Trabajo Desarrollo app movil.

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
datos de la cuenta y **Cerrar sesión**. La sesión se conserva al rotar o navegar,
pero termina cuando muere el proceso; no se guarda la contraseña en preferencias.
Crear cuenta continúa siendo una pantalla pendiente.

## Verificación

Requiere el SDK Android 37 y el JDK 25 configurados por el proyecto.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas de login cubren validación, lectura de la cuenta temporal,
credenciales incorrectas, inicio, recreación de la actividad y cierre de sesión.
