# Integración de Inicio con Actividades

Inicio y Actividades leen las mismas tablas mediante ActivityRepository y RoomActivityRepository.
Inicio usa ActivityEntity directamente y cuenta las filas de ActivityMember igual que Actividades.
No hay una segunda base, copias de actividades ni ejemplos agregados automáticamente.
Si la base está vacía, Inicio lo informa. Los ejemplos de las pruebas solo existen durante esas pruebas.

Se conserva el listado vertical y el acceso: sin sesión, Ver actividad ofrece Login, Registro o Cancelar;
con sesión, abre una consulta del nombre, comuna, fecha/hora, referencia e inscritos reales.
Registro sigue siendo la pantalla pendiente existente. Inicio no realiza inscripciones.

## Cambios pendientes que requieren autorización

- Estados: ActivityEntity no guarda si una actividad está abierta, en progreso o completa.
  Para mostrarlos se necesitaría acordar su significado, agregar un campo, migrar Room de versión 2
  a la siguiente y adaptar el formulario y las operaciones del módulo Actividades. No se infieren del texto de fecha.
- Cupos: ActivityEntity no guarda capacidad. Para mostrar x/capacidad se necesitaría agregarla,
  migrar Room y adaptar el formulario. Para imponer un máximo habría que autorizar también una operación
  de inscripción que compruebe el límite de forma segura en ActivityDao y su flujo de guardado.
  Esta integración solo muestra inscritos reales, sin inventar un denominador ni imponer límites.
- Categorías e intereses: el modelo persistido no los guarda. Se conserva InterestCategory para el trabajo futuro,
  sin asignar categorías ficticias a las actividades reales.
- Acceso directo al mapa de una actividad desde Inicio: la ruta actual actividades no recibe un identificador.
  Se necesitaría ampliar esa ruta y ActivitiesScreen para recibir la selección y centrar el mapa,
  respetando el filtro por comuna. Esto tocaría la implementación de Google Maps, por lo que no se realizó.
  El diálogo de consulta existente y la navegación habitual a Actividades siguen disponibles.

No se modificaron ActivityEntity, ActivityDao, el esquema de Room, las operaciones de inscripción
ni ActivitiesScreen y su implementación de Google Maps.
ActivitiesViewModel solo cambia sus lecturas al repositorio compartido; conserva las mismas escrituras.

## Verificación

Las pruebas cubren base vacía, aparición de actividades guardadas, actualización del número de inscritos,
consulta con y sin sesión, navegación entre secciones y conservación de la posición del listado.
Las pruebas existentes de persistencia, migración, Contacto y sesión se mantienen.
