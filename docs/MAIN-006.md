# MAIN-006 — Registro funcional de cuentas locales

## Flujo y arquitectura
Registro → RegisterViewModel → AuthRepository → RoomAuthRepository → UserDao → Room.
Login y restauración de sesión usan la misma instancia de RoomAuthRepository.
AuthUser sigue siendo la proyección pública de perfil; no incluye credenciales.
UserEntity es únicamente el registro persistido con el material de verificación.
Crear una cuenta no inicia sesión: la confirmación ofrece Login con el correo completado.
La pantalla valida nombre (1–80 tras recortar), correo válido de hasta 254 caracteres,
contraseña de 12–128 caracteres y confirmación idéntica. Se permiten frases y Unicode.
Las contraseñas no se recortan ni se normalizan. Se descartan al salir/recrear la vista
y después del registro exitoso; no se escriben en SavedStateHandle ni Bundle.
Se muestra carga, duplicado, error de almacenamiento y éxito real tras insertar.
Se bloquean pulsaciones repetidas y la salida por Atrás durante el registro.

## Unicidad y usuarios anteriores
normalizedEmail = email.trim().lowercase(Locale.ROOT), con índice único en Room.
El chequeo previo mejora el mensaje, y el índice resuelve también la carrera entre registros.
Nunca se usa REPLACE para una cuenta.
users.json permanece sin cambios y TemporaryAuthRepository se conserva.
Antes de registrar, autenticar o restaurar por ID, se importan idempotentemente los
usuarios demo desde assets mediante una transacción. Conservan ID, perfil, hash y sal.
Así demo-001 mantiene su sesión, actividades e inscripciones y su correo queda reservado.
Las cuentas nuevas usan IDs local-UUID y se identifican separadamente de las demo.
Perfil solo muestra el aviso de demostración para las cuentas marcadas como tales.

## Contraseñas
Cuentas nuevas: PBKDF2-HMAC-SHA256, 600.000 iteraciones, sal aleatoria de SecureRandom
de 16 bytes por cuenta y hash de 256 bits. Se almacenan algoritmo, parámetros y versión.
La derivación corre en Dispatchers.Default y fuera de la transacción de escritura.
Se usa la API ligera de Bouncy Castle 1.86 para conservar API 24/25 sin implementar PBKDF2
manualmente ni modificar los proveedores globales de Android.
La verificación compara bytes con MessageDigest.isEqual y rechaza parámetros no admitidos.
El caso antiguo PBKDF2-SHA1 de 120.000 iteraciones se restringe a las cuentas demo.
No hay contraseñas en texto plano en Room ni logs. Las copias temporales de chars/bytes
del derivador se limpian; los String de entrada tienen las limitaciones normales de la JVM.
La contraseña no cifra la base ni sustituye al aislamiento del sistema operativo.

Referencias:
- https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2
- https://www.bouncycastle.org/resources/new-release-bouncy-castle-java-1-86/
- https://developer.android.com/reference/javax/crypto/SecretKeyFactory

## Migración autorizada 3 → 4
Solo crea users y el índice único index_users_normalizedEmail.
Se conservan las migraciones 1 → 2 y 2 → 3; se registra toda la cadena hasta 4.
No modifica ni borra contacts, activities o activity_members.
No añade claves foráneas que invaliden IDs históricos, ni migra usuarios hacia IDs nuevos.
El esquema exportado está en app/schemas/com.duoc.plomoredsocial.model.MygosDatabase/4.json.
La importación demo ocurre en el repositorio después de la migración, también en instalaciones nuevas.
No se usa fallbackToDestructiveMigration.

## Sesión y asociaciones
LocalSessionStore no cambia: guarda solo el ID en noBackupFilesDir usando AtomicFile.
LoginViewModel restaura el usuario desde Room, conserva la limpieza de contraseñas y
persiste el cierre antes de retirar la sesión de la interfaz.
La creación y las inscripciones siguen usando AuthUser.id; no se modifica su lógica.
Los contactos siguen admitiendo invitados y no se atribuyen retroactivamente a una cuenta.

