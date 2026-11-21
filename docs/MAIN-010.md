# MAIN-010 — GPS y notificaciones nativas

## Alcance y decisiones

Se trabaja en `main` y se conserva Room v6. `LocationPicker` consulta la
ubicación actual con `LocationManagerCompat` y los proveedores Android
disponibles; no usa una coordenada fija de Maps como si fuera GPS. El permiso
se solicita al pulsar **Usar mi ubicación**. El usuario puede escoger un punto
manualmente o continuar con dirección textual. No se añaden servicios de fondo.

El aviso de actividad creada se dispara desde el callback de éxito de
`ActivitiesViewModel.create`, después de `ActivityDao.createForAccount`. En
Android 13+ se pide `POST_NOTIFICATIONS` entonces; un rechazo no revierte el
guardado. `NotificationManager` crea el canal una vez y usa el ID de la
actividad para reemplazar un posible aviso repetido.

## Archivos afectados

- `app/src/main/AndroidManifest.xml`: permisos de ubicación y notificaciones;
  GPS opcional para la instalación.
- `app/src/main/java/com/duoc/plomoredsocial/ui/activities/ActivitiesScreen.kt`:
  botón GPS, estados y permiso; disparo del aviso tras guardar.
- `app/src/main/java/com/duoc/plomoredsocial/notifications/ActivityCreatedNotifier.kt`:
  canal y notificación local.
- `app/src/main/res/drawable/ic_activity_notification.xml`: ícono monocromo.
- `app/src/test/java/com/duoc/plomoredsocial/DeviceLocationProviderTest.kt`:
  elección de proveedor según permisos y disponibilidad.
- `app/src/androidTest/java/com/duoc/plomoredsocial/ActivityCreationTest.kt` y
  `ActivityManagementUiTest.kt`: permiso de prueba y comprobación del aviso.
- `README.md` y este documento: instrucciones y evidencia.

## Verificación observada

Con JBR de Android Studio y `GRADLE_USER_HOME=C:\Users\Juseppe\.gradle`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --rerun-tasks --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.duoc.plomoredsocial.ActivityCreationTest,com.duoc.plomoredsocial.ActivityManagementUiTest,com.duoc.plomoredsocial.HomeRoomIntegrationTest' --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

La primera ejecución terminó `BUILD SUCCESSFUL in 1m 6s`; los XML indican
**29 pruebas unitarias, 0 fallos** (26 anteriores y 3 de selección de
proveedor). La ejecución final terminó `BUILD SUCCESSFUL in 2m 6s`: **13 pruebas
instrumentadas, 0 fallos** en Pixel_7. `ActivityManagementUiTest` comprueba
una notificación activa para el ID creado tras guardar en Room y que dos
intentos inmediatos de guardado producen un solo callback y aviso. La creación
manual sin coordenadas y la regresión Room siguen aprobadas.
La suite instrumentada **completa** terminó `BUILD SUCCESSFUL in 5m 55s`:
**67/67 pruebas, 0 fallos y 0 omitidas** en Pixel_7.

La app debug se instaló y abrió en Pixel_7. En el mapa de creación se probó
**No permitir**: apareció el aviso de permiso denegado y el mapa permaneció
utilizable. Tras conceder ubicación precisa, el mapa se centró en la ubicación
ficticia fijada con `adb emu geo fix -70.6693 -33.4489`, apareció un marcador
y el botón para confirmar el punto quedó habilitado. Se confirmó el punto y
el formulario mostró sus coordenadas. Se abrieron Inicio, Explorar y el
formulario; el mapa de selección cargó.

## Pendientes y límites de evidencia

- El aviso se verificó en prueba instrumentada con permiso concedido; la
  bandeja de notificaciones y el rechazo de `POST_NOTIFICATIONS` no se
  comprobaron manualmente en esta pasada.
- Un intento de revocar `POST_NOTIFICATIONS` desde el mismo proceso de prueba
  hizo que Android cerrara ese proceso. Ese caso se retiró y la suite final
  pasó; la denegación sigue pendiente de prueba manual desde Ajustes.
- Ubicación desactivada, ausencia de posición y timeout tienen manejo en
  código, pero no se provocaron manualmente en Pixel_7.
- No se hizo una revisión **manual** completa de Registro, Perfil ni ADMIN en
  MAIN-010. Sus pruebas instrumentadas sí pasaron dentro de la suite completa.
- MAIN-009 mantiene cinco endpoints Xano pendientes y escrituras remotas
  deshabilitadas. GPS y notificaciones son locales.

## Texto para Trello

**MAIN-010 — Implementar recursos nativos GPS y notificaciones**

- GPS actual del dispositivo mediante LocationManagerCompat, con permisos
  solicitados al tocar «Usar mi ubicación» y marcador centrado en el mapa.
- Permiso denegado probado visualmente; mapa y dirección manual conservados.
- Notificación local con canal Android tras persistencia exitosa en Room;
  permiso POST_NOTIFICATIONS en Android 13+ sin bloquear el guardado.
- 29 pruebas unitarias y 67/67 instrumentadas de la suite completa aprobadas
  en Pixel_7; las de creación verifican una notificación por actividad.
- Build debug y ejecución de Inicio, Explorar y selector de mapa comprobados.
- Pendiente verificar manualmente ubicación desactivada y rechazo de
  notificaciones; no cerrar la tarjeta hasta registrar esas evidencias.
