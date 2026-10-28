# MAIN-007 — Explorar y creación de actividades

## Navegación y catálogo
Inicio es informativo; Explorar (ruta buscar) contiene un solo buscador, filtro por comuna
y catálogo LazyColumn conectado a Room. Perfil conserva cuenta, sesión y contacto.
Crear actividad abre el formulario compartido; sin sesión ofrece Login y Registro.
Ver mapa abre un panel sin duplicar el listado. La ruta actividades se conserva como
compatibilidad de mapa, sin buscador ni catálogo adicionales.
El detalle conserva inscripción, retiro, datos e indicaciones.
HomeViewModel / RoomActivityRepository alimentan el catálogo y contadores;
ExploreViewModel conserva filtros en SavedStateHandle. No se duplicaron repositorios.

## Formulario y validación
Fecha y hora se introducen separadamente: DD-MM-AAAA y HH:mm de 24 horas.
Solo se aceptan dígitos en la entrada manual; se insertan guiones y dos puntos.
Los iconos abren DatePickerDialog y TimePickerDialog nativos.
ActivityFormValidator valida calendario estricto, años bisiestos, rangos horarios,
fechas anteriores a hoy y momentos anteriores o iguales al instante de guardar.
Se usa la zona horaria del dispositivo. La comprobación de calendario no depende de
que exista la medianoche durante cambios de horario de verano.
ActivitiesViewModel vuelve a validar al guardar.
El horario continúa en schedule como texto: DD-MM-AAAA, HH:mm. Los horarios anteriores
no se transforman ni se rechazan al leerlos.
La descripción es opcional, multilínea, admite hasta 2000 caracteres y muestra contador.
Los campos y la apertura de selectores recuerdan su estado durante la recreación.
El formulario enumera los campos pendientes y el motivo temporal que impide guardar.

## Migración autorizada Room 2 → 3
El usuario autorizó expresamente la migración antes de implementarla.
ActivityEntity agrega description TEXT NOT NULL DEFAULT '' y hace anulables latitude
y longitude. Se reconstruye únicamente activities, copiando registros en orden rowid,
conservando IDs, horarios y coordenadas existentes, y descripción vacía para datos previos.
activity_members y contacts permanecen intactas. Se conserva la migración 1 → 2 y se
registra la cadena 1 → 2 → 3. El esquema exportado es app/schemas/.../3.json.
No se usa migración destructiva.
ActivityFormValidator y el guardado exigen coordenadas ambas presentes o ambas ausentes,
y verifican rangos si están presentes. No se generan coordenadas para direcciones textuales.

## Ubicación manual y Maps
Comuna y dirección o referencia son obligatorias. El punto geográfico es opcional,
incluido cuando Maps está configurado.
MAPS_CONFIGURED=false deshabilita únicamente el selector y el mapa integrado; ahora
permite guardar con ubicación textual. Se explica este comportamiento en el formulario.
Las actividades sin coordenadas aparecen en el catálogo, detalle e inscripciones.
No crean marcadores y sus botones de mapa/indicaciones por coordenadas están deshabilitados.
Las actividades con coordenadas conservan marcadores, cámara, controles e Intent de indicaciones.
La descripción persistida se muestra en el detalle.
No se ha validado Maps con clave/API real ni la navegación efectiva en una aplicación externa.

## Intereses pendientes
Las categorías de interés todavía no se almacenan. No se infieren de títulos.
Su filtro requiere definir y persistir la relación con actividades, formulario y una
migración posterior con pruebas. La versión 3 solo incorpora descripción y coordenadas opcionales.

## Pruebas
Línea base revisada antes de la autorización: 6 unitarias y 24 instrumentadas aprobadas.
Se agregaron pruebas unitarias de formatos, límites, calendario, hora y coordenadas.
Se agregaron pruebas instrumentadas de migración 2 → 3 conservando contactos e inscritos,
persistencia tras reapertura de descripción sin coordenadas, selectores nativos,
límite de descripción y creación completa con Maps no configurado.
Se mantiene la cobertura de migración 1 → 3, catálogo reactivo, búsqueda, filtros,
scroll/recreación, inscripción/retiro, Login/Registro, sesión, Perfil y Contáctanos.

Resultado: compilación correcta; 11 pruebas unitarias aprobadas y suite completa de
27 instrumentadas aprobada, sin fallos ni errores en Pixel 7 / API 37.
Tras ajustar la comprobación de medianoche se repitieron las unitarias y las 3 pruebas
instrumentadas de creación/selectores/migración: BUILD SUCCESSFUL.
MAPS_CONFIGURED=false fue probado realmente; no se validó una clave real de Maps.

## Corrección del cursor de fecha y hora
Los campos usaban String y agregaban separadores en onValueChange sin conservar selección.
Ahora guardan dígitos y selección en TextFieldValue con su Saver. DateTimeInput.kt filtra
pegados, limita 8/4 dígitos y remapea selección/composición contando dígitos. Los separadores
se dibujan mediante VisualTransformation con OffsetMapping bidireccional; no son caracteres
editables, por lo que borrar a ambos lados elimina el dígito correspondiente sin atascarse.
Los selectores nativos completan los dígitos y sitúan el cursor al final.
El texto formateado sigue llegando al validador y al guardado exactamente como antes.
Los errores específicos se muestran al abandonar/finalizar el campo o intentar guardar;
durante la escritura incompleta solo se indica que hay que completar fecha y hora.
Esta corrección no cambia Room, entidades, migraciones ni validadores de negocio.
Verificación del cursor: compilación correcta, 16 unitarias y 6 instrumentadas relevantes
aprobadas en Pixel 7 / API 37, sin fallos. Incluyen selección tras cada dígito y recreación,
edición intermedia, borrado antes/después de separadores, pegado formateado, validación
al abandonar el campo, DatePicker/TimePicker y guardado real del horario en Room.