## Copias y límites
mygos.db, su WAL y SHM se excluyen de cloud backup y de device-transfer.
Como es una base compartida, esta exclusión también alcanza actividades, contactos e
inscripciones: se preservan en el dispositivo pero no se transfieren automáticamente.
La sesión continúa excluida de copias por estar en noBackupFilesDir.
Son cuentas locales: sin servidor, sincronización, verificación de correo ni recuperación
remota. Borrar datos o desinstalar elimina las cuentas locales.
Google Maps conserva su configuración y sus limitaciones existentes; no se validó API real.
El proyecto mantiene minSdk 24. El emulador disponible para las pruebas es API 37;
la compatibilidad de ejecución con API 24/25 necesita una prueba en esos dispositivos.

## Pruebas
Migración desde 1/2/3 conservando datos; compatibilidad demo y sesión anterior.
Registro y Login tras reapertura; correos con mayúsculas/espacios y duplicados concurrentes.
Hash contrastado con JCA independiente, sales distintas, Unicode y rechazo de formato inseguro.
Errores de almacenamiento, doble envío, límites, confirmación y limpieza de secretos.
Dos usuarios con organizador e inscripciones independientes.
Regresión de navegación, perfil, contacto, búsqueda, filtros, scroll, creación, fecha/hora y Maps sin clave.

## Archivos de implementación
- Modelo/Room: AuthModels.kt, UserEntity.kt, UserDao.kt, RegisterValidator.kt, MygosDatabase.kt y esquema 4.json.
- Autenticación: AuthRepository.kt, RoomAuthRepository.kt, PasswordDeriver.kt; TemporaryAuthRepository.kt se conserva y marca las cuentas demo.
- Interfaz y estado: RegisterScreen.kt, RegisterViewModel.kt, MygosComponents.kt, MainActivity.kt, MygosApp.kt, TemporaryScreen.kt, HomeScreen.kt y texto de acceso en ExploreScreen.kt.
- Configuración: app/build.gradle.kts, backup_rules.xml y data_extraction_rules.xml.
- Pruebas nuevas: RegisterValidatorTest, PasswordDeriverTest, LocalAuthRepositoryTest, UserMigrationTest y RegisterViewModelTest.
- Pruebas adaptadas: ProfileRegisterTest, SessionPersistenceTest, ActivitiesPersistenceTest y ActivityMigrationTest.
LoginScreen, LoginViewModel, LocalSessionStore, users.json y el módulo de actividades conservan su mecanismo existente.

## Resultado verificado
- Compilación mediante MCP build_project: correcta.
- testDebugUnitTest: 20 pruebas, 0 fallos.
- Suite instrumentada completa: 35 pruebas, 0 fallos y 0 errores.
- Se añadió y ejecutó después un caso de sesión local en ViewModels nuevos: 1 prueba aprobada.
  Total: 36 casos instrumentados distintos verificados en dos ejecuciones finales.
- Dispositivo: Pixel 7 / API 37. No hay otro AVD instalado; API 24/25 queda pendiente de ejecución real.
- Los tests incluyen registro/UI, confirmación incorrecta, duplicado demo y concurrente, fallo de
  almacenamiento, doble envío, contraseña descartada al recrear, Login posterior, restauración,
  cierre persistente, asociaciones de dos usuarios, contactos y migraciones 1/2/3 → 4.
- Las contraseñas se contrastaron con JCA independiente y los formatos inseguros se rechazan.
- La nueva dependencia puede requerir Sync Gradle para actualizar el índice del editor de Android Studio;
  la compilación Gradle y las pruebas ejecutadas sí resolvieron y usaron Bouncy Castle 1.86.
- Google Maps con clave real sigue pendiente; se conservó el manejo sin configuración.
- Se revisó visualmente el formulario de registro y se dejó abierto en el emulador, sin crear cuentas manuales de prueba.
- El ejecutor de configuración app de MCP devolvió Cannot find runner for app; se instaló el APK compilado con adb install -r y se inició MainActivity por ADB desde el terminal MCP.
