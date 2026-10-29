# MAIN-008 — Panel administrativo local

## Identidad, roles y migración

La rama de trabajo es `feature/MAIN-008-panel-admin`; Git estaba limpio antes de
modificar. La biblioteca Room sigue en **2.8.4**. La base cambia de **5 a 6** con
`MIGRATION_5_6`: añade `users.role TEXT NOT NULL DEFAULT 'USER'` y concede `ADMIN`
únicamente a la fila con ID `demo-001` y origen `isDemo=1`. Se conservan los IDs,
credenciales y datos anteriores, sin migración destructiva. Se mantienen los JSON
de esquema 1–5 y se añade el 6 generado por Room/KSP.

Solo hay una cuenta demo empaquetada actualmente: Alex, ID `demo-001`, cuyo correo
inicial es `demo@mygos.cl`. El correo no es una prueba de identidad administrativa.
Los registros normales reciben USER. No se incorpora una función para promover
otras cuentas. La autorización requiere simultáneamente rol ADMIN, ID principal
y origen demo, consultados en Room mediante la sesión persistida.

La importación demo comprueba primero el ID. Si ya existe con origen demo,
conserva nombre, correo, credenciales y rol; no restaura los datos del asset. Un
conflicto de ID con otro origen o de correo al importar una cuenta ausente falla
de forma segura, sin reemplazar ni promover al usuario en conflicto.

## Política de eliminación elegida

- Si un usuario organiza actividades, no se elimina. La interfaz y el servicio
  informan cuántas actividades debe resolver primero.
- El administrador puede eliminar esas actividades individualmente, con
  confirmación. Se reutiliza `ActivityDao.deleteAsOrganizer` de MAIN-005 dentro de
  la transacción administrativa, sin duplicar el SQL de borrado de inscripciones.
- Si no organiza actividades, se eliminan sus inscripciones y su cuenta en una
  única transacción. Un fallo revierte ambas operaciones.
- No se elimina al último administrador. La cuenta principal está protegida en
  interfaz y servicio; actualmente es el único ADMIN admitido por la política.
- Las escrituras de creación e inscripción de la interfaz comprueban dentro de
  una transacción que la cuenta aún exista. Así, una pantalla antigua no puede
  recrear asociaciones después del borrado. Las consultas históricas y sus IDs
  se conservan; no se añaden claves foráneas incompatibles con datos antiguos.

## Pantalla y seguridad de las operaciones

Desde Perfil aparece **Panel de administración** únicamente para ADMIN. La ruta
`administracion` también comprueba el usuario autenticado y rechaza USER y sesiones
ausentes. El servicio `AdminRepository` obtiene el actor desde `SessionStore`;
no acepta un ID de administrador proporcionado por la interfaz. Cada lectura y
mutación comprueba de nuevo la cuenta y el rol persistidos, y las escrituras
revalidan esa autorización dentro de la transacción.

La pantalla mantiene Compose, MVVM y los componentes morado/lavanda existentes.
Tiene pestañas Usuarios y Actividades con estados de carga, vacíos y error.

- Usuarios: ID, nombre, correo y rol; edición con las validaciones existentes,
  normalización y control de correos duplicados mediante índice único.
- Contraseñas: restablecimiento con confirmación, reutilizando `PasswordDeriver`
  (PBKDF2-HMAC-SHA256, 600 000 iteraciones, sal aleatoria de 16 bytes, clave de
  256 bits). Nunca se muestran credenciales originales, hashes ni sales. Los campos
  nuevos no usan `rememberSaveable` y se descartan al cerrar o recrear la pantalla.
- Actividades: ID, título, organizador con ID y nombre disponible, fecha,
  comuna y ocupación/capacidad. Borrado individual con diálogo de confirmación.
- Room Flow actualiza las listas administrativas, Perfil, Explorar y la fuente de
  marcadores del mapa. Las funciones de MAIN-005, Maps, registro y contacto se
  conservan.

## Sesiones y límites reales

`LoginViewModel` observa una proyección pública de la cuenta en Room. Las ediciones
actualizan la sesión visible; una cuenta eliminada desaparece del estado y se
limpia su ID persistido. La restauración también descarta IDs inexistentes. No se
leen hashes o sales para construir la sesión pública.

El archivo de sesión y SQLite no comparten una transacción física. Si un fallo de
almacenamiento deja un ID antiguo en el archivo, ese ID ya no autoriza ninguna
operación y se descarta al restaurar. Borrar un usuario no borra actividades de
otras personas ni reasigna la organización de forma silenciosa. Restablecer una
contraseña conserva las sesiones locales existentes de una cuenta que sigue viva.

