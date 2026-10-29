# MAIN-005 — Gestión de reuniones y actividades

## Persistencia y relaciones

- Biblioteca Room: **2.8.4**, sin cambio de dependencia.
- Versión de base de datos: **4 → 5** mediante `MIGRATION_4_5`.
- La migración añade `capacity INTEGER NOT NULL DEFAULT 10` a `activities`.
- Se conservan los JSON 1–4 y se añade el esquema 5 generado por Room/KSP.
- No se recrean tablas, se borran datos ni se usa migración destructiva.
- El esquema anterior relaciona organizadores e inscritos mediante IDs de texto, sin
  claves foráneas. No se añaden restricciones que puedan invalidar IDs históricos.
- El organizador ocupa un cupo: `1 + inscritos distintos del organizador`.
  Los registros históricos del organizador se conservan, pero no duplican el conteo.
- Si una actividad antigua ya supera diez participantes, se conserva íntegra y se
  bloquean nuevas inscripciones hasta que haya cupos. No se expulsan participantes.

## Comportamiento

- Crear actividad incluye un selector discreto de 1 a 10, inicialmente 10.
- Formulario, ViewModel y DAO rechazan capacidad inválida al guardar.
- Inscribirse revalida existencia, duplicados y cupo dentro de `@Transaction`.
- Retirarse elimina la inscripción y libera el cupo; no elimina al organizador.
- El detalle muestra participantes/capacidad y estado completo.
- El botón de eliminar solo aparece para el organizador de la sesión actual.
  Un AlertDialog exige confirmación; cancelar conserva todos los datos.
- El DAO vuelve a comprobar el ID del organizador y elimina inscripciones y actividad
  en una transacción. Si falla el borrado, se revierte todo.
- Perfil consulta Room por el ID real de sesión: creadas y participaciones vigentes.
  La segunda consulta excluye las actividades propias para no duplicarlas.
- Las consultas Flow actualizan Perfil, Explorar y la lista utilizada por el mapa.
  Cambiar de cuenta descarta listas y diálogos de la cuenta anterior.
- Sin sesión se conservan Login, Registro y Contáctanos sin las listas privadas.
- Se mantienen búsqueda, filtros, navegación, registro, descripción, coordenadas
  opcionales, selección en Maps e indicaciones externas. No se modifica autenticación.

## Archivos de aplicación y esquema

- `model/ActivityEntity.kt`: capacidad y conteo único del organizador.
- `model/ActivityDao.kt`: consultas de Perfil, cupos y eliminación transaccional.
- `model/ActivityFormValidator.kt`: validación de capacidad.
- `model/MygosDatabase.kt`: versión 5 y migración explícita.
- `viewmodel/ActivitiesViewModel.kt`: operaciones, mensajes y consultas de Perfil.
- `viewmodel/HomeViewModel.kt`: contador compartido con Explorar.
- `ui/activities/ActivitiesScreen.kt`: selector, contador y confirmación de borrado.
- `ui/activities/ProfileActivities.kt`: secciones privadas y acceso al detalle/mapa.
- `ui/navigation/ExploreScreen.kt`: participantes/capacidad.
- `ui/navigation/TemporaryScreen.kt`: integración con Perfil y aislamiento por cuenta.
- `app/schemas/com.duoc.plomoredsocial.model.MygosDatabase/5.json`.

Las rutas Kotlin anteriores son relativas a
`app/src/main/java/com/duoc/plomoredsocial/`.

## Pruebas

Se añaden `ActivityManagementTest`, `ActivityManagementUiTest` y
`CapacityMigrationTest`. Usan bases en memoria o archivos temporales propios;
los datos ficticios se limitan a las pruebas.

Se conservan y adaptan las pruebas existentes `ActivitiesPersistenceTest`,
`ActivityMigrationTest`, `UserMigrationTest`, `HomeRoomIntegrationTest` y
`HomeActivityAccessTest` para la nueva migración y el contador con organizador.
`ActivitiesAccessTest` espera la inicialización asíncrona de la vista del mapa.
`ActivityCreationTest` comprueba creación manual también con Maps configurado.

Cobertura nueva: capacidades 1/5/10 y fuera de rango, duplicado histórico del
organizador, treinta inscripciones concurrentes para cinco cupos, retiro,
aislamiento de cuentas, listas reactivas, eliminación autorizada, rechazo de
terceros, cancelación, limpieza de inscritos y rollback ante fallo. La migración
4→5 conserva contactos, usuarios, credenciales almacenadas, actividades con y
sin coordenadas, descripciones e inscripciones, incluso al reabrir la base.

### Resultados verificados

- Rama inicial `feature/MAIN-005-gestion-reuniones`; Git limpio antes de editar.
- Compilación MCP de Android Studio correcta.
- Validación final de Gradle: `testDebugUnitTest`, `assembleDebug`,
  `compileDebugAndroidTestKotlin` e `installDebug` correctas.
- **20 pruebas unitarias aprobadas** en la validación final.
- **46 pruebas instrumentadas distintas aprobadas**, combinando la suite completa
  con las repeticiones de las pruebas afectadas; ningún caso pendiente ni omitido
  en el resultado combinado. No se presenta como una única ejecución completa limpia.
- La primera suite de 45 tuvo 43 aprobadas, una omitida y un fallo de búsqueda del
  nodo del mapa. Se corrigió la espera del SDK y se precisó el botón Cancelar del
  selector. Después se verificaron 18 pruebas afectadas y se repitió el recorrido
  de Maps configurado hasta completarlo correctamente. La prueba nueva de rollback
  lleva el total de casos distintos a 46.
- Las seis pruebas de acceso y creación se ejecutaron también con
  `MAPS_CONFIGURED=false`: todas aprobadas, sin omisiones. Se usó una variable de
  entorno temporal solo para el proceso de pruebas; `local.properties` no se editó.
- Se restauró la compilación normal con `MAPS_CONFIGURED=true` y se instaló en el
  emulador Pixel 7, Android 17. La presencia del mapa en la interfaz no certifica
  facturación, restricciones ni disponibilidad de todas las teselas de Google.
- La comparación de JSON confirma que `contacts`, `users` y `activity_members`
  conservan exactamente su definición; en `activities` se añade la capacidad.
- Gradle avisa de funciones obsoletas para Gradle 10; no impiden esta compilación.
- Sin `git add`, commit, push ni Pull Request.

## Limitaciones

La autorización es local: la capa de datos comprueba que el ID recibido coincide
con el organizador; la interfaz obtiene ese ID de la sesión autenticada existente.
No hay servidor ni sincronización entre dispositivos. Los IDs históricos sin
cuenta asociada no se reasignan. No se crean claves ni se modifican restricciones
de Google Cloud. Que Maps esté configurado no prueba que Google autorice la clave.

El script Windows `gradlew.bat` presenta un error de classpath en este entorno.
La validación usa mediante el terminal MCP el JDK de Android Studio y el mismo
`gradle/wrapper/gradle-wrapper.jar`, sin modificar el wrapper.
