# ConexionTesterBD — Conexión y gestión de una base de datos (Swing + JDBC)

Aplicación de escritorio en **Java (Swing)**, sin frameworks, con dos ventanas:

1. **Probador de conexión**: se escribe la URL, el usuario y la contraseña de una base de
   datos y se valida la conexión con JDBC. El resultado aparece en **verde** (conexión
   exitosa) o en **rojo** (fallo de conexión) con el mensaje técnico del error.
2. **Gestión de inventario (CRUD)**: se abre cuando la conexión es exitosa y permite
   **crear, consultar, buscar, actualizar y eliminar** productos de la base de datos.

Actividad EA3 — Incorporando UI y BD. Programación Orientada a Objetos II — Grupo 9.

## Integrantes

| Integrante | Usuario de GitHub |
|---|---|
| Carlos Andrés Castrillón Ceballos | carloscastrillon62-coder |
| Ximena Murillo Granada | ximenamurillo-coder |
| José Ángel Higuita Puerta | josehiguitap-hub |

## Requisitos

- JDK 17 o superior.
- Driver de SQLite incluido en `lib/sqlite-jdbc-3.42.0.1.jar` (no es un framework: es el
  driver JDBC que permite a Java comunicarse con SQLite).

## Estructura

```
ConexionTesterBD/
├── src/conexion/
│   ├── ConexionTester.java    Ventana de conexión (JFrame) y lógica JDBC
│   ├── GestionProductos.java  Ventana del inventario: JTable + formulario CRUD
│   ├── ProductoDAO.java       Acceso a datos: todo el SQL (PreparedStatement)
│   ├── Producto.java          Record con los datos de un producto
│   ├── Categoria.java         Record con los datos de una categoría
│   ├── PruebasConexion.java   6 pruebas de consola de la conexión
│   └── PruebasCRUD.java       8 pruebas de consola del CRUD
├── sql/
│   ├── 01_crear_tablas.sql    Script de creación de la base de datos
│   └── 02_poblar_datos.sql    Script de poblado (5 categorías y 15 productos)
├── lib/sqlite-jdbc-3.42.0.1.jar
├── compilar.bat  ejecutar.bat  pruebas.bat
└── README.md
```

## Compilar y ejecutar (Windows)

Doble clic en `compilar.bat` y luego en `ejecutar.bat`, o desde la terminal, en la carpeta
del proyecto:

```powershell
javac -encoding UTF-8 -d out src\conexion\*.java
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" conexion.ConexionTester
```

Pruebas (doble clic en `pruebas.bat`):

```powershell
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" conexion.PruebasConexion
java -cp "out;lib\sqlite-jdbc-3.42.0.1.jar" conexion.PruebasCRUD
```

En Linux o macOS cambie `;` por `:` en el classpath y `\` por `/` en las rutas.

## Crear la base de datos

La base de datos es el archivo `inventario.db` (SQLite). No se sube al repositorio: cada
persona la crea con los scripts de la carpeta `sql`, de una de estas dos formas:

- **Desde la aplicación:** probar la conexión con `jdbc:sqlite:inventario.db`, presionar
  **Abrir inventario (CRUD)** y luego **Crear BD con scripts**.
- **Desde DBeaver:** crear una conexión SQLite al archivo `inventario.db` de la carpeta del
  proyecto y ejecutar `01_crear_tablas.sql` y después `02_poblar_datos.sql`.

## Casos para probar en la ventana de conexión

| URL | Usuario | Resultado esperado |
|---|---|---|
| `jdbc:sqlite:inventario.db` | (vacío) | Verde: conexión exitosa |
| `jdbc:mysql://localhost:3306/inventario` | root | Rojo: driver no encontrado (`ClassNotFoundException`) |
| `jdbc:sqlite:carpeta_inexistente/inventario.db` | (vacío) | Rojo: fallo de conexión (`SQLException`) |
| `jdbc:oracle:thin:@localhost:1521:xe` | admin | Rojo: "No suitable driver" (`SQLException`) |

## Ramas del repositorio

| Rama | Contenido |
|---|---|
| `main` | Versión estable del proyecto |
| `feature/scripts-sql` | Scripts de creación y poblado de la base de datos |
| `feature/crud-productos` | Ventana del inventario, DAO, records y pruebas del CRUD |
| `docs/manual-usuario` | README y manual de usuario en PDF |