Todo es local al dispositivo: no hay servidor, administración remota ni una defensa
frente a manipulación directa de archivos privados con acceso root. Maps conserva
su configuración existente; la autorización y facturación de Google son externas.

## Archivos

Rutas Kotlin de aplicación relativas a `app/src/main/java/com/duoc/plomoredsocial/`:

- `model/AuthModels.kt`, `UserEntity.kt`, `UserDao.kt`, `ActivityDao.kt`,
  `MygosDatabase.kt`.
- `repository/AuthRepository.kt`, `RoomAuthRepository.kt`, `AdminRepository.kt`.
- `viewmodel/LoginViewModel.kt`, `ActivitiesViewModel.kt`, `AdminViewModel.kt`.
- `ui/admin/AdminScreen.kt`.
- `ui/navigation/MygosDestination.kt`, `MygosApp.kt`, `TemporaryScreen.kt`.
- `app/src/main/res/values/strings_navigation.xml`.
- `app/schemas/com.duoc.plomoredsocial.model.MygosDatabase/6.json`.

Pruebas nuevas en `app/src/androidTest/java/com/duoc/plomoredsocial/`:
`AdminRepositoryTest.kt`, `AdminMigrationTest.kt`, `AdminPanelTest.kt` y
`AdminNavigationTest.kt`. Se adaptan `ActivitiesPersistenceTest.kt`,
`ActivityMigrationTest.kt`, `CapacityMigrationTest.kt`, `UserMigrationTest.kt`,
`ActivityManagementUiTest.kt` y `SessionPersistenceTest.kt` para la cadena de
migraciones, las cuentas de fixtures y las comprobaciones de rol persistido.

Las pruebas usan cuentas ficticias únicamente como fixtures en memoria o en bases
temporales. La prueba de navegación de USER crea y retira su propia cuenta de
prueba. No se crean usuarios nuevos como parte de la funcionalidad del panel.

## Verificación

Se comprueban permisos de lectura y escritura, bloqueo de rutas, permisos
adulterados por correo/rol sin identidad válida, persistencia de roles y sesión,
edición y duplicados, contraseña nueva y sal diferente, eliminación con bloqueo
de organizadores, cancelación, rollback y actualización de listas. La migración
5→6 se prueba con datos anteriores y con un ID principal de origen incorrecto.

La compilación se ejecuta mediante MCP en Android Studio. Para Gradle se utiliza
el JDK de Android Studio con `gradle/wrapper/gradle-wrapper.jar`, evitando el fallo
de classpath que ya presentaba `gradlew.bat`, sin modificar ese script.

No se ejecutan `git add`, commit, push ni Pull Request.

### Resultados finales

- Compilación mediante MCP de Android Studio correcta, sin errores.
- Validación final de Gradle: `testDebugUnitTest`, `assembleDebug`,
  `connectedDebugAndroidTest` e instalación `installDebug` correctas.
- **20 pruebas unitarias aprobadas** en la ejecución final.
- Suite completa: **61 instrumentadas aprobadas**, sin fallos ni omisiones.
- Después de incorporar la protección transaccional contra escrituras de cuentas
  eliminadas: **22 casos afectados repetidos y aprobados**, sin fallos ni omisiones.
- Prueba concurrente adicional: borrado simultáneo con creación e inscripción,
  repetido con cuentas distintas, aprobada. Total combinado: **62 casos
  instrumentados distintos aprobados**. No se presenta como una única corrida de 62.
- Resumen combinado sin datos de cuentas ni credenciales:
  `build/main008-final-results.json` (artefacto local excluido de Git).
- Inspecciones del IDE para repositorio, ViewModel y pantalla administrativa:
  sin errores. `git diff --check` se utiliza para revisar los cambios finales.
- La comparación de esquemas 5 y 6 confirma que contactos, actividades e
  inscripciones no cambian su definición y que se conservan todas las columnas de
  usuarios, añadiendo únicamente `role`.
- Maps, manifiesto, dependencias y `PasswordDeriver` no se modificaron.
- Gradle conserva un aviso de funciones obsoletas para Gradle 10; no impide la
  compilación actual.

El panel se dejó abierto en **Usuarios** con la sesión demo ADMIN en el emulador
Pixel 7 (Android 17), después de inspeccionar visualmente ambas pestañas. El estado
actual contiene la cuenta demo y ninguna actividad guardada; no se agregaron
usuarios ni actividades de ejemplo para rellenarlo. Los recorridos con otras
cuentas y actividades se verificaron mediante fixtures aisladas.

Capturas de la revisión visual, excluidas de Git:
`build/main008-admin-users.png` y `build/main008-admin-activities.png`.
