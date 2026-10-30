# Tipografía principal — Plus Jakarta Sans

Continuación de DIS-004 en `feature/DIS-004-rediseno-visual`. Se conservan todos
los cambios pendientes del rediseño y la paleta azul/lavanda aprobada.

## Recursos y procedencia

Se utilizan los TTF estáticos originales de la distribución
[2.7.1 del repositorio oficial de Tokotype](https://github.com/tokotype/PlusJakartaSans/tree/2.7.1/fonts/ttf),
sin modificar los archivos de fuente ni añadir un proveedor de fuentes descargables.
La licencia SIL Open Font License 1.1 se incluye en el APK como recurso raw.

| Archivo en `app/src/main/res/font` | Peso real |
| --- | --- |
| `plus_jakarta_sans_regular.ttf` | Regular, 400 |
| `plus_jakarta_sans_medium.ttf` | Medium, 500 |
| `plus_jakarta_sans_semibold.ttf` | SemiBold, 600 |
| `plus_jakarta_sans_bold.ttf` | Bold, 700 |

Los cuatro archivos suman 528 880 bytes antes de compresión. Se verificaron sus
tablas TrueType, pesos y cobertura de vocales acentuadas, ñ/Ñ, ü/Ü y ¿/¡.
Se comprobó también que los cuatro TTF y la licencia están empaquetados en el APK.

## Aplicación del tema

`ui/theme/Type.kt` declara una única familia `PlusJakartaSans` y los quince roles
de Material 3. No queda ningún rol con la familia predeterminada del sistema.

| Uso | Tamaño / interlineado | Peso |
| --- | --- | --- |
| `headlineLarge` | 30 / 38 sp | Bold |
| `headlineMedium` | 24 / 32 sp | Bold |
| `headlineSmall` | 22 / 30 sp | Bold |
| Tarjetas: `titleLarge`, `titleMedium` | 22 / 28, 18 / 26 sp | SemiBold |
| Encabezado pequeño: `titleSmall` | 14 / 20 sp | SemiBold |
| Cuerpo grande, medio y pequeño | 16 / 24, 14 / 21, 12 / 18 sp | Regular |
| Botones y tabs: `labelLarge` | 14 / 20 sp | SemiBold |
| Ayudas y navegación: `labelMedium`, `labelSmall` | 12 / 16, 11 / 16 sp | Medium |
| Roles display | 44 / 52, 36 / 44, 32 / 40 sp | Bold |

Se conservan los tamaños e interlineados que ya utilizaban las pantallas; se
unifican los pesos y se completan los estilos que antes heredaban valores de
Material. `headlineSmall` pasa a Bold. El espaciado de letras es uniforme y se
mantiene la escala de texto del dispositivo, sin alturas de texto fijas.

Inicio, Explorar, actividades, Perfil, Contáctanos, Login, Registro,
Administración, formularios y diálogos Compose reciben la tipografía por el tema
compartido. La revisión del código no encontró estilos locales de fuente que
requirieran duplicar o modificar cada pantalla.

`plus_jakarta_sans.xml` ofrece la misma familia a los controles nativos desde
Android 8 (API 26). Los temas v26 y v31 la referencian, manteniendo los atributos
de splash y barras. En API 24/25, Compose utiliza los TTF locales, mientras que
los controles nativos del sistema conservan su fuente compatible. Los textos
dibujados por terceros, como las etiquetas dentro del mapa de Google, siguen
siendo responsabilidad de ese SDK.

## Archivos de esta continuación

- `app/src/main/java/com/duoc/plomoredsocial/ui/theme/Type.kt`.
- Los cuatro TTF y `plus_jakarta_sans.xml` en `app/src/main/res/font/`.
- `app/src/main/res/raw/plus_jakarta_sans_license.txt`.
- `app/src/main/res/values-v26/themes.xml`.
- `app/src/main/res/values-v31/themes.xml`.
- Este informe y la referencia tipográfica de `docs/DIS-004.md`.

No se modifican pantallas individualmente, colores, dependencias, navegación,
Room, autenticación, repositorios, Maps, sesión ni reglas de negocio.

## Validación

Compilación mediante MCP de Android Studio y Gradle. Se reutilizan las pruebas
existentes de accesibilidad visual, Login, Registro, navegación, Contáctanos,
Administración y creación/inscripción de actividades, sin cambiar aserciones.

La instrumentación se ejecuta directamente con AndroidJUnitRunner tras instalar
los APK con `adb install -r`, conservando los datos existentes y evitando que el
runner de Gradle desinstale la aplicación al terminar. Los registros locales de
esta ejecución quedan en `build/typography-instrumented.log`, excluido de Git.

No se realiza `git add`, commit, push ni Pull Request.
